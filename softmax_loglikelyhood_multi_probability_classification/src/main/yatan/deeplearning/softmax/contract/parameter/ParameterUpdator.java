package yatan.deeplearning.softmax.contract.parameter;

import yatan.distributedcomputer.Parameter;

public interface ParameterUpdator extends StatefulProcessor {
    public void update(Parameter parameter, Parameter gradient, int sliceId, int totalSlice);
}
