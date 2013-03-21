package yatan.distributedcomputer.contract;

import yatan.distributed.akka.ActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;

public interface ParameterActorContract extends ActorContract {
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end);

    public void updateGradient(Parameter gradient);

    public interface ParameterActorReceiver {
        public void receiveParameter(Parameter parameter);
    }
}
