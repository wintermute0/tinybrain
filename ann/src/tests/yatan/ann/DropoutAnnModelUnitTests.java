package yatan.ann;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.commons.matrix.Matrix;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DropoutAnnModelUnitTests {
    private static final double DOUBLE_COMPARE_DELTA = 0.00000001;
    private AnnConfiguration annConfiguration;
    private DefaultAnnModel defaultAnnModel;
    private DropoutAnnModel trainingDropoutAnnModel;
    private DropoutAnnModel runningDropoutAnnModel;

    @Before
    public void setUp() {
        this.annConfiguration = new AnnConfiguration(50);
        this.annConfiguration.addLayer(100, ActivationFunction.TANH);
        this.annConfiguration.addLayer(200, ActivationFunction.TANH);
        this.annConfiguration.addLayer(300, ActivationFunction.TANH);

        this.defaultAnnModel = new DefaultAnnModel(this.annConfiguration);
        this.trainingDropoutAnnModel = new DropoutAnnModel(this.defaultAnnModel, true);
        this.runningDropoutAnnModel = new DropoutAnnModel(this.defaultAnnModel, false);
    }

    @After
    public void tearDown() {
        this.runningDropoutAnnModel = null;
        this.trainingDropoutAnnModel = null;
        this.defaultAnnModel = null;
        this.annConfiguration = null;
    }

    @Test
    public void testGetDropoutMasks_Accuracy() {
        boolean[][] dropoutMasks = this.trainingDropoutAnnModel.getDropoutMasks();
        assertEquals("Should have layer size - 1 dropout masks.", this.defaultAnnModel.getLayerCount() - 1,
                dropoutMasks.length);

        for (int i = 0; i < this.defaultAnnModel.getLayerCount() - 1; i++) {
            Matrix layer = this.defaultAnnModel.getLayer(i);
            // if this is a hidden layer
            boolean[] dropoutMask = dropoutMasks[i];
            assertEquals("Dropout mask size should be the same as neural size", layer.columnSize(), dropoutMask.length);
            boolean foundTrue = false;
            boolean foundFalse = false;
            for (boolean mask : dropoutMask) {
                if (mask) {
                    foundTrue = true;
                } else {
                    foundFalse = true;
                }
            }
            assertTrue("Should found true.", foundTrue);
            assertTrue("Should found false.", foundFalse);
        }
    }

    @Test
    public void testGetLayer_Training_Accuracy() {
        for (int i = 0; i < this.defaultAnnModel.getLayerCount(); i++) {
            Matrix layer = this.defaultAnnModel.getLayer(i);
            Matrix dropoutLayer = this.trainingDropoutAnnModel.getLayer(i);

            if (i < this.defaultAnnModel.getLayerCount() - 1) {
                // if this is a hidden layer
                boolean[] dropoutMask = this.trainingDropoutAnnModel.getDropoutMasks()[i];
                assertEquals("Dropout mask size should be the same as neural size", layer.columnSize(),
                        dropoutMask.length);
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize(); inputIndex++) {
                        if (dropoutMask[neuralIndex]
                                || (i > 0 && inputIndex < layer.rowSize() - 1 && this.trainingDropoutAnnModel
                                        .getDropoutMasks()[i - 1][inputIndex])) {
                            assertEquals("The weight " + i + ", " + inputIndex + ", " + neuralIndex
                                    + " should be dropped.", 0, dropoutLayer.getData()[inputIndex][neuralIndex],
                                    DOUBLE_COMPARE_DELTA);
                        } else {
                            assertEquals("Dropped some weight that should not be.",
                                    layer.getData()[inputIndex][neuralIndex],
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        }
                    }
                }
            } else {
                // if this is the last layer
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize(); inputIndex++) {
                        if (i > 0 && inputIndex < layer.rowSize() - 1
                                && this.trainingDropoutAnnModel.getDropoutMasks()[i - 1][inputIndex]) {
                            assertEquals("The weight should be dropped.", 0,
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        } else {
                            assertEquals("Dropped some weight that should not be.",
                                    layer.getData()[inputIndex][neuralIndex],
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testGetLayer_Training_Reuse_Accuracy() {
        for (int i = 0; i < this.trainingDropoutAnnModel.getLayerCount(); i++) {
            this.trainingDropoutAnnModel.getLayer(i);
        }
        this.trainingDropoutAnnModel = new DropoutAnnModel(this.defaultAnnModel, true, this.trainingDropoutAnnModel);
        for (int i = 0; i < this.defaultAnnModel.getLayerCount(); i++) {
            Matrix layer = this.defaultAnnModel.getLayer(i);
            Matrix dropoutLayer = this.trainingDropoutAnnModel.getLayer(i);

            if (i < this.defaultAnnModel.getLayerCount() - 1) {
                // if this is a hidden layer
                boolean[] dropoutMask = this.trainingDropoutAnnModel.getDropoutMasks()[i];
                assertEquals("Dropout mask size should be the same as neural size", layer.columnSize(),
                        dropoutMask.length);
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize(); inputIndex++) {
                        if (dropoutMask[neuralIndex]
                                || (i > 0 && inputIndex < layer.rowSize() - 1 && this.trainingDropoutAnnModel
                                        .getDropoutMasks()[i - 1][inputIndex])) {
                            assertEquals("The weight should be dropped.", 0,
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        } else {
                            assertEquals("Dropped some weight that should not be.",
                                    layer.getData()[inputIndex][neuralIndex],
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        }
                    }
                }
            } else {
                // if this is the last layer
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize(); inputIndex++) {
                        if (i > 0 && inputIndex < layer.rowSize() - 1
                                && this.trainingDropoutAnnModel.getDropoutMasks()[i - 1][inputIndex]) {
                            assertEquals("The weight should be dropped.", 0,
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        } else {
                            assertEquals("Dropped some weight that should not be.",
                                    layer.getData()[inputIndex][neuralIndex],
                                    dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testGetLayer_Running_Accuracy() {
        for (int i = 0; i < this.defaultAnnModel.getLayerCount(); i++) {
            Matrix layer = this.defaultAnnModel.getLayer(i);
            Matrix dropoutLayer = this.runningDropoutAnnModel.getLayer(i);

            if (i == 0) {
                // the first layer should have identical weights
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize(); inputIndex++) {
                        assertEquals("The first layer should have identical weights",
                                layer.getData()[inputIndex][neuralIndex],
                                dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                    }
                }
            } else {
                // other layers should have weights that has been halved
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize() - 1; inputIndex++) {
                        assertEquals("Weight " + i + ", " + inputIndex + ", " + neuralIndex + " should be havled.",
                                layer.getData()[inputIndex][neuralIndex] / 2,
                                dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                    }
                    assertEquals("Weight for baises should not be changed.",
                            layer.getData()[layer.rowSize() - 1][neuralIndex],
                            dropoutLayer.getData()[layer.rowSize() - 1][neuralIndex], DOUBLE_COMPARE_DELTA);
                }
            }
        }
    }

    @Test
    public void testGetLayer_Running_Reuse_Accuracy() {
        for (int i = 0; i < this.trainingDropoutAnnModel.getLayerCount(); i++) {
            this.runningDropoutAnnModel.getLayer(i);
        }
        this.runningDropoutAnnModel = new DropoutAnnModel(this.defaultAnnModel, false, this.runningDropoutAnnModel);
        for (int i = 0; i < this.defaultAnnModel.getLayerCount(); i++) {
            Matrix layer = this.defaultAnnModel.getLayer(i);
            Matrix dropoutLayer = this.runningDropoutAnnModel.getLayer(i);

            if (i == 0) {
                // the first layer should have identical weights
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize(); inputIndex++) {
                        assertEquals("The first layer should have identical weights",
                                layer.getData()[inputIndex][neuralIndex],
                                dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                    }
                }
            } else {
                // other layers should have weights that has been halved
                for (int neuralIndex = 0; neuralIndex < layer.columnSize(); neuralIndex++) {
                    for (int inputIndex = 0; inputIndex < layer.rowSize() - 1; inputIndex++) {
                        assertEquals("Weight " + i + ", " + inputIndex + ", " + neuralIndex + " should be havled.",
                                layer.getData()[inputIndex][neuralIndex] / 2,
                                dropoutLayer.getData()[inputIndex][neuralIndex], DOUBLE_COMPARE_DELTA);
                    }
                    assertEquals("Weight for baises should not be changed.",
                            layer.getData()[layer.rowSize() - 1][neuralIndex],
                            dropoutLayer.getData()[layer.rowSize() - 1][neuralIndex], DOUBLE_COMPARE_DELTA);
                }
            }
        }
    }
}
