package yatan.ann;

import yatan.ann.AnnModel.Configuration.ActivationFunction;
import yatan.commons.ml.SingleFunction;

final public class AnnActivationFunctions {
    private AnnActivationFunctions() {
    }

    public static SingleFunction<Double, Double> activationFunction(ActivationFunction function) {
        switch (function) {
        case SIGMOID:
            return sigmoid();
        case TANH:
            return tanh();
        case Y_EQUALS_X:
            return yEqualsX();
        default:
            return null;
        }
    }

    public static SingleFunction<double[], Double> multiInputActivationFunction(ActivationFunction function) {
        switch (function) {
        case SOFTMAX:
            return softmax();
        default:
            return null;
        }
    }

    public static SingleFunction<Double, Double> sigmoid() {
        return new SingleFunction<Double, Double>() {
            @Override
            public Double compute(Double x) {
                return 1 / (1 + Math.exp(-x));
            }

            @Override
            public Double derivative(Double x) {
                // double ex = Math.exp(x);
                // return ex / Math.pow((1 + ex), 2);
                double fx = compute(x);
                return fx * (1 - fx);
            }

            @Override
            public int degree() {
                return 1;
            }
        };
    }

    public static SingleFunction<Double, Double> tanh() {
        return new SingleFunction<Double, Double>() {
            @Override
            public Double compute(Double x) {
                if (x > 100) {
                    return 1.0;
                } else if (x < -100) {
                    return -1.0;
                }

                double e2x = Math.exp(2 * x);
                /*
                 * if (Double.isInfinite(e2x)) { if (x > 1) { return 1.0; } else if (x < -1) { return -1.0; } else {
                 * return x; } }
                 */
                return (e2x - 1) / (e2x + 1);
            }

            @Override
            public Double derivative(Double x) {
                double tanh = compute(x);
                return 1 - tanh * tanh;
            }

            @Override
            public int degree() {
                return 1;
            }
        };
    }

    public static SingleFunction<Double, Double> yEqualsX() {
        return new SingleFunction<Double, Double>() {
            @Override
            public Double compute(Double x) {
                return x;
            }

            @Override
            public Double derivative(Double x) {
                return 1.0;
            }

            @Override
            public int degree() {
                return 1;
            }
        };
    }

    /**
     * <p>
     * Softmax(i, x) = e^xi / sum(e^xj)
     * </p>
     * <p>
     * The first element of the input vector should be i, and the remaining is vector x.
     * </p>
     * @return
     */
    public static SingleFunction<double[], Double> softmax() {
        return new SingleFunction<double[], Double>() {
            @Override
            public Double compute(double[] x) {
                double numerator = Math.exp(x[(int) (x[0]) + 1]);
                double denominator = 0;
                for (int i = 1; i < x.length; i++) {
                    denominator += Math.exp(x[i]);
                }

                return numerator / denominator;
            }

            @Override
            public Double derivative(double[] x) {
                throw new UnsupportedOperationException("You shouldn't be calculating the derivative of softmax.");
            }

            @Override
            public int degree() {
                throw new UnsupportedOperationException("Softmax function can be any degree.");
            }
        };
    }
}
