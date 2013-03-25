package yatan.ann;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import yatan.commons.matrix.Matrix;

@SuppressWarnings("serial")
public class AnnModel implements Serializable {
    // private final Logger logger = Logger.getLogger(AnnModel.class);
    private final Configuration configuration;
    private final List<Matrix> matrices = new ArrayList<Matrix>();
    private final int maxOutputDegree;

    public AnnModel(Configuration configuration) {
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
            Matrix matrix =
                    new Matrix(this.configuration.bias.get(i) ? lastDegree + 1 : lastDegree,
                            this.configuration.layers.get(i));
            matrix.randomInitialize();
            this.matrices.add(matrix);
            lastDegree = this.configuration.layers.get(i);
        }
    }

    public int getLayerCount() {
        return this.configuration.layers.size();
    }

    public Matrix getLayer(int i) {
        return this.matrices.get(i);
    }

    public int getMaxOutputDegree() {
        return this.maxOutputDegree;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void update(AnnGradient gradient, double learningRate) {
        if (gradient.getGradients().size() != this.matrices.size()) {
            throw new IllegalArgumentException("Gradient layer count " + gradient.getGradients().size()
                    + " does not match the layer count of this model, which is " + this.matrices.size());
        }

        for (int i = 0; i < this.matrices.size(); i++) {
            this.matrices.get(i).update(gradient.getGradients().get(i), learningRate);
        }
    }

    public void update(AnnGradient gradient, double lampda, List<Matrix> annDeltaSqureSum) {
        if (gradient.getGradients().size() != this.matrices.size()) {
            throw new IllegalArgumentException("Gradient layer count does not match.");
        }

        for (int i = 0; i < this.matrices.size(); i++) {
            this.matrices.get(i).update(gradient.getGradients().get(i), lampda, annDeltaSqureSum.get(i));
        }
    }

    public static class Configuration implements Serializable {
        public static enum ActivationFunction {
            Y_EQUALS_X, TANH, SIGMOID, SOFTMAX(true);

            private boolean multipleInput;

            private ActivationFunction() {
            }

            private ActivationFunction(boolean multipleInput) {
                this.multipleInput = multipleInput;
            }

            public boolean isMultiInput() {
                return this.multipleInput;
            }
        }

        private final int inputDegree;
        private final List<Integer> layers = new ArrayList<Integer>();
        private final List<ActivationFunction> activationFunctions =
                new ArrayList<AnnModel.Configuration.ActivationFunction>();
        private final List<Boolean> bias = new ArrayList<Boolean>();

        public Configuration(int inputDegree) {
            this.inputDegree = inputDegree;
        }

        public Configuration addLayer(int units, ActivationFunction activationFunction, boolean bias) {
            this.layers.add(units);
            this.activationFunctions.add(activationFunction);
            this.bias.add(bias);

            return this;
        }

        public Configuration addLayer(int units, ActivationFunction activationFunction) {
            return addLayer(units, activationFunction, true);
        }

        public ActivationFunction activationFunctionOfLayer(int layer) {
            return this.activationFunctions.get(layer);
        }

        public boolean isLayerBiased(int layer) {
            return this.bias.get(layer);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AnnConfiguration: [Input degree = " + this.inputDegree);
            for (int i = 0; i < this.layers.size(); i++) {
                sb.append(", layer ").append(i).append(" = (").append(this.layers.get(i)).append(", ")
                        .append(this.activationFunctions.get(i)).append(", ")
                        .append(this.bias.get(i) ? "baised" : "not-baised").append(")");
            }
            sb.append("]");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((activationFunctions == null) ? 0 : activationFunctions.hashCode());
            result = prime * result + ((bias == null) ? 0 : bias.hashCode());
            result = prime * result + inputDegree;
            result = prime * result + ((layers == null) ? 0 : layers.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Configuration other = (Configuration) obj;
            if (activationFunctions == null) {
                if (other.activationFunctions != null)
                    return false;
            } else if (!activationFunctions.equals(other.activationFunctions))
                return false;
            if (bias == null) {
                if (other.bias != null)
                    return false;
            } else if (!bias.equals(other.bias))
                return false;
            if (inputDegree != other.inputDegree)
                return false;
            if (layers == null) {
                if (other.layers != null)
                    return false;
            } else if (!layers.equals(other.layers))
                return false;
            return true;
        }
    }
}
