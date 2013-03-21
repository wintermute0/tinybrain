package yatan.distributedcomputer.contract;

import yatan.distributed.akka.ActorContract;
import yatan.distributedcomputer.ComputeInstruction;
import yatan.distributedcomputer.contract.ParameterActorContract.ParameterActorReceiver;
import yatan.distributedcomputer.contract.data.DataActorContract.DataActorReceiver;

public interface ComputeActorContract extends ActorContract, ParameterActorReceiver, DataActorReceiver {
    public void compute(ComputeInstruction computeInstruction);

    public void abort();
}
