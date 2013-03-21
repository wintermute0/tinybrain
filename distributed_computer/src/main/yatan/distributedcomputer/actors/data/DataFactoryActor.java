package yatan.distributedcomputer.actors.data;

import java.util.List;

import com.google.inject.Inject;

import yatan.distributed.akka.BaseActor;
import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributed.akka.message.ActorResponseInvokeMessage;
import yatan.distributed.akka.message.BaseMessage;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.DataFactoryActorContract;

public class DataFactoryActor extends BaseActor<DataFactoryActorContract> {
    @Inject
    public DataFactoryActor(DataFactoryActorContract actorImpl) {
        super(actorImpl);
    }

    public static class ProduceMessage extends ActorInvokeMessage {
        public ProduceMessage(int size) {
            setCommand("produce");
            setArguments(size);
        }
    }

    public static class ReceiveDataFromFactoryMessage extends ActorResponseInvokeMessage {
        public ReceiveDataFromFactoryMessage(BaseMessage originalMessage, List<Data> data) {
            super(originalMessage);

            setCommand("receiveDataFromFactory");
            setArguments(data);
        }
    }
}