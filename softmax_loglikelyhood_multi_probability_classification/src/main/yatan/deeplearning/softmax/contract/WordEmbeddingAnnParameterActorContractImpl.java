package yatan.deeplearning.softmax.contract;

import java.io.File;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.FileWriterWithEncoding;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnModel.Configuration;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class WordEmbeddingAnnParameterActorContractImpl extends BaseActorContract implements ParameterActorContract {
    public static final double ADAGRAD_LEARNING_RATE_LAMPDA = 0.01;

    private static final double STATE_SAVING_INTERVAL_MINUTES = 10;

    private static final String MODEL_FOLDER = "test_files/results/";

    private static Date lastSaveTime = new Date();

    private static final String MODEL_FILE_PREFIX = "softmax_model_";

    private final Dictionary dictionary;
    private final Configuration annConfiguration;
    private final int wordVectorSize;

    private WordEmbedding wordEmbedding;
    private AnnModel annModel;

    private Matrix wordEmbeddingDeltaSumSquare;
    private List<Matrix> annDeltaSumSquare;

    @Inject
    public WordEmbeddingAnnParameterActorContractImpl(Dictionary dictionary,
            @Named("ann_configuration") Configuration annConfiguration, @Named("word_vector_size") int wordVectorSize) {
        Preconditions.checkArgument(dictionary != null, "The parameter 'dictionary' cannot be null.");
        Preconditions.checkArgument(annConfiguration != null);

        this.dictionary = dictionary;
        this.annConfiguration = annConfiguration;
        this.wordVectorSize = wordVectorSize;
    }

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        initIfNecessary();

        Parameter parameter = new Parameter();
        parameter.setSerializable(new Serializable[] {wordEmbedding, annModel});

        tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(), parameter));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updateGradient(Parameter gradient) {
        Preconditions.checkArgument(gradient != null, "Gradient cannot be null.");

        Serializable[] inputData = (Serializable[]) gradient.getSerializable();
        AnnGradient annGradient = (AnnGradient) inputData[0];
        annModel.update(annGradient, ADAGRAD_LEARNING_RATE_LAMPDA, annDeltaSumSquare);

        Map<Integer, Double[]> wordEmbeddingDelta = (Map<Integer, Double[]>) inputData[1];
        wordEmbedding.update(wordEmbeddingDelta, ADAGRAD_LEARNING_RATE_LAMPDA, wordEmbeddingDeltaSumSquare);

        // save state if necessary
        if (new Date().getTime() - lastSaveTime.getTime() > STATE_SAVING_INTERVAL_MINUTES * 60 * 1000) {
            lastSaveTime = new Date();
            saveState();
        }
    }

    private void initIfNecessary() {
        if (wordEmbedding == null) {
            getLogger().info("Initializing parameter actor...");
            // File modelFolder = new File(MODEL_FOLDER);
            // if (modelFolder.isDirectory()) {
            // List<File> modelFiles = ImmutableList.copyOf(modelFolder.listFiles());
            // modelFiles = Lists.newArrayList(Collections2.filter(modelFiles, new Predicate<File>() {
            // public boolean apply(File file) {
            // return file.isFile() && Files.getFileExtension(file.getName()).equals("json");
            // }
            // }));
            //
            // if (!modelFiles.isEmpty()) {
            // Collections.sort(modelFiles, new Comparator<File>() {
            // @Override
            // public int compare(File arg0, File arg1) {
            // return -arg0.getName().compareTo(arg1.getName());
            // }
            // });
            // File file = modelFiles.get(0);
            // getLogger().info("Load word embedding from file " + file);
            // try {
            // String text = Files.toString(file, Charsets.UTF_8);
            // JsonElement jsonElement = new JsonParser().parse(text);
            // wordEmbedding =
            // new Gson().fromJson(jsonElement.getAsJsonObject().get("wordEmbedding"),
            // WordEmbedding.class);
            //
            // double sigma = 0.1;
            // getLogger().info("Scale word embedding with sigma " + sigma);
            // scaleWordEmbedding(wordEmbedding, sigma);
            // } catch (IOException e) {
            // getLogger().error("Error occurred while loading word embedding from file.", e);
            // }
            // }
            // }

            // load persisted states
            loadState();

            if (wordEmbedding == null) {
                getLogger().info(
                        "Create new random word embedding with dictionary size = " + this.dictionary.words()
                                + ", word vector size = " + wordVectorSize);
                wordEmbedding = new WordEmbedding(this.dictionary.words(), wordVectorSize);
            }

            if (annModel == null) {
                getLogger().info("Create new ANN model with configuration " + this.annConfiguration);
                wordEmbeddingDeltaSumSquare =
                        new Matrix(wordEmbedding.getMatrix().rowSize(), wordEmbedding.getMatrix().columnSize());
                // Configuration configuration =
                // new Configuration(wordEmbedding.getWordVectorSize() * TrainingInstanceProducer.WINDOWS_SIZE);
                // configuration.addLayer(300, ActivationFunction.TANH);
                // configuration.addLayer(DataCenter.tags().size(), ActivationFunction.SOFTMAX, false);
                annModel = new AnnModel(this.annConfiguration);

                annDeltaSumSquare = new ArrayList<Matrix>();
                for (int i = 0; i < annModel.getLayerCount(); i++) {
                    Matrix layer = annModel.getLayer(i);
                    annDeltaSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
                }
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
                    new Gson().toJson(new PersistableState(wordEmbedding, annModel, wordEmbeddingDeltaSumSquare,
                            annDeltaSumSquare));
            writer.write(json);
        } catch (IOException e) {
            getLogger().error("Error occurred while trying to save parameter server state: " + e.getMessage(), e);
        } finally {
            close(writer);
        }
    }

    private boolean loadState() {
        getLogger().info("Trying to find persisted parameter server state...");
        File stateFile = null;
        for (File file : new File(MODEL_FOLDER).listFiles()) {
            if (file.isFile() && Files.getFileExtension(file.getName()).equals("json")
                    && (stateFile == null || stateFile.getName().compareTo(file.getName()) < 0)) {
                stateFile = file;
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

                    // only reuse other saved states if the ANN model configuration is identical
                    if (this.annConfiguration.equals(state.annModel.getConfiguration())) {
                        getLogger()
                                .info("Loading ANN and delta sum squre data because the ANN model configuration is identical.");
                        wordEmbeddingDeltaSumSquare = state.wordEmbeddingDeltaSumSquare;
                        annModel = state.annModel;
                        annDeltaSumSquare = state.annDeltaSumSquare;
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
            } catch (IOException e) {
                getLogger().error("Error occurred while trying to load parameter server state: " + e.getMessage(), e);
                return false;
            } finally {
                close(reader, is);
            }

            return true;
        } else {
            getLogger().info("Can't find any persisted parameter sever state. Let's start from strach.");
            return false;
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

    // private static void scaleWordEmbedding(WordEmbedding wordEmbedding, double sigma) {
    // double total = 0;
    // Matrix matrix = wordEmbedding.getMatrix();
    // double[][] data = matrix.getData();
    // for (int i = 0; i < matrix.rowSize(); i++) {
    // for (int j = 0; j < matrix.columnSize(); j++) {
    // total += data[i][j];
    // }
    // }
    //
    // double mean = total / (matrix.rowSize() * matrix.columnSize());
    // double dv = 0;
    // for (int i = 0; i < matrix.rowSize(); i++) {
    // for (int j = 0; j < matrix.columnSize(); j++) {
    // dv += Math.pow(data[i][j] - mean, 2);
    // }
    // }
    //
    // double sdv = Math.sqrt(dv);
    // for (int i = 0; i < matrix.rowSize(); i++) {
    // for (int j = 0; j < matrix.columnSize(); j++) {
    // data[i][j] = sigma * data[i][j] / sdv;
    // }
    // }
    // }

    public static class PersistableState {
        public WordEmbedding wordEmbedding;
        public AnnModel annModel;

        public Matrix wordEmbeddingDeltaSumSquare;
        public List<Matrix> annDeltaSumSquare;

        public PersistableState(WordEmbedding wordEmbedding, AnnModel annModel, Matrix wordEmbeddingDeltaSumSquare,
                List<Matrix> annDeltaSumSquare) {
            this.wordEmbedding = wordEmbedding;
            this.annModel = annModel;
            this.wordEmbeddingDeltaSumSquare = wordEmbeddingDeltaSumSquare;
            this.annDeltaSumSquare = annDeltaSumSquare;
        }

        @Override
        public String toString() {
            return "PersistableState [wordEmbedding=" + wordEmbedding + ", annModel=" + annModel
                    + ", wordEmbeddingDeltaSumSquare=" + wordEmbeddingDeltaSumSquare + ", annDeltaSumSquare="
                    + annDeltaSumSquare + "]";
        }
    }
}
