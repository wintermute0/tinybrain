package yatan.ann;

import java.util.List;

import yatan.commons.matrix.Matrix;

public interface AnnModel {
    public int getLayerCount();

    public Matrix getLayer(int i);

    public int getMaxOutputDegree();

    public AnnConfiguration getConfiguration();

    public void update(AnnGradient gradient, double learningRate);

    public void update(AnnGradient gradient, double rho, double epsilon, List<Matrix> annGradientSqureSum,
            List<Matrix> deltaAnnSquareSum);

    public void update(AnnGradient gradient, double lampda, List<Matrix> annDeltaSqureSum);

    public void update(AnnGradient gradient, double lampda, List<Matrix> annDeltaSqureSum, int sliceId, int totalSlice);

    public void postProcessAnnGradient(AnnGradient annGradient);

    public void reuseLowerLayer(AnnModel annModel, int discardTopNLayer);
}