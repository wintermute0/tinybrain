package yatan.ann;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.List;

import com.google.common.base.Preconditions;

import yatan.commons.matrix.Matrix;

@SuppressWarnings("serial")
public class DefaultAnnModel implements Serializable, AnnModel {
    // private final Logger logger = Logger.getLogger(AnnModel.class);
    private final AnnConfiguration configuration;
    private final List<Matrix> matrices = new ArrayList<Matrix>();
    private final int maxOutputDegree;

    public DefaultAnnModel(AnnConfiguration configuration) {
        Preconditions.checkArgument(configuration != null);

        // this.logger.info("Creating ANN with " + configuration);

        this.configuration = configuration;

        // calculate the maximum output degree
        int maxOutputDegree = 0;
        for (Integer degree : this.configuration.layers) {
            if (degree > maxOutputDegree) {
                maxOutputDegree = degree;
            }
        }

        this.maxOutputDegree = maxOutputDegree;

        // allocate matrices
        int lastDegree = this.configuration.inputDegree;
        for (int i = 0; i < this.configuration.layers.size(); i++) {
            Matrix matrix = new Matrix(lastDegree + 1, this.configuration.layers.get(i));
            double absMaxValue =
                    Math.sqrt(6.0 / (lastDegree + (i == this.configuration.layers.size() - 1 ? 1
                            : this.configuration.layers.get(i + 1))));
            matrix.randomInitialize(-absMaxValue, absMaxValue);
            this.matrices.add(matrix);
            lastDegree = this.configuration.layers.get(i);

            // if this layer is not biased, set the biase weight to zero
            if (!this.configuration.biased.get(i)) {
                int lastRow = matrix.rowSize() - 1;
                for (int column = 0; column < matrix.columnSize(); column++) {
                    matrix.getData()[lastRow][column] = 0;
                }
            }
        }
    }

    @Override
    public int getLayerCount() {
        return this.configuration.layers.size();
    }

    @Override
    public Matrix getLayer(int i) {
        return this.matrices.get(i);
    }

    @Override
    public int getMaxOutputDegree() {
        return this.maxOutputDegree;
    }

    @Override
    public AnnConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void update(AnnGradient gradient, double learningRate) {
        if (gradient.getGradients().size() != this.matrices.size()) {
            throw new IllegalArgumentException("Gradient layer count " + gradient.getGradients().size()
                    + " does not match the layer count of this model, which is " + this.matrices.size());
        }

        for (int i = 0; i < this.matrices.size(); i++) {
            this.matrices.get(i).update(gradient.getGradients().get(i), learningRate);
        }
    }

    @Override
    public void update(AnnGradient gradient, double rho, double epsilon, List<Matrix> annGradientSqureSum,
            List<Matrix> deltaAnnSquareSum) {
        if (gradient.getGradients().size() != this.matrices.size()) {
            throw new IllegalArgumentException("Gradient layer count does not match.");
        }

        for (int i = 0; i < this.matrices.size(); i++) {
            if (gradient.getGradients().get(i) != null) {
                this.matrices.get(i).update(gradient.getGradients().get(i), rho, epsilon, annGradientSqureSum.get(i),
                        deltaAnnSquareSum.get(i));
            }
        }
    }

    @Override
    public void update(AnnGradient gradient, double lampda, List<Matrix> annDeltaSqureSum, double momentumWeight,
            List<Matrix> lastDeltaW) {
        if (gradient.getGradients().size() != this.matrices.size()) {
            throw new IllegalArgumentException("Gradient layer count does not match.");
        }

        for (int i = 0; i < this.matrices.size(); i++) {
            this.matrices.get(i).update(gradient.getGradients().get(i), lampda, annDeltaSqureSum.get(i),
                    momentumWeight, lastDeltaW.get(i));
        }
    }

    @Override
    public void postProcessAnnGradient(AnnGradient annGradient) {
        // do nothing
    }

    @Override
    public void reuseLowerLayer(AnnModel annModel) {
        for (int layer = 0; layer < Math.min(annModel.getLayerCount(), getLayerCount()); layer++) {
            if (annModel.getConfiguration().activationFunctionOfLayer(layer) == this.configuration
                    .activationFunctionOfLayer(layer)) {
                Matrix reuse = annModel.getLayer(layer);
                Matrix current = getLayer(layer);
                if (reuse.columnSize() == current.columnSize() && reuse.rowSize() == current.rowSize()) {
                    for (int i = 0; i < reuse.rowSize(); i++) {
                        for (int j = 0; j < reuse.columnSize(); j++) {
                            current.getData()[i][j] = reuse.getData()[i][j];
                        }
                    }
                }
            }
        }
    }
}
