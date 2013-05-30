package yatan.deeplearning.softmax.contract.parameter;

import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class WordEmbeddingAnnParameterActorContractImpl2 extends BaseActorContract implements ParameterActorContract {

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateGradient(Parameter gradient) {
        // TODO Auto-generated method stub

    }

}
