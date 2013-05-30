package yatan.deeplearning.softmax.contract.parameter.updator;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.DefaultAnnModel;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.softmax.contract.parameter.ParameterUpdator;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.distributedcomputer.Parameter;

public class AdaGradParameterUpdator implements ParameterUpdator {
    private static final String WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY = "wordEmbeddingWeightSumSquare";

    private static final String ANN_DELTA_WEIGHT_SUM_SQUARE_KEY = "annDeltaWeightSumSquare";

    private static final String ANN_MODEL_KEY = "annModel";

    private static final Logger LOGGER = Logger.getLogger(AdaGradParameterUpdator.class);

    @Inject
    private Dictionary dictionary;
    @Inject
    private int wordVectorSize;
    @Inject
    private AnnConfiguration annConfiguration;

    private Matrix wordEmbeddingGradientSumSquare;
    private List<Matrix> annDeltaGradientSumSquare;

    @Override
    public void update(Parameter parameter, Parameter gradient, int sliceId, int totalSlice) {
        Serializable[] serializableParameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) serializableParameters[0];
        AnnModel annModel = (AnnModel) serializableParameters[1];

        Serializable[] inputData = (Serializable[]) gradient.getSerializable();
        AnnGradient annGradient = (AnnGradient) inputData[0];
        // annModel.update(annGradient, ADA_DELTA_RHO, ADA_DELTA_EPSILON, annDeltaGradientSumSquare,
        // this.deltaAnnWeightSumSquare);
        annModel.update(annGradient, 0.1, annDeltaGradientSumSquare, sliceId, totalSlice);

        @SuppressWarnings("unchecked")
        Map<Integer, Double[]> wordEmbeddingDelta = (Map<Integer, Double[]>) inputData[1];
        // wordEmbedding.update(wordEmbeddingDelta, ADA_DELTA_RHO, ADA_DELTA_EPSILON,
        // this.wordEmbeddingGradientSumSquare,
        // this.deltaWordEmbeddingSumSquare);
        wordEmbedding.update(wordEmbeddingDelta, 0.1, wordEmbeddingGradientSumSquare, sliceId, totalSlice);
    }

    @Override
    public void save(Map<String, Serializable> state) {
        state.put(ANN_DELTA_WEIGHT_SUM_SQUARE_KEY, (Serializable) this.annDeltaGradientSumSquare);
        state.put(WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY, this.wordEmbeddingGradientSumSquare);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load(Map<String, Serializable> state) {
        // only reuse other saved states if the ANN model configuration is identical
        if (state.containsKey(ANN_MODEL_KEY)) {
            DefaultAnnModel persistedAnnModel = (DefaultAnnModel) state.get(ANN_MODEL_KEY);
            if (this.annConfiguration.equals(persistedAnnModel.getConfiguration())) {
                if (state.containsKey(ANN_DELTA_WEIGHT_SUM_SQUARE_KEY)) {
                    LOGGER.info("Loading ANN AdaGrad data...");
                    this.annDeltaGradientSumSquare = (List<Matrix>) state.get(ANN_DELTA_WEIGHT_SUM_SQUARE_KEY);
                }
                if (state.containsKey(WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY)) {
                    LOGGER.info("Loading word embedding AdaGrad data...");
                    this.wordEmbeddingGradientSumSquare = (Matrix) state.get(WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY);
                }
            }
        }

        if (this.annDeltaGradientSumSquare == null) {
            LOGGER.info("Create new ANN AdaGrad data.");
            this.annDeltaGradientSumSquare = Lists.newArrayList();
        }
        if (this.wordEmbeddingGradientSumSquare == null) {
            LOGGER.info("Create new word embedding AdaGrad.");
            this.wordEmbeddingGradientSumSquare = new Matrix(wordVectorSize, dictionary.size());
        }
    }
}
