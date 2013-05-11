package yatan.distributed.ml.ann;

public class AnnTrainerConfiguration {
    public static enum LossFunction {
        LeastSquare, SoftmaxLoglikelyhood
    }

    public LossFunction lossFunction = LossFunction.LeastSquare;
}
