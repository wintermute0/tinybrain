package yatan.distributedcomputer.contract.data;

import java.util.List;

import com.google.inject.ImplementedBy;

import yatan.distributed.akka.ActorContract;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.DataFactoryActorContract.DataFactoryActorReceiver;
import yatan.distributedcomputer.contract.data.impl.DefaultDataActorContract;

@ImplementedBy(DefaultDataActorContract.class)
public interface DataActorContract extends ActorContract, DataFactoryActorReceiver {
    public void requestData(int size);

    public interface DataActorReceiver {
        public void receiveData(List<Data> data);
    }
}
