package yatan.distributedcomputer.actors.data;

import java.util.List;

import com.google.inject.Inject;

import yatan.distributed.akka.BaseActor;
import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributed.akka.message.ActorResponseInvokeMessage;
import yatan.distributed.akka.message.BaseMessage;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.DataActorContract;

public class DataActor extends BaseActor<DataActorContract> {
    @Inject
    public DataActor(DataActorContract impl) {
        super(impl);
    }

    public static class RequestDataMessage extends ActorInvokeMessage {
        public RequestDataMessage(int size) {
            setCommand("requestData");
            setArguments(size);
        }
    }

    public static class ReceiveDataMessage extends ActorResponseInvokeMessage {
        public ReceiveDataMessage(BaseMessage originalMessage, List<Data> data) {
            super(originalMessage);

            setCommand("receiveData");
            setArguments(data);
        }
    }
}
