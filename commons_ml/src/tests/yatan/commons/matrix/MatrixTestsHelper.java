package yatan.commons.matrix;

import org.junit.Assert;

final public class MatrixTestsHelper {
    public static final double EQUAL_DELTA = 0.0000001;

    private MatrixTestsHelper() {
    }

    public static void assertTwoDimensionalDoubleArrayMatch(String message, double[][] expected, double[][] actual,
            double delta) {
        Assert.assertEquals(message + ": array row size should match.", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertArrayEquals(message + ": row " + i + " should match.", expected[i], actual[i], delta);
        }
    }
}
