package yatan.deeplearning.softmax;

public class TrainerConfiguration {
    /**
     * The last one is for input
     */
    // public static double l2Lambdas[] = new double[] {0.0001, 0.0005, 0.001};
    public double l2Lambdas[] = new double[] {0.0001, 0.001, 0.001};

    public int hiddenLayerSize = 100;
    public int wordVectorSize = 50;

    public boolean dropout = false;
}
