package yatan.deeplearning.softmax.contract.parameter;

import yatan.distributedcomputer.Parameter;

public interface ParameterFactory extends StatefulProcessor {
    public Parameter initializeParameter();

    public Parameter cloneParameter();
}
