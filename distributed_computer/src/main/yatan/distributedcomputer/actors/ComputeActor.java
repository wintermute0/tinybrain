package yatan.distributedcomputer.actors;

import com.google.inject.Inject;

import yatan.distributed.akka.BaseActor;

import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributedcomputer.ComputeInstruction;
import yatan.distributedcomputer.contract.ComputeActorContract;

public class ComputeActor extends BaseActor<ComputeActorContract> {
    @Inject
    public ComputeActor(ComputeActorContract impl) {
        super(impl);
    }

    public static class ComputeMessage extends ActorInvokeMessage {
        public ComputeMessage(ComputeInstruction computeInstruction) {
            setCommand("compute");
            setArguments(computeInstruction);
        }
    }

    public static class AbortMessage extends ActorInvokeMessage {
        public AbortMessage() {
            setCommand("abort");
        }
    }
}
