package yatan.distributedcomputer.contract.data.impl;

import java.util.List;

import com.google.inject.Inject;

import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.actors.data.DataFactoryActor;
import yatan.distributedcomputer.contract.data.DataFactoryActorContract;

public class DefaultDataFactoryActorContract extends BaseActorContract implements DataFactoryActorContract {
    private final DataProducer dataProducer;

    @Inject
    public DefaultDataFactoryActorContract(DataProducer dataProducer) {
        this.dataProducer = dataProducer;
    }

    @Override
    public void produce(int size) throws DataProducerException {
        getLogger().debug("Producing new data...");
        List<Data> dataset = this.dataProducer.produceData(size);
        getLogger().debug("Data production complete.");
        tellSender(new DataFactoryActor.ReceiveDataFromFactoryMessage(getActor().getMessage(), dataset));
    }
}
