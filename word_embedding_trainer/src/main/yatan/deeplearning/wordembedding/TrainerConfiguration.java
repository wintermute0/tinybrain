package yatan.deeplearning.wordembedding;

public class TrainerConfiguration {
    /**
     * The last one is for input
     */
    // public static double l2Lambdas[] = new double[] {0.0001, 0.0005, 0.001};
    public double l2Lambdas[] = null;

    public int hiddenLayerSize = 100;
    public int wordVectorSize = 50;

    public boolean dropout = false;
    public boolean wordEmbeddingDropout = false;
    public double wordEmbeddingDropoutRate = 0.2;
}
