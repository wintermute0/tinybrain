package yatan.ann;

import java.util.List;

@SuppressWarnings("serial")
public class SparseAnnData extends AnnData {
    private int inputDimension;
    private List<Integer> inputActiveIndex;
    private int outputDimension;
    private List<Integer> outputActiveIndex;

    private double[] expandedInput;
    private double[] expandedOutput;

    public SparseAnnData(int inputDimension, List<Integer> inputActiveIndex, int outputDimension,
            List<Integer> outputActiveIndex) {
        super(null, null);

        this.inputDimension = inputDimension;
        this.inputActiveIndex = inputActiveIndex;
        this.outputDimension = outputDimension;
        this.outputActiveIndex = outputActiveIndex;
    }

    @Override
    public double[] getInput() {
        if (this.expandedInput == null) {
            this.expandedInput = generateBinaryArray(this.inputDimension, this.inputActiveIndex);
        }

        return this.expandedInput;
    }

    @Override
    public double[] getOutput() {
        if (this.expandedOutput == null) {
            this.expandedOutput = generateBinaryArray(this.outputDimension, this.outputActiveIndex);
        }

        return this.expandedOutput;
    }

    private double[] generateBinaryArray(int dimension, List<Integer> activeIndex) {
        double[] array = new double[dimension];

        for (int index : activeIndex) {
            array[index] = 1;
        }

        return array;
    }
}
