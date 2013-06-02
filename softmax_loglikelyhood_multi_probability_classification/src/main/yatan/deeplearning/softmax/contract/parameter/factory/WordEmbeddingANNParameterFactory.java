package yatan.deeplearning.softmax.contract.parameter.factory;

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnConfiguration;
import yatan.ann.DefaultAnnModel;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.softmax.TrainerConfiguration;
import yatan.deeplearning.softmax.contract.parameter.ParameterFactory;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Parameter;

public class WordEmbeddingANNParameterFactory implements ParameterFactory {
    private static final String ANN_MODEL_KEY = "annModel";
    private static final String WORD_EMBEDDING_KEY = "wordEmbedding";

    private static final Logger LOGGER = Logger.getLogger(WordEmbeddingANNParameterFactory.class);

    @Inject
    private Dictionary dictionary;
    @Inject
    private AnnConfiguration annConfiguration;
    @Inject
    @Named("word_vector_size")
    private int wordVectorSize;
    @Inject
    private TrainerConfiguration trainerConfiguration;

    private WordEmbedding wordEmbedding;
    private DefaultAnnModel annModel;

    private DefaultAnnModel persistedAnnModel;

    @Override
    public Parameter initializeParameter() {
        if (wordEmbedding == null) {
            if (wordEmbedding == null) {
                LOGGER.info("Create new random word embedding with dictionary size = " + this.dictionary.words()
                        + ", word vector size = " + wordVectorSize);
                wordEmbedding = new WordEmbedding(this.dictionary.words(), wordVectorSize);
            }
        }

        if (annModel == null) {
            LOGGER.info("Create new ANN model with configuration " + this.annConfiguration);
            annModel = new DefaultAnnModel(this.annConfiguration);

            // reuse the same lower layer o the persistable state
            if (this.persistedAnnModel != null) {
                LOGGER.info("Reuse the lower layer of the persisted ann model...");
                this.annModel.reuseLowerLayer(this.persistedAnnModel, 0);
                LOGGER.info("Ann model statistics after reuseing:");
                LogUtility.logAnnModel(LOGGER, annModel);
            }

            if (this.trainerConfiguration.dropout) {
                LOGGER.info("Scale ANN for dropout learning.");
                scaleANN(annModel, 2);
            }
        }

        Parameter parameter = new Parameter();
        parameter.setSerializable(new Serializable[] {this.wordEmbedding, this.annModel});

        return parameter;
    }

    public Parameter cloneParameter() {
        Parameter parameter = new Parameter();
        parameter.setSerializable(new Serializable[] {this.wordEmbedding.clone(), this.annModel.clone()});

        return parameter;
    }

    @Override
    public void save(Map<String, Serializable> state) {
        state.put(WORD_EMBEDDING_KEY, this.wordEmbedding);
        state.put(ANN_MODEL_KEY, this.annModel);
    }

    @Override
    public void load(JsonObject jsonObject) {
        LOGGER.info("Reading parameter factory state...");

        if (jsonObject.has(WORD_EMBEDDING_KEY)) {
            WordEmbedding persistedWordEmbedding =
                    new Gson().fromJson(jsonObject.getAsJsonObject(WORD_EMBEDDING_KEY), WordEmbedding.class);
            if (this.wordVectorSize == persistedWordEmbedding.getWordVectorSize()
                    && this.dictionary.words().equals(persistedWordEmbedding.getDictionary())) {
                wordEmbedding = persistedWordEmbedding;
                LOGGER.info("Word embedding " + wordEmbedding + " has been loaded.");

                // scale word embedding
                // LOGGER.info("Scale word embedding to [-1, 1]");
                // scaleWordEmbedding(wordEmbedding);
                // LogUtility.logWordEmbedding(LOGGER, wordEmbedding);
            }
        }

        if (this.wordEmbedding == null) {
            LOGGER.info("Word embedding configuration does not match. Ingore the state file.");
        }

        // only reuse other saved states if the ANN model configuration is identical
        if (jsonObject.has(ANN_MODEL_KEY)) {
            this.persistedAnnModel =
                    (DefaultAnnModel) new Gson().fromJson(jsonObject.get(ANN_MODEL_KEY), DefaultAnnModel.class);
            if (this.annConfiguration.equals(persistedAnnModel.getConfiguration())) {
                LOGGER.info("Loading ANN model data because the ANN model configuration is identical.");
                this.annModel = persistedAnnModel;
            }
        }

        if (this.annModel == null) {
            LOGGER.info("Ignore ANN and delta sum squre data because the ANN model configuration is differenct.");
        }
    }

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
}
