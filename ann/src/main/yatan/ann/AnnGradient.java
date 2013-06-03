package yatan.ann;

import java.io.Serializable;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import yatan.commons.matrix.Matrix;

@SuppressWarnings("serial")
public class AnnGradient implements Serializable {
    private List<Matrix> gradients;
    private double[] deltaForInputLayer;

    public AnnGradient(List<Matrix> gradients) {
        this.gradients = gradients;
    }

    public AnnGradient(List<Matrix> gradients, double[] deltaForInputLayer) {
        this.gradients = gradients;
        this.deltaForInputLayer = deltaForInputLayer;
    }

    public List<Matrix> getGradients() {
        return gradients;
    }

    public void setGradients(List<Matrix> gradients) {
        this.gradients = gradients;
    }

    public AnnGradient clone() {
        List<Matrix> gradients = Lists.newArrayList();
        for (Matrix matrix : this.gradients) {
            Matrix clone = null;
            if (matrix != null) {
                clone = new Matrix(matrix.rowSize(), matrix.columnSize());
                for (int i = 0; i < matrix.rowSize(); i++) {
                    for (int j = 0; j < matrix.columnSize(); j++) {
                        clone.getData()[i][j] = matrix.getData()[i][j];
                    }
                }
            }

            gradients.add(clone);
        }

        return new AnnGradient(gradients, this.deltaForInputLayer == null ? null : Arrays.copyOf(
                this.deltaForInputLayer, 0));
    }

    public void updateByPlus(AnnGradient gradient) {
        if (this.gradients.size() != gradient.getGradients().size()) {
            throw new IllegalArgumentException("Gradient layer size does not match.");
        }
        /*
         * if (this.deltaForInputLayer != null && this.deltaForInputLayer.length !=
         * gradient.getDeltaForInputLayer().length) { throw new
         * IllegalArgumentException("Delta for input layer size does not match."); }
         */

        // update gradients for all layer
        for (int i = 0; i < this.gradients.size(); i++) {
            if (this.gradients.get(i) != null && gradient.getGradients().get(i) != null) {
                this.gradients.get(i).updateByPlus(gradient.getGradients().get(i));
            }
        }

        /*
         * // update delta for input layer if (this.deltaForInputLayer != null) { for (int i = 0; i <
         * this.deltaForInputLayer.length; i++) { this.deltaForInputLayer[i] += gradient.getDeltaForInputLayer()[i]; } }
         */
    }

    public void averageBy(double batchSize) {
        for (Matrix matrix : this.gradients) {
            if (matrix == null) {
                continue;
            }

            double[][] data = matrix.getData();
            for (int i = 0; i < matrix.rowSize(); i++) {
                for (int j = 0; j < matrix.columnSize(); j++) {
                    data[i][j] /= batchSize;
                }
            }
        }
    }

    public double[] getDeltaForInputLayer() {
        return deltaForInputLayer;
    }

    public void setDeltaForInputLayer(double[] deltaForInputLayer) {
        this.deltaForInputLayer = deltaForInputLayer;
    }
}
