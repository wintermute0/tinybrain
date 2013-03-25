package yatan.commons.ml;

public interface SingleFunction<X, Y> {
    public Y compute(X x);

    public Y derivative(X x);

    public int degree();
}
