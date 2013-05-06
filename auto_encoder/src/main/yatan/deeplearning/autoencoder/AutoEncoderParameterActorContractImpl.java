package yatan.deeplearning.autoencoder;

import yatan.ann.AnnModel;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class AutoEncoderParameterActorContractImpl extends BaseActorContract implements ParameterActorContract {
    private AnnModel annModel;

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {

    }

    @Override
    public void updateGradient(Parameter gradient) {

    }
}
