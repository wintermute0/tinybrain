package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Closeable;

import java.io.File;
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

import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnModel.Configuration;
import yatan.ann.AnnModel.Configuration.ActivationFunction;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.data.ZhWikiTrainingDataProducer;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class ParameterActorWordEmbeddingImpl extends BaseActorContract implements ParameterActorContract {
    private static final boolean SCALE_AND_RETRAING = false;
    private static final double STATE_SAVING_INTERVAL_MINUTES = 10;

    // THIS IS THE LEARNING RATE FOR ENGLISH WORDS: public static final double ADAGRAD_LEARNING_RATE_LAMPDA = 0.0001;
    // public static final double ADAGRAD_LEARNING_RATE_LAMPDA = 0.0001;
    public static final double ADAGRAD_LEARNING_RATE_LAMPDA = 0.01;

    private static final String STATE_FOLDER = "test_files/results/";

    private static WordEmbedding wordEmbedding;
    private static AnnModel annModel;

    private static Matrix wordEmbeddingDeltaSumSquare;
    private static List<Matrix> annDeltaSumSquare;

    private static Date lastSaveTime = new Date();

    private final Dictionary dictionary;

    @Inject
    public ParameterActorWordEmbeddingImpl(Dictionary dictionary) {
        Preconditions.checkArgument(dictionary != null);

        this.dictionary = dictionary;
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
        Serializable[] inputData = (Serializable[]) gradient.getSerializable();
        AnnGradient annGradient = (AnnGradient) inputData[0];
        annModel.update(annGradient, ADAGRAD_LEARNING_RATE_LAMPDA, annDeltaSumSquare);

        Map<Integer, Double[]> wordEmbeddingDelta = (Map<Integer, Double[]>) inputData[1];
        wordEmbedding.update(wordEmbeddingDelta, ADAGRAD_LEARNING_RATE_LAMPDA, wordEmbeddingDeltaSumSquare);

        if (new Date().getTime() - lastSaveTime.getTime() > STATE_SAVING_INTERVAL_MINUTES * 60 * 1000) {
            lastSaveTime = new Date();
            saveState();
        }
    }

    private void initIfNecessary() {
        if (wordEmbedding != null) {
            // if there is already word embedding, simply return
            return;
        }

        // try to load saved state
        loadState();

        WordEmbedding scaledWordEmbedding = null;
        if (wordEmbedding != null && SCALE_AND_RETRAING) {
            getLogger().info("Scale word embedding and reset all other training parameters...");
            scaledWordEmbedding = scaleWordEmbedding(wordEmbedding, 0.1);
            wordEmbedding = null;
        }

        if (wordEmbedding == null) {
            getLogger().info("Initializing parameter actor...");
            // init stuff
            wordEmbedding = new WordEmbedding(this.dictionary.words(), 100);

            wordEmbeddingDeltaSumSquare =
                    new Matrix(wordEmbedding.getMatrix().rowSize(), wordEmbedding.getMatrix().columnSize());

            Configuration configuration =
                    new Configuration(wordEmbedding.getWordVectorSize() * ZhWikiTrainingDataProducer.WINDOWS_SIZE);
            configuration.addLayer(100, ActivationFunction.TANH);
            // configuration.addLayer(1, ActivationFunction.Y_EQUALS_X, false);
            configuration.addLayer(2, ActivationFunction.SOFTMAX);
            annModel = new AnnModel(configuration);

            annDeltaSumSquare = new ArrayList<Matrix>();
            for (int i = 0; i < annModel.getLayerCount(); i++) {
                Matrix layer = annModel.getLayer(i);
                annDeltaSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
            }
        }

        if (scaledWordEmbedding != null) {
            getLogger().info("Use scaled word embedding.");
            wordEmbedding = scaledWordEmbedding;
        }
    }

    private void saveState() {
        File stateFile = new File(STATE_FOLDER + (new Date().getTime()) + ".json");
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
        for (File file : new File(STATE_FOLDER).listFiles()) {
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
                wordEmbedding = state.wordEmbedding;
                wordEmbeddingDeltaSumSquare = state.wordEmbeddingDeltaSumSquare;
                annModel = state.annModel;
                annDeltaSumSquare = state.annDeltaSumSquare;
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

    private static WordEmbedding scaleWordEmbedding(WordEmbedding wordEmbedding, double sigma) {
        double total = 0;
        Matrix matrix = wordEmbedding.getMatrix();
        double[][] data = matrix.getData();
        for (int i = 0; i < matrix.rowSize(); i++) {
            for (int j = 0; j < matrix.columnSize(); j++) {
                total += data[i][j];
            }
        }

        double mean = total / (matrix.rowSize() * matrix.columnSize());
        double dv = 0;
        for (int i = 0; i < matrix.rowSize(); i++) {
            for (int j = 0; j < matrix.columnSize(); j++) {
                dv += Math.pow(data[i][j] - mean, 2);
            }
        }

        double sdv = Math.sqrt(dv);
        for (int i = 0; i < matrix.rowSize(); i++) {
            for (int j = 0; j < matrix.columnSize(); j++) {
                data[i][j] = sigma * data[i][j] / sdv;
            }
        }

        return wordEmbedding;
    }

    public static class PersistableState {
        private WordEmbedding wordEmbedding;
        private AnnModel annModel;

        private Matrix wordEmbeddingDeltaSumSquare;
        private List<Matrix> annDeltaSumSquare;

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
