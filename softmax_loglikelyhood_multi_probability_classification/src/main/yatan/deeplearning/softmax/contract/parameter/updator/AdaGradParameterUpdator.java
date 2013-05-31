package yatan.deeplearning.softmax.contract.parameter.updator;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;

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
    private static final String WORD_EMBEDDING_KEY = "wordEmbedding";

    private static final Logger LOGGER = Logger.getLogger(AdaGradParameterUpdator.class);

    @Inject
    private Dictionary dictionary;
    @Inject
    private AnnConfiguration annConfiguration;
    @Inject
    @Named("word_vector_size")
    private int wordVectorSize;

    private Matrix wordEmbeddingGradientSumSquare;
    private List<Matrix> annDeltaGradientSumSquare;

    @Inject(optional = true)
    @Named("word_embedding_lambda")
    private double wordEmbeddingLambda = 0.1;
    @Inject(optional = true)
    @Named("ann_lambda")
    private double annLambda = 0.1;

    @Override
    public void update(Parameter parameter, Parameter gradient, int sliceId, int totalSlice) {
        Serializable[] serializableParameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) serializableParameters[0];
        AnnModel annModel = (AnnModel) serializableParameters[1];

        Serializable[] inputData = (Serializable[]) gradient.getSerializable();
        AnnGradient annGradient = (AnnGradient) inputData[0];
        // annModel.update(annGradient, ADA_DELTA_RHO, ADA_DELTA_EPSILON, annDeltaGradientSumSquare,
        // this.deltaAnnWeightSumSquare);
        annModel.update(annGradient, this.annLambda, annDeltaGradientSumSquare, sliceId, totalSlice);

        @SuppressWarnings("unchecked")
        Map<Integer, Double[]> wordEmbeddingDelta = (Map<Integer, Double[]>) inputData[1];
        // wordEmbedding.update(wordEmbeddingDelta, ADA_DELTA_RHO, ADA_DELTA_EPSILON,
        // this.wordEmbeddingGradientSumSquare,
        // this.deltaWordEmbeddingSumSquare);
        wordEmbedding.update(wordEmbeddingDelta, this.wordEmbeddingLambda, wordEmbeddingGradientSumSquare, sliceId,
                totalSlice);
    }

    @Override
    public void save(Map<String, Serializable> state) {
        state.put(ANN_DELTA_WEIGHT_SUM_SQUARE_KEY, (Serializable) this.annDeltaGradientSumSquare);
        state.put(WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY, this.wordEmbeddingGradientSumSquare);
    }

    @SuppressWarnings({"unchecked", "serial"})
    @Override
    public void load(JsonObject jsonObject) {
        LOGGER.info("Initializing " + getClass().getSimpleName() + " with word embedding lambda = "
                + this.wordEmbeddingLambda + ", ann lambda = " + this.annLambda);

        // only reuse other saved states if the ANN model configuration is identical
        if (jsonObject.has(ANN_MODEL_KEY)) {
            AnnModel annModel =
                    (DefaultAnnModel) new Gson().fromJson(jsonObject.get(ANN_MODEL_KEY), DefaultAnnModel.class);
            if (annModel.getConfiguration().equals(this.annConfiguration)) {
                if (jsonObject.has(ANN_DELTA_WEIGHT_SUM_SQUARE_KEY)) {
                    LOGGER.info("Loading ANN AdaGrad data...");
                    this.annDeltaGradientSumSquare =
                            (List<Matrix>) new Gson().fromJson(jsonObject.get(ANN_DELTA_WEIGHT_SUM_SQUARE_KEY),
                                    new TypeToken<List<Matrix>>() {
                                    }.getType());
                }

                if (jsonObject.has(WORD_EMBEDDING_KEY)) {
                    WordEmbedding wordEmbedding =
                            (WordEmbedding) new Gson()
                                    .fromJson(jsonObject.get(WORD_EMBEDDING_KEY), WordEmbedding.class);
                    if (wordEmbedding.getDictionary().equals(this.dictionary.words())
                            && wordEmbedding.getWordVectorSize() == this.wordVectorSize) {
                        if (jsonObject.has(WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY)) {
                            LOGGER.info("Loading word embedding AdaGrad data...");
                            this.wordEmbeddingGradientSumSquare =
                                    (Matrix) new Gson().fromJson(jsonObject.get(WORD_EMBEDDING_WEIGHT_SUM_SQUARE_KEY),
                                            Matrix.class);
                        }
                    }
                }
            }
        }

        if (this.annDeltaGradientSumSquare == null) {
            LOGGER.info("Create new ANN AdaGrad data.");
            AnnModel annModel = new DefaultAnnModel(this.annConfiguration);
            this.annDeltaGradientSumSquare = Lists.newArrayList();
            for (int i = 0; i < this.annConfiguration.layers.size(); i++) {
                Matrix layer = annModel.getLayer(i);
                this.annDeltaGradientSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
            }
        }
        if (this.wordEmbeddingGradientSumSquare == null) {
            LOGGER.info("Create new word embedding AdaGrad.");
            this.wordEmbeddingGradientSumSquare = new Matrix(wordVectorSize, dictionary.size());
        }
    }
}
