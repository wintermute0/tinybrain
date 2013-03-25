package yatan.commons.matrix;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MatrixUnitTests extends Assert {
    private static final double EQUAL_DELTA = 0.0000001;
    private Matrix instance;

    @Before
    public void setUp() {
        this.instance = null;
    }

    @After
    public void tearDown() {
        this.instance = null;
    }

    @Test
    public void testTranposeAccuracy() {
        this.instance = new Matrix(new double[][] { {1, 2, 3}, {4, 5, 6}});
        Matrix result = this.instance.transpose();
        assertArrayEquals("Row 1 should match.", new double[] {1, 4}, result.getData()[0], EQUAL_DELTA);
        assertArrayEquals("Row 2 should match.", new double[] {2, 5}, result.getData()[1], EQUAL_DELTA);
        assertArrayEquals("Row 3 should match.", new double[] {3, 6}, result.getData()[2], EQUAL_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultiplyByFailure_WrongVectorSize() {
        this.instance = new Matrix(new double[][] { {1, 2, 3}, {4, 5, 6}});
        double[] vector = {7, 8, 9};
        this.instance.multiplyBy(vector);
    }

    @Test
    public void testMultiplyByAccuracy() {
        this.instance = new Matrix(new double[][] { {1, 2, 3}, {4, 5, 6}});
        double[] vector = {7, 8};
        double[] result = this.instance.multiplyBy(vector);
        assertArrayEquals("Result should match.", new double[] {39, 54, 69}, result, EQUAL_DELTA);
    }

    @Test
    public void testMultiplyAccuracy() {
        this.instance = new Matrix(new double[][] { {1, 2, 3}, {4, 5, 6}});
        double[] vector = {7, 8, 9};
        double[] result = this.instance.multiply(vector);
        assertArrayEquals("Result should match.", new double[] {50, 122}, result, EQUAL_DELTA);
    }
}
