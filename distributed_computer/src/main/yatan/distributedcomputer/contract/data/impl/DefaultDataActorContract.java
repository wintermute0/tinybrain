package yatan.distributedcomputer.contract.data.impl;

import java.util.List;

import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.dispatch.Future;
import akka.dispatch.OnFailure;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;

import yatan.distributed.akka.BaseActorContract;
import yatan.distributed.akka.message.BaseMessage;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.actors.data.DataActor.ReceiveDataMessage;
import yatan.distributedcomputer.actors.data.DataFactoryActor;
import yatan.distributedcomputer.actors.data.DataFactoryActor.ProduceMessage;
import yatan.distributedcomputer.contract.data.DataActorContract;

public class DefaultDataActorContract extends BaseActorContract implements DataActorContract {
    private static final int RETRY_COMPUTE_DELAY_SECONDS = 10;
    private static final Timeout REQUEST_TIMEOUT = new Timeout(Duration.create(60 * 1, TimeUnit.SECONDS));

    private final Provider<DataFactoryActor> dataFactoryActorProvider;

    @Inject
    @Named("data_produce_batch_size")
    private int dataProduceBatchSize = 200000;

    private ActorRef dataFactoryActor;

    private List<Data> dataset = Lists.newArrayList();
    private List<DataRequest> queuedRequests = Lists.newArrayList();

    private Status status = Status.READY;

    @Inject
    public DefaultDataActorContract(Provider<DataFactoryActor> dataFactoryActorProvider) {
        this.dataFactoryActorProvider = dataFactoryActorProvider;
    }

    @Override
    public void preStart() {
        this.dataFactoryActor = getActor().context().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = -4688280963565232966L;

            @Override
            public Actor create() throws Exception {
                return dataFactoryActorProvider.get();
            }
        }), "data_factory");
    }

    @Override
    public void requestData(final int size) {
        getLogger().debug("Receive data request from " + getActor().getSender() + ", size = " + size);
        // queue the request
        this.queuedRequests.add(new DataRequest(getActor().getMessage(), getActor().getSender(), size));
        // try to executed the queue
        executeQueuedRequest();
    }

    @Override
    public void receiveDataFromFactory(List<Data> dataset) {
        getLogger().debug("Receive " + dataset.size() + " data from factory.");
        // clear future
        status = Status.READY;

        // save dataset
        this.dataset.addAll(dataset);

        // try to execute queued requests
        executeQueuedRequest();
    }

    public int getDataProduceBatchSize() {
        return dataProduceBatchSize;
    }

    @Inject(optional = true)
    public void setDataProduceBatchSize(int dataProduceBatchSize) {
        this.dataProduceBatchSize = dataProduceBatchSize;
    }

    private void executeQueuedRequest() {
        for (DataRequest dataRequest : Lists.newArrayList(this.queuedRequests)) {
            if (this.dataset.size() >= dataRequest.size) {
                // if we already have enough data, remove the request from queue and give sender data
                getLogger().debug("Sending " + dataRequest.size + " data to " + dataRequest.sender);
                dataRequest.sender.tell(new ReceiveDataMessage(dataRequest.message, Lists.newArrayList(this.dataset
                        .subList(0, dataRequest.size))));
                this.dataset = Lists.newArrayList(this.dataset.subList(dataRequest.size, this.dataset.size()));
                this.queuedRequests.remove(0);
            } else {
                // not enough data, stop executing the queue
                break;
            }
        }

        // if we're not already requesting data
        if (status == Status.READY) {
            // if there is still queued request or the cached data is under limit
            if (!this.queuedRequests.isEmpty()) {
                getLogger().debug("Ask for new data from data factory.");
                status = Status.WAITING_FOR_FACTORY_TO_PRODUCE_DATA;
                Future<Object> produceDataFuture =
                        Patterns.ask(this.dataFactoryActor, new ProduceMessage(dataProduceBatchSize), REQUEST_TIMEOUT);
                produceDataFuture.onFailure(new OnFailure() {
                    @Override
                    public void onFailure(Throwable e) throws Throwable {
                        // if parameter request failed, wait 10 seconds and try again
                        getLogger().warn(
                                "Failed to produce more data from factory. Try to ask the factory again in "
                                        + RETRY_COMPUTE_DELAY_SECONDS + " seconds.", e);
                        getActor()
                                .getContext()
                                .system()
                                .scheduler()
                                .scheduleOnce(Duration.create(RETRY_COMPUTE_DELAY_SECONDS, TimeUnit.SECONDS),
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                status = Status.READY;
                                                getLogger().info("Try to execute queued request again.");
                                                executeQueuedRequest();
                                            }
                                        });
                    }
                });
                Patterns.pipe(produceDataFuture).to(getActor().getSelf());
            }
        }
    }

    private static class DataRequest {
        BaseMessage message;
        ActorRef sender;
        int size;

        public DataRequest(BaseMessage message, ActorRef sender, int size) {
            this.message = message;
            this.sender = sender;
            this.size = size;
        }
    }

    private static enum Status {
        READY, WAITING_FOR_FACTORY_TO_PRODUCE_DATA
    }
}
