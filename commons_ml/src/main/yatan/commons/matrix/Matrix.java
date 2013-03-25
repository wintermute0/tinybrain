package yatan.commons.matrix;

import java.io.Serializable;

import java.util.Date;
import java.util.Random;

@SuppressWarnings("serial")
final public class Matrix implements Serializable {
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

    public void randomInitialize() {
        Random random = new Random(new Date().getTime());
        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                this.data[i][j] = (random.nextDouble() - 0.5) / 5;
            }
        }
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

    public void update(Matrix gradient, double lampda, Matrix annDeltaSqureSum) {
        if (rowSize() != gradient.rowSize() || columnSize() != gradient.columnSize()) {
            throw new IllegalArgumentException("The size of the gradient matrix does not match.");
        }

        for (int i = 0; i < rowSize(); i++) {
            for (int j = 0; j < columnSize(); j++) {
                if (gradient.getData()[i][j] == 0) {
                    continue;
                }

                annDeltaSqureSum.getData()[i][j] += Math.pow(gradient.getData()[i][j], 2);
                double learningRate = lampda / Math.sqrt(annDeltaSqureSum.getData()[i][j]);
                this.data[i][j] += gradient.getData()[i][j] * learningRate;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < this.data.length; i++) {
            for (int j = 0; j < this.data[i].length; j++) {
                sb.append(this.data[i][j]).append(" ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
