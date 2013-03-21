package yatan.ann;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AnnData implements Serializable {
    private double[] input;
    private double[] output;

    public AnnData(double[] input, double[] output) {
        this.input = input;
        this.output = output;
    }

    public double[] getInput() {
        return input;
    }

    public void setInput(double[] input) {
        this.input = input;
    }

    public double[] getOutput() {
        return output;
    }

    public void setOutput(double[] output) {
        this.output = output;
    }
}
