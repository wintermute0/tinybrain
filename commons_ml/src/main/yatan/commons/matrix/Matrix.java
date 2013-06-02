package yatan.commons.matrix;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import com.google.common.base.Preconditions;

@SuppressWarnings("serial")
final public class Matrix implements Serializable, Cloneable {
    private final double[][] data;

    public Matrix(int row, int column) {
        if (row <= 0) {
            throw new IllegalArgumentException("Row size must be positive.");
        }
        if (column <= 0) {
            throw new IllegalArgumentException("Column size must be positive.");
        }

        this.data = new double[row][column];
    }

    public Matrix(double[][] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null.");
        }

        this.data = data;
    }

    @Override
    public Matrix clone() {
        double[][] clonedData = new double[this.data.length][];
        for (int i = 0; i < this.data.length; i++) {
            clonedData[i] = Arrays.copyOf(this.data[i], this.data[i].length);
        }

        return new Matrix(clonedData);
    }

    public void randomInitialize(double low, double high) {
        Random random = new Random(new Date().getTime());
        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                this.data[i][j] = low + (high - low) * random.nextDouble();
            }
        }
    }

    public void randomInitialize() {
        randomInitialize(-1, 1);
    }

    public Matrix transpose() {
        double[][] newData = new double[columnSize()][rowSize()];
        for (int i = 0; i < columnSize(); i++) {
            for (int j = 0; j < rowSize(); j++) {
                newData[i][j] = this.data[j][i];
            }
        }

        return new Matrix(newData);
    }

    public Matrix multiply(Matrix m) {
        double[][] result = new double[rowSize()][m.columnSize()];

        double[][] data2 = m.getData();
        for (int i = 0; i < rowSize(); i++) {
            for (int k = 0; k < columnSize(); k++) {
                for (int j = 0; j < m.columnSize(); j++) {
                    result[i][j] += this.data[i][k] * data2[k][j];
                }
            }
        }

        return new Matrix(result);
    }

    public double[] multiply(double[] vec) {
        if (columnSize() != vec.length) {
            throw new IllegalArgumentException("The vector length must be equal to the column number of the data.");
        }

        double[] result = new double[rowSize()];
        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < vec.length; j++) {
                result[i] += this.data[i][j] * vec[j];
            }
        }

        return result;
    }

    public double[] multiplyBy(double[] vector) {
        if (vector.length != this.data.length) {
            throw new IllegalArgumentException("Vector size " + vector.length + " does equal to the row number "
                    + this.data.length + " of the matrix.");
        }

        double[] result = new double[this.data[0].length];
        for (int j = 0; j < vector.length; j++) {
            for (int i = 0; i < result.length; i++) {
                result[i] += vector[j] * this.data[j][i];
            }
        }

        return result;
    }

    public void update(Matrix gradient, double learningRate) {
        if (rowSize() != gradient.rowSize() || columnSize() != gradient.columnSize()) {
            throw new IllegalArgumentException("The size of the gradient matrix does not match.");
        }

        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                this.data[i][j] += gradient.getData()[i][j] * learningRate;
            }
        }
    }

    public void update(Matrix gradient, double rho, double epsilon, Matrix gradientSquareSum, Matrix deltaSquareSum) {
        if (rowSize() != gradient.rowSize() || columnSize() != gradient.columnSize()) {
            throw new IllegalArgumentException("The size of the gradient matrix does not match.");
        }
        Preconditions.checkArgument(rho < 1 && rho > 0.5, "Rho must be inside (0.5, 1).");
        Preconditions.checkArgument(epsilon < 0.1, "Epsilon must < 0.1");
        Preconditions.checkArgument(deltaSquareSum != null, "Delta square sum matrix cannot be empty.");
        Preconditions.checkArgument(deltaSquareSum.rowSize() == rowSize()
                && deltaSquareSum.columnSize() == columnSize(),
                "Delta square sum matrix must be the same size as gradient matrix.");

        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                gradientSquareSum.getData()[i][j] =
                        rho * gradientSquareSum.getData()[i][j] + (1 - rho) * Math.pow(gradient.getData()[i][j], 2);

                double learningRate =
                        Math.sqrt(deltaSquareSum.getData()[i][j] + epsilon)
                                / Math.sqrt(gradientSquareSum.getData()[i][j] + epsilon);
                double delta = learningRate * gradient.getData()[i][j];

                this.data[i][j] += delta;

                deltaSquareSum.getData()[i][j] = rho * deltaSquareSum.getData()[i][j] + (1 - rho) * Math.pow(delta, 2);
            }
        }
    }

    public void update(Matrix gradient, double lambda, Matrix annDeltaSqureSum) {
        update(gradient, lambda, annDeltaSqureSum, 0, 1);
    }

    public void update(Matrix gradient, double lambda, Matrix annDeltaSqureSum, int sliceId, int totalSlice) {
        Preconditions.checkArgument(gradient != null);
        Preconditions.checkArgument(annDeltaSqureSum != null);
        Preconditions.checkArgument(sliceId < totalSlice && sliceId >= 0);

        if (rowSize() != gradient.rowSize() || columnSize() != gradient.columnSize()) {
            throw new IllegalArgumentException("The size of the gradient matrix does not match.");
        }

        int columnStart = sliceId * (columnSize() / totalSlice);
        int columnEnd = sliceId == totalSlice - 1 ? columnSize() : (sliceId + 1) * (columnSize() / totalSlice);
        for (int i = 0; i < rowSize(); i++) {
            for (int j = columnStart; j < columnEnd; j++) {
                annDeltaSqureSum.getData()[i][j] += Math.pow(gradient.getData()[i][j], 2);
                double learningRate = lambda / Math.sqrt(annDeltaSqureSum.getData()[i][j]);

                if (learningRate > lambda) {
                    learningRate = lambda;
                }

                double deltaW = gradient.getData()[i][j] * learningRate;
                this.data[i][j] += deltaW;
            }
        }
    }

    public void updateByPlus(Matrix matrix) {
        if (rowSize() != matrix.rowSize() || columnSize() != matrix.columnSize()) {
            throw new IllegalArgumentException("The size of the matrix does not match.");
        }

        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                this.data[i][j] += matrix.getData()[i][j];
            }
        }
    }

    public int rowSize() {
        return this.data.length;
    }

    public int columnSize() {
        return this.data[0].length;
    }

    public double[][] getData() {
        return this.data;
    }

    public void setData(double[][] data) {
        if (this.data.length != data.length) {
            throw new IllegalArgumentException("Data row number does not match.");
        }

        for (int i = 0; i < this.data.length; i++) {
            for (int j = 0; j < this.data[i].length; j++) {
                if (this.data[i].length != data[i].length) {
                    throw new IllegalArgumentException("Size of row " + i + " does not match.");
                }

                this.data[i][j] = data[i][j];
            }
        }
    }

    // public String toString() {
    // StringBuilder sb = new StringBuilder();
    //
    // for (int i = 0; i < this.data.length; i++) {
    // for (int j = 0; j < this.data[i].length; j++) {
    // sb.append(this.data[i][j]).append(" ");
    // }
    // sb.append("\n");
    // }
    //
    // return sb.toString();
    // }
}
