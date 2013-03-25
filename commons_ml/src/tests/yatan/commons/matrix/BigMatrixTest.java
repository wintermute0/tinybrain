package yatan.commons.matrix;

import java.util.Date;
import java.util.Random;

public class BigMatrixTest {
    public static void main(String[] args) {
        int n = 1000;
        double[][] data1 = new double[n][n];
        double[][] data2 = new double[n][n];

        fill(data1);
        fill(data2);

        System.out.println("Start.");
        long start = new Date().getTime();
        new Matrix(data1).multiply(new Matrix(data2));
        System.out.println(new Date().getTime() - start);
    }

    private static void fill(double[][] data) {
        Random random = new Random(new Date().getTime());
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = random.nextDouble();
            }
        }
    }
}
