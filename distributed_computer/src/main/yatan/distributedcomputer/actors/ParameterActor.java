package yatan.distributedcomputer.actors;

import com.google.inject.Inject;

import yatan.distributed.akka.BaseActor;

import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributed.akka.message.ActorResponseInvokeMessage;
import yatan.distributed.akka.message.BaseMessage;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class ParameterActor extends BaseActor<ParameterActorContract> {
    @Inject
    public ParameterActor(ParameterActorContract impl) {
        super(impl);
    }

    public static class RequestParameterMessage extends ActorInvokeMessage {
        public RequestParameterMessage(ParameterIndexPath start, ParameterIndexPath end) {
            setCommand("requestParameters");
            setArguments(start, end);
        }
    }

    public static class UpdateGradientMessage extends ActorInvokeMessage {
        public UpdateGradientMessage(Parameter gradient) {
            setCommand("updateGradient");
            setArguments(gradient);
        }
    }

    public static class ReceiveParameterMessage extends ActorResponseInvokeMessage {
        public ReceiveParameterMessage(BaseMessage originalMessage, Parameter parameter) {
            super(originalMessage);

            setCommand("receiveParameter");
            setArguments(parameter);
        }
    }
}
