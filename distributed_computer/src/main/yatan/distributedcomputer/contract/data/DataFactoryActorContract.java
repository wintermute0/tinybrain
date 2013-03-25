package yatan.distributedcomputer.contract.data;

import java.util.List;

import com.google.inject.ImplementedBy;

import yatan.distributed.akka.ActorContract;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;
import yatan.distributedcomputer.contract.data.impl.DefaultDataFactoryActorContract;

@ImplementedBy(DefaultDataFactoryActorContract.class)
public interface DataFactoryActorContract extends ActorContract {
    public void produce(int size) throws DataProducerException;

    public static interface DataFactoryActorReceiver {
        public void receiveDataFromFactory(List<Data> dataset);
    }
}