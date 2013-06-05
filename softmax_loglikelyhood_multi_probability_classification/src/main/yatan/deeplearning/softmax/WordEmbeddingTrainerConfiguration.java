package yatan.deeplearning.softmax;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;

public class WordEmbeddingTrainerConfiguration {
    /**
     * The last one is for input
     */
    public double l2Lambdas[] = new double[] {0.00001, 0.00001, 0.00001, 0.00001, 0.00001};

    public int windowSize = 5;
    public int wordFrequencyRankLowerBound = -1;

    public int hiddenLayerSize = 300;
    public int wordVectorSize = 100;

    public boolean dropout = false;
    public boolean wordEmbeddingDropout = false;
    public double wordEmbeddingDropoutRate = 0.2;

    public String modelFilePrefix = "word_embedding_";

    public AnnConfiguration annConfiguration;

    public int trainingActorCount = 16;
    public int parameterActorUpdateSlice = 8;

    public double wordEmbeddingLambda = 0.1;
    public double annLambda = 0.1;

    public int fixBottomLayers = 0; // ANN_CONFIGURATION.layers.size() - 2;

    public WordEmbeddingTrainerConfiguration() {
        AnnConfiguration annConfiguration = new AnnConfiguration(this.wordVectorSize * this.windowSize);
        annConfiguration.addLayer(hiddenLayerSize, ActivationFunction.TANH);
        annConfiguration.addLayer(hiddenLayerSize, ActivationFunction.TANH);
        annConfiguration.addLayer(hiddenLayerSize, ActivationFunction.TANH);
        annConfiguration.addLayer(1, ActivationFunction.SIGMOID);
    }
}
