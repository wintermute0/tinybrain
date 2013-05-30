package yatan.deeplearning.softmax.contract.parameter;

import java.io.File;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.FileWriterWithEncoding;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.dispatch.Future;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnGradient;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnConfiguration;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.softmax.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class WordEmbeddingAnnParameterActorContractImpl extends BaseActorContract implements ParameterActorContract {
    private static final Timeout SLICE_UPDATE_TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    private static final double ADA_DELTA_RHO = 0.95;
    private static final double ADA_DELTA_EPSILON = 0.000001;

    private static final double STATE_SAVING_INTERVAL_MINUTES = 10;

    private static final String MODEL_FOLDER = "test_files/results/";

    private static Date lastSaveTime = new Date();

    private static final String MODEL_FILE_PREFIX = "softmax_model_";

    private final List<ActorRef> sliceUpdateActors = Lists.newArrayList();

    private final Dictionary dictionary;
    private final AnnConfiguration annConfiguration;
    private final int wordVectorSize;

    @Inject(optional = false)
    private TrainerConfiguration trainerConfiguration;

    @Inject
    @Named("parameter_actor_update_slice")
    private int updateSlice;

    private WordEmbedding wordEmbedding;
    private DefaultAnnModel annModel;

    private Matrix wordEmbeddingGradientSumSquare;
    private List<Matrix> annDeltaGradientSumSquare;

    private Matrix deltaWordEmbeddingSumSquare;
    private List<Matrix> deltaAnnWeightSumSquare;

    @Inject
    public WordEmbeddingAnnParameterActorContractImpl(Dictionary dictionary,
            @Named("ann_configuration") AnnConfiguration annConfiguration, @Named("word_vector_size") int wordVectorSize) {
        Preconditions.checkArgument(dictionary != null, "The parameter 'dictionary' cannot be null.");
        Preconditions.checkArgument(annConfiguration != null);

        this.dictionary = dictionary;
        this.annConfiguration = annConfiguration;
        this.wordVectorSize = wordVectorSize;
    }

    @Override
    public void preStart() {
        for (int i = 0; i < updateSlice; i++) {
            this.sliceUpdateActors.add(getActor().context().actorOf(
                    new Props(new SliceUpdateActorFactory(i, updateSlice)), "slice_updator_" + i));
        }
    }

    private class SliceUpdateActorFactory implements UntypedActorFactory {
        private static final long serialVersionUID = 5348017784800584586L;
        private final int sliceId;
        private final int totalSlice;

        public SliceUpdateActorFactory(int sliceId, int totalSlice) {
            Preconditions.checkArgument(sliceId < totalSlice && sliceId >= 0);

            this.sliceId = sliceId;
            this.totalSlice = totalSlice;
        }

        @Override
        public Actor create() throws Exception {
            return new SliceUpdateActor(sliceId, totalSlice);
        }
    }

    private class SliceUpdateActor extends UntypedActor {
        private final int sliceId;
        private final int totalSlice;

        public SliceUpdateActor(int sliceId, int totalSlice) {
            Preconditions.checkArgument(sliceId < totalSlice && sliceId >= 0);

            this.sliceId = sliceId;
            this.totalSlice = totalSlice;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            Parameter gradient = (Parameter) message;

            Serializable[] inputData = (Serializable[]) gradient.getSerializable();
            AnnGradient annGradient = (AnnGradient) inputData[0];
            // annModel.update(annGradient, ADA_DELTA_RHO, ADA_DELTA_EPSILON, annDeltaGradientSumSquare,
            // this.deltaAnnWeightSumSquare);
            annModel.update(annGradient, 0.1, annDeltaGradientSumSquare, this.sliceId, this.totalSlice);

            @SuppressWarnings("unchecked")
            Map<Integer, Double[]> wordEmbeddingDelta = (Map<Integer, Double[]>) inputData[1];
            // wordEmbedding.update(wordEmbeddingDelta, ADA_DELTA_RHO, ADA_DELTA_EPSILON,
            // this.wordEmbeddingGradientSumSquare,
            // this.deltaWordEmbeddingSumSquare);
            wordEmbedding
                    .update(wordEmbeddingDelta, 0.1, wordEmbeddingGradientSumSquare, this.sliceId, this.totalSlice);

            getSender().tell("done.");
        }
    }

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        initIfNecessary();

        Parameter parameter = new Parameter();
        parameter.setSerializable(new Serializable[] {wordEmbedding, annModel});

        tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(), parameter));
    }

    @Override
    public void updateGradient(Parameter gradient) {
        Preconditions.checkArgument(gradient != null, "Gradient cannot be null.");

        final List<Future<Object>> futures = Lists.newArrayList();
        for (int i = 0; i < this.sliceUpdateActors.size(); i++) {
            futures.add(Patterns.ask(this.sliceUpdateActors.get(i), gradient, SLICE_UPDATE_TIMEOUT));
        }

        // wait till all update slice is done
        final Future<Iterable<Object>> aggregatedSliceUpdateFutures =
                Futures.sequence(futures, getActor().getContext().dispatcher());
        aggregatedSliceUpdateFutures.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable arg0, Iterable<Object> arg1) throws Throwable {
                synchronized (aggregatedSliceUpdateFutures) {
                    aggregatedSliceUpdateFutures.notify();
                }
            }
        });

        try {
            while (!aggregatedSliceUpdateFutures.isCompleted()) {
                synchronized (aggregatedSliceUpdateFutures) {
                    aggregatedSliceUpdateFutures.wait();
                }
            }
        } catch (InterruptedException e) {
            getLogger().error("Interrupted while waiting for the slice update actors.", e);
        }

        // save state if necessary
        if (new Date().getTime() - lastSaveTime.getTime() > STATE_SAVING_INTERVAL_MINUTES * 60 * 1000) {
            lastSaveTime = new Date();
            saveState();
        }
    }

    private void initIfNecessary() {
        if (wordEmbedding == null) {
            getLogger().info("Initializing parameter actor...");

            // load persisted states
            PersistableState persistableState = loadState();

            if (wordEmbedding == null) {
                getLogger().info(
                        "Create new random word embedding with dictionary size = " + this.dictionary.words()
                                + ", word vector size = " + wordVectorSize);
                wordEmbedding = new WordEmbedding(this.dictionary.words(), wordVectorSize);
            }

            if (annModel == null) {
                getLogger().info("Create new ANN model with configuration " + this.annConfiguration);
                // Configuration configuration =
                // new Configuration(wordEmbedding.getWordVectorSize() * TrainingInstanceProducer.WINDOWS_SIZE);
                // configuration.addLayer(300, ActivationFunction.TANH);
                // configuration.addLayer(DataCenter.tags().size(), ActivationFunction.SOFTMAX, false);
                annModel = new DefaultAnnModel(this.annConfiguration);

                // reuse the same lower layer o the persistable state
                if (persistableState != null && persistableState.annModel != null) {
                    getLogger().info("Reuse the lower layer of the persisted ann model...");
                    this.annModel.reuseLowerLayer(persistableState.annModel, 0);
                    getLogger().info("Ann model statistics after reuseing:");
                    LogUtility.logAnnModel(getLogger(), annModel);
                }

                if (this.trainerConfiguration.dropout) {
                    getLogger().info("Scale ANN for dropout learning.");
                    scaleANN(annModel, 2);
                }
            }

            if (annDeltaGradientSumSquare == null || wordEmbeddingGradientSumSquare == null
                    || deltaAnnWeightSumSquare == null || deltaWordEmbeddingSumSquare == null) {
                getLogger().info("Create new ANN gradient/delta sum squre.");
                this.annDeltaGradientSumSquare = Lists.newArrayList();
                this.deltaAnnWeightSumSquare = Lists.newArrayList();
                for (int i = 0; i < annModel.getLayerCount(); i++) {
                    Matrix layer = annModel.getLayer(i);
                    this.annDeltaGradientSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
                    this.deltaAnnWeightSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
                }

                getLogger().info("Create new word embedding gradient/delta sum squre.");
                this.wordEmbeddingGradientSumSquare =
                        new Matrix(wordEmbedding.getMatrix().rowSize(), wordEmbedding.getMatrix().columnSize());
                this.deltaWordEmbeddingSumSquare =
                        new Matrix(wordEmbedding.getMatrix().rowSize(), wordEmbedding.getMatrix().columnSize());
            }
        }
    }

    private void saveState() {
        File stateFile = new File(MODEL_FOLDER + MODEL_FILE_PREFIX + (new Date().getTime()) + ".json");
        getLogger().info("Saving parameter server state to " + stateFile + "...");
        FileWriterWithEncoding writer = null;
        try {
            writer = new FileWriterWithEncoding(stateFile, Charsets.UTF_8);
            String json =
                    new Gson().toJson(new PersistableState(wordEmbedding, annModel, wordEmbeddingGradientSumSquare,
                            annDeltaGradientSumSquare, this.deltaWordEmbeddingSumSquare, this.deltaAnnWeightSumSquare));
            writer.write(json);
        } catch (IOException e) {
            getLogger().error("Error occurred while trying to save parameter server state: " + e.getMessage(), e);
        } finally {
            close(writer);
        }
    }

    private PersistableState loadState() {
        getLogger().info("Trying to find persisted parameter server state...");
        File stateFile = null;
        File modelFolderFile = new File(MODEL_FOLDER);
        if (modelFolderFile.isDirectory()) {
            for (File file : modelFolderFile.listFiles()) {
                if (file.isFile() && Files.getFileExtension(file.getName()).equals("json")
                        && (stateFile == null || stateFile.getName().compareTo(file.getName()) < 0)) {
                    stateFile = file;
                }
            }
        }

        if (stateFile != null) {
            getLogger().info("Loading parameter server state from " + stateFile + "...");
            FileInputStream is = null;
            InputStreamReader reader = null;
            try {
                is = new FileInputStream(stateFile);
                reader = new InputStreamReader(is, Charsets.UTF_8);
                PersistableState state = new Gson().fromJson(reader, PersistableState.class);

                if (this.wordVectorSize == state.wordEmbedding.getWordVectorSize()
                        && this.dictionary.words().equals(state.wordEmbedding.getDictionary())) {
                    wordEmbedding = state.wordEmbedding;
                    getLogger().info("Word embedding " + wordEmbedding + " has been loaded.");

                    // scale word embedding
                    // getLogger().info("Scale word embedding to [-1, 1]");
                    // scaleWordEmbedding(wordEmbedding);
                    // LogUtility.logWordEmbedding(getLogger(), wordEmbedding);

                    // only reuse other saved states if the ANN model configuration is identical
                    if (state.annModel != null && this.annConfiguration.equals(state.annModel.getConfiguration())) {
                        getLogger()
                                .info("Loading ANN and delta sum squre data because the ANN model configuration is identical.");
                        this.wordEmbeddingGradientSumSquare = state.wordEmbeddingWeightSumSquare;

                        this.annModel = state.annModel;
                        this.annDeltaGradientSumSquare = state.annDeltaWeightSumSquare;

                        this.deltaWordEmbeddingSumSquare = state.deltaWordEmbeddingSumSquare;
                        this.deltaAnnWeightSumSquare = state.deltaAnnSumSquare;
                    } else {
                        getLogger()
                                .info("Ignore ANN and delta sum squre data because the ANN model configuration is differenct.");

                        // otherwise, we only reuse word embedding, to we need to scale it first
                        // double sigma = 1;
                        // getLogger().info("Scale word embedding with sigma " + sigma);
                        // scaleWordEmbedding(wordEmbedding, sigma);
                    }
                } else {
                    getLogger().info("Word embedding configuration does not match. Ingore the state file.");
                }

                return state;
            } catch (IOException e) {
                getLogger().error("Error occurred while trying to load parameter server state: " + e.getMessage(), e);
                return null;
            } finally {
                close(reader, is);
            }
        } else {
            getLogger().info("Can't find any persisted parameter sever state. Let's start from strach.");
            return null;
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // private static void scaleWordEmbedding(WordEmbedding wordEmbedding) {
    // double max = Double.MIN_VALUE;
    // double min = Double.MAX_VALUE;
    // double[][] data = wordEmbedding.getMatrix().getData();
    // for (int i = 0; i < data.length; i++) {
    // for (int j = 0; j < data[0].length; j++) {
    // double value = data[i][j];
    // if (value > max) {
    // max = value;
    // }
    // if (value < min) {
    // min = value;
    // }
    // }
    // }
    //
    // for (int i = 0; i < data.length; i++) {
    // for (int j = 0; j < data[0].length; j++) {
    // data[i][j] = (data[i][j] - (max + min) / 2) / (max - min) * 2;
    // }
    // }
    // }

    private static void scaleANN(DefaultAnnModel annModel, double factor) {
        for (int i = 0; i < annModel.getLayerCount(); i++) {
            Matrix matrix = annModel.getLayer(i);
            for (int row = 0; row < matrix.rowSize() - 1; row++) {
                for (int column = 0; column < matrix.columnSize(); column++) {
                    matrix.getData()[row][column] *= factor;
                }
            }
        }
    }

    public static class PersistableState {
        public WordEmbedding wordEmbedding;
        public DefaultAnnModel annModel;

        public Matrix wordEmbeddingWeightSumSquare;
        public List<Matrix> annDeltaWeightSumSquare;

        public Matrix deltaWordEmbeddingSumSquare;
        public List<Matrix> deltaAnnSumSquare;

        public PersistableState(WordEmbedding wordEmbedding, DefaultAnnModel annModel,
                Matrix wordEmbeddingDeltaSumSquare, List<Matrix> annDeltaSumSquare, Matrix deltaWordEmbeddingSumSquare,
                List<Matrix> deltaAnnSumSquare) {
            this.wordEmbedding = wordEmbedding;
            this.annModel = annModel;
            this.wordEmbeddingWeightSumSquare = wordEmbeddingDeltaSumSquare;
            this.annDeltaWeightSumSquare = annDeltaSumSquare;
            this.deltaWordEmbeddingSumSquare = deltaWordEmbeddingSumSquare;
            this.deltaAnnSumSquare = deltaAnnSumSquare;
        }
    }
}
