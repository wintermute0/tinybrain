package yatan.deeplearning.sequence.softmax.contract;

import java.io.Closeable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.output.FileWriterWithEncoding;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnGradient;
import yatan.ann.DefaultAnnModel;
import yatan.commons.matrix.Matrix;

import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class CRFANNParameterActorContractImpl extends BaseActorContract implements ParameterActorContract {
    private static final String MODEL_FOLDER = "test_files/results/";
    private static final String MODEL_FILE_PREFIX = "crfann_model_";
    private static final double STATE_SAVING_INTERVAL_MINUTES = 5;

    private static Date lastSaveTime = new Date();

    @Inject
    private Dictionary dictionary;
    @Inject
    @Named("ann_configuration")
    private AnnConfiguration annConfiguration;
    @Inject
    @Named("word_vector_size")
    private int wordVectorSize;
    @Inject
    @Named("tag_count")
    private int tagCount;

    private WordEmbedding wordEmbedding;
    private DefaultAnnModel annModel;
    private double[][] tagTransitionWeights;

    private Matrix wordEmbeddingGradientSumSquare;
    private List<Matrix> annDeltaGradientSumSquare;
    private double[][] tagTransitionWeightsGradientSumSquare;

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        initIfNecessary();

        Parameter parameter = new Parameter();
        parameter.setSerializable(new Serializable[] {wordEmbedding, annModel, this.tagTransitionWeights});

        tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(), parameter));
    }

    @Override
    public void updateGradient(Parameter gradient) {
        Preconditions.checkArgument(gradient != null, "Gradient cannot be null.");

        Serializable[] inputData = (Serializable[]) gradient.getSerializable();
        AnnGradient annGradient = (AnnGradient) inputData[0];

        final double learningRate = 0.001;
        if (annGradient != null) {
            // annModel.update(annGradient, ADA_DELTA_RHO, ADA_DELTA_EPSILON, annDeltaGradientSumSquare,
            // this.deltaAnnWeightSumSquare);
            // annModel.update(annGradient, 0.1, this.annDeltaGradientSumSquare);
            annModel.update(annGradient, learningRate);

            @SuppressWarnings("unchecked")
            Map<Integer, Double[]> wordEmbeddingDelta = (Map<Integer, Double[]>) inputData[1];
            wordEmbedding.update(wordEmbeddingDelta, learningRate);
            // wordEmbedding.update(wordEmbeddingDelta, ADA_DELTA_RHO, ADA_DELTA_EPSILON,
            // this.wordEmbeddingGradientSumSquare,
            // this.deltaWordEmbeddingSumSquare);
            // wordEmbedding.update(wordEmbeddingDelta, 0.1, this.wordEmbeddingGradientSumSquare);

            double[][] transitaionGradient = (double[][]) inputData[2];
            for (int i = 0; i < transitaionGradient.length; i++) {
                for (int j = 0; j < transitaionGradient[i].length; j++) {
//                    this.tagTransitionWeights[i][j] +=
//                            learningRate * (transitaionGradient[i][j] - 0 * this.tagTransitionWeights[i][j]);

                    // this.tagTransitionWeights[i][j] += 0.01 * transitaionGradient[i][j];
                }
            }
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
            }

            if (this.tagTransitionWeights == null) {
                Random random = new Random(new Date().getTime());
                this.tagTransitionWeights = new double[tagCount + 2][tagCount + 2];
                for (int i = 0; i < this.tagTransitionWeights.length; i++) {
                    for (int j = 0; j < this.tagTransitionWeights[i].length; j++) {
                        this.tagTransitionWeights[i][j] = (random.nextDouble() - 0.5) / 50;
                    }
                }
            }

            if (annDeltaGradientSumSquare == null || wordEmbeddingGradientSumSquare == null) {
                getLogger().info("Create new ANN gradient/delta sum squre.");
                this.annDeltaGradientSumSquare = Lists.newArrayList();
                for (int i = 0; i < annModel.getLayerCount(); i++) {
                    Matrix layer = annModel.getLayer(i);
                    this.annDeltaGradientSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
                }

                getLogger().info("Create new word embedding gradient/delta sum squre.");
                this.wordEmbeddingGradientSumSquare =
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
                    new Gson().toJson(new PersistableState(wordEmbedding, annModel, tagTransitionWeights,
                            wordEmbeddingGradientSumSquare, annDeltaGradientSumSquare));
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

                        this.tagTransitionWeights = state.tagTransitionWeights;
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

    public static class PersistableState {
        public WordEmbedding wordEmbedding;
        public DefaultAnnModel annModel;
        public double[][] tagTransitionWeights;

        public Matrix wordEmbeddingWeightSumSquare;
        public List<Matrix> annDeltaWeightSumSquare;

        public PersistableState(WordEmbedding wordEmbedding, DefaultAnnModel annModel, double[][] tagTransitionWeights,
                Matrix wordEmbeddingDeltaSumSquare, List<Matrix> annDeltaSumSquare) {
            this.wordEmbedding = wordEmbedding;
            this.annModel = annModel;
            this.tagTransitionWeights = tagTransitionWeights;
            this.wordEmbeddingWeightSumSquare = wordEmbeddingDeltaSumSquare;
            this.annDeltaWeightSumSquare = annDeltaSumSquare;
        }
    }
}
