package yatan.deeplearning.softmax.contract.parameter;

import yatan.distributedcomputer.Parameter;

public interface ParameterFactory extends StatefulProcessor {
    public Parameter initializeParameter();

    public Parameter newEmptyParameter();

    public void cloneParameter(Parameter snapshot, int sliceId, int totalSlice);
}
