package yatan.ann;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class AnnConfiguration implements Serializable {
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

    public final int inputDegree;
    public final List<Integer> layers = new ArrayList<Integer>();
    public final List<Boolean> biased = Lists.newArrayList();

    private final List<AnnConfiguration.ActivationFunction> activationFunctions =
            new ArrayList<AnnConfiguration.ActivationFunction>();

    public AnnConfiguration(int inputDegree) {
        this.inputDegree = inputDegree;
    }

    public AnnConfiguration addLayer(int units, AnnConfiguration.ActivationFunction activationFunction, boolean biased) {
        this.layers.add(units);
        this.activationFunctions.add(activationFunction);
        this.biased.add(biased);

        return this;
    }

    public AnnConfiguration addLayer(int units, AnnConfiguration.ActivationFunction activationFunction) {
        return addLayer(units, activationFunction, true);
    }

    public AnnConfiguration.ActivationFunction activationFunctionOfLayer(int layer) {
        return this.activationFunctions.get(layer);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AnnConfiguration: [Input degree = " + this.inputDegree);
        for (int i = 0; i < this.layers.size(); i++) {
            sb.append(", layer ").append(i).append(" = (").append(this.layers.get(i)).append(", ")
                    .append(this.activationFunctions.get(i)).append(", ")
                    .append(this.biased.get(i) ? "baised" : "un-baised").append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activationFunctions == null) ? 0 : activationFunctions.hashCode());
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
        AnnConfiguration other = (AnnConfiguration) obj;
        if (activationFunctions == null) {
            if (other.activationFunctions != null)
                return false;
        } else if (!activationFunctions.equals(other.activationFunctions))
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