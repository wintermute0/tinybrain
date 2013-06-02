package yatan.ann;

import java.util.Date;

import java.util.List;
import java.util.Random;

import yatan.commons.matrix.Matrix;

public class DropoutAnnModel implements AnnModel {
    private static final Random RANDOM = new Random(new Date().getTime());

    private final AnnModel annModel;
    private final boolean training;
    private final boolean[][] dropoutMasks;

    private final Matrix[] layers;
    private final Matrix[] reusedLayers;

    public DropoutAnnModel(AnnModel annModel, boolean training) {
        this(annModel, training, null);
    }

    public DropoutAnnModel(AnnModel annModel, boolean training, DropoutAnnModel reuse) {
        this.annModel = annModel;
        this.training = training;
        if (reuse != null) {
            this.reusedLayers = reuse.layers;
        } else {
            this.reusedLayers = null;
        }

        if (this.training) {
            this.dropoutMasks = new boolean[this.annModel.getLayerCount() - 1][];
            for (int i = 0; i < this.dropoutMasks.length; i++) {
                this.dropoutMasks[i] = new boolean[annModel.getConfiguration().layers.get(i)];
                for (int j = 0; j < this.dropoutMasks[i].length; j++) {
                    this.dropoutMasks[i][j] = RANDOM.nextBoolean();
                }
            }
        } else {
            this.dropoutMasks = new boolean[0][0];
        }

        this.layers = new Matrix[annModel.getLayerCount()];
    }

    protected boolean[][] getDropoutMasks() {
        return this.dropoutMasks;
    }

    @Override
    public int getLayerCount() {
        return this.annModel.getLayerCount();
    }

    @Override
    public Matrix getLayer(int i) {
        if (this.layers[i] == null) {
            if (this.training) {
                // dropout neurons
                Matrix matrix = this.annModel.getLayer(i);
                Matrix dropoutMatrix;
                if (this.reusedLayers != null && this.reusedLayers.length > i && reusedLayers[i] != null
                        && reusedLayers[i].rowSize() == matrix.rowSize()
                        && reusedLayers[i].columnSize() == matrix.columnSize()) {
                    dropoutMatrix = this.reusedLayers[i];
                } else {
                    dropoutMatrix = new Matrix(matrix.rowSize(), matrix.columnSize());
                }

                // for (int column = 0; column < dropoutMatrix.columnSize(); column++) {
                // // drop neurons if we're in training
                // if (i < this.annModel.getLayerCount() - 1 && this.dropoutMasks[i][column]) {
                // for (int row = 0; row < dropoutMatrix.rowSize(); row++) {
                // dropoutMatrix.getData()[row][column] = 0;
                // }
                // } else {
                // for (int row = 0; row < dropoutMatrix.rowSize(); row++) {
                // if (i > 0 && row < dropoutMatrix.rowSize() - 1 && this.dropoutMasks[i - 1][row]) {
                // // dropout the output weights according to the dropout mask for the last layer
                // dropoutMatrix.getData()[row][column] = 0;
                // } else {
                // dropoutMatrix.getData()[row][column] = matrix.getData()[row][column];
                // }
                // }
                // }
                // }

                for (int row = 0; row < dropoutMatrix.rowSize(); row++) {
                    for (int column = 0; column < dropoutMatrix.columnSize(); column++) {
                        if (i < this.annModel.getLayerCount() - 1 && this.dropoutMasks[i][column]) {
                            dropoutMatrix.getData()[row][column] = 0;
                        } else {
                            if (i > 0 && row < dropoutMatrix.rowSize() - 1 && this.dropoutMasks[i - 1][row]) {
                                // dropout the output weights according to the dropout mask for the last layer
                                dropoutMatrix.getData()[row][column] = 0;
                            } else {
                                dropoutMatrix.getData()[row][column] = matrix.getData()[row][column];
                            }
                        }
                    }
                }

                this.layers[i] = dropoutMatrix;
            } else {
                // half the output weights if we're not in training
                Matrix matrix = this.annModel.getLayer(i);
                Matrix dropoutMatrix = matrix;

                // do not do anything to the weights of the first layer
                if (i > 0) {
                    if (this.reusedLayers != null && this.reusedLayers.length > i && reusedLayers[i] != null
                            && reusedLayers[i].rowSize() == matrix.rowSize()
                            && reusedLayers[i].columnSize() == matrix.columnSize()) {
                        dropoutMatrix = this.reusedLayers[i];
                    } else {
                        dropoutMatrix = new Matrix(matrix.rowSize(), matrix.columnSize());
                    }

                    for (int row = 0; row < matrix.rowSize() - 1; row++) {
                        for (int column = 0; column < matrix.columnSize(); column++) {
                            dropoutMatrix.getData()[row][column] = matrix.getData()[row][column] / 2;
                        }
                    }

                    for (int column = 0; column < matrix.columnSize(); column++) {
                        dropoutMatrix.getData()[matrix.rowSize() - 1][column] =
                                matrix.getData()[matrix.rowSize() - 1][column];
                    }
                }

                this.layers[i] = dropoutMatrix;
            }
        }

        return this.layers[i];
    }

    @Override
    public AnnConfiguration getConfiguration() {
        return this.annModel.getConfiguration();
    }

    @Override
    public void update(AnnGradient gradient, double learningRate) {
        this.annModel.update(gradient, learningRate);
    }

    @Override
    public void update(AnnGradient gradient, double rho, double epsilon, List<Matrix> annGradientSqureSum,
            List<Matrix> deltaAnnSquareSum) {
        this.annModel.update(gradient, rho, epsilon, annGradientSqureSum, deltaAnnSquareSum);
    }

    @Override
    public void postProcessAnnGradient(AnnGradient annGradient) {
        // for (int layer = 0; layer < annGradient.getGradients().size(); layer++) {
        // Matrix gradient = annGradient.getGradients().get(layer);
        // for (int column = 0; column < gradient.columnSize(); column++) {
        // // drop neurons if we're in training
        // if (layer < this.annModel.getLayerCount() - 1 && this.dropoutMasks[layer][column]) {
        // for (int row = 0; row < gradient.rowSize(); row++) {
        // gradient.getData()[row][column] = 0;
        // }
        // } else {
        // for (int row = 0; row < gradient.rowSize() - 1; row++) {
        // if (layer > 0 && this.dropoutMasks[layer - 1][row]) {
        // // dropout the output weights according to the dropout mask for the last layer
        // gradient.getData()[row][column] = 0;
        // }
        // }
        // }
        // }
        // }

        // FIXME: what about gradient of input?
    }

    @Override
    public void update(AnnGradient gradient, double lampda, List<Matrix> annDeltaSqureSum) {
        this.annModel.update(gradient, lampda, annDeltaSqureSum);
    }

    @Override
    public void update(AnnGradient gradient, double lampda, List<Matrix> annDeltaSqureSum, int sliceId, int totalSlice) {
        this.annModel.update(gradient, lampda, annDeltaSqureSum, sliceId, totalSlice);
    }

    @Override
    public void reuseLowerLayer(AnnModel annModel, int discardTopNLayer) {
        this.annModel.reuseLowerLayer(annModel, discardTopNLayer);
    }
}
