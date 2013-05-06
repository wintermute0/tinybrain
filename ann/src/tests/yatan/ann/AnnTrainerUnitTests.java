package yatan.ann;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.commons.matrix.Matrix;
import yatan.commons.matrix.MatrixTestsHelper;

public class AnnTrainerUnitTests extends Assert {
    private AnnTrainer instance;
    private DefaultAnnModel model;
    private double[] input;
    private AnnData data;

    @Before
    public void setUp() {
        this.instance = new AnnTrainer();

        this.model =
                new DefaultAnnModel(new AnnConfiguration(2).addLayer(3, ActivationFunction.SIGMOID)
                        .addLayer(2, ActivationFunction.SIGMOID).addLayer(1, ActivationFunction.SIGMOID));
        this.model.getLayer(0).setData(new double[][] { {0.1, 0.3, 0.5}, {0.2, 0.4, 0.6}, {0, 0, 0}});
        this.model.getLayer(1).setData(new double[][] { {0.7, 0.8}, {0.9, -0.1}, {-0.2, -0.3}, {0, 0}});
        this.model.getLayer(2).setData(new double[][] { {-0.4}, {-0.5}, {0}});

        this.input = new double[] {1.0, 2.0};
        this.data = new AnnData(new double[] {1.0, 2.0}, new double[] {1.0});
    }

    @After
    public void tearDown() {
        this.instance = null;
    }

    @Test
    public void testRunAccuracy() {
        double[][] sum = new double[this.model.getLayerCount()][];
        double[][] result = this.instance.run(this.model, this.input, sum);
        MatrixTestsHelper.assertTwoDimensionalDoubleArrayMatch("ANN run result should match.", new double[][] {
                {0.6224593312, 0.75026010559, 0.84553473491}, {0.71950347101, 0.54214972785}, {0.36377887198}}, result,
                0.001);

        for (int i = 0; i < sum.length; i++) {
            for (int j = 0; j < sum[i].length; j++) {
                System.out.print(sum[i][j] + ", ");
            }
            System.out.println();
        }
    }

    @Test
    public void testBackpropagation() {
        double[][] sum = new double[this.model.getLayerCount()][];
        double[][] output = this.instance.run(this.model, this.input, sum);
        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output[i].length; j++) {
                System.out.print(output[i][j] + " ");
            }
            System.out.println();
        }

        System.out.println("\nGridents: ");

        AnnGradient gradient = this.instance.backpropagateLeastSqure(this.model, this.data, output, sum);
        gradient.getGradients();
        for (Matrix matrix : gradient.getGradients()) {
            System.out.println(matrix);
        }
    }

    @Test
    public void testBackpropagation2() {
        this.model =
                new DefaultAnnModel(new AnnConfiguration(2).addLayer(2, ActivationFunction.SIGMOID).addLayer(1,
                        ActivationFunction.SIGMOID));
        this.model.getLayer(0).setData(new double[][] { {2, 1}, {-2, 3}, {0, -1}});
        this.model.getLayer(1).setData(new double[][] { {3}, {-2}, {-1}});

        double[][] output = this.instance.run(this.model, new double[] {1, 0}, new double[2][]);
        MatrixTestsHelper.assertTwoDimensionalDoubleArrayMatch("First output should match.", new double[][] {
                {0.881, 0.5}, {0.655}}, output, 0.001);
    }
}
