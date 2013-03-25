package yatan.common;

public class CheckUtility {
    public static void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException("The '" + name + "' cannot be null.");
        }
    }

    public static void isPositive(int num, String name) {
        if (num <= 0) {
            throw new IllegalArgumentException("The '" + name + "' should be positive.");
        }
    }

    public static void checkState(Object obj, String name) {
        if (obj == null) {
            throw new IllegalStateException("The '" + name + "' cannot be null.");
        }
    }
}
