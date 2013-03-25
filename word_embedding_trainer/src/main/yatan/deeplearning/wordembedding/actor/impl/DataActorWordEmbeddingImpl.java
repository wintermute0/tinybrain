package yatan.deeplearning.wordembedding.actor.impl;

import java.util.ArrayList;
import java.util.List;

import yatan.deeplearning.wordembedding.data.EnWikiTrainingDataProducer;
import yatan.deeplearning.wordembedding.data.TrainingDataProducerException;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.actors.data.DataActor;
import yatan.distributedcomputer.contract.data.DataActorContract;

import yatan.distributed.akka.BaseActorContract;

@Deprecated
public class DataActorWordEmbeddingImpl extends BaseActorContract implements DataActorContract {
    private EnWikiTrainingDataProducer trainingDataProducer = new EnWikiTrainingDataProducer();

    @Override
    public void requestData(int size) {
        List<Data> dataset = new ArrayList<Data>();

        boolean success = false;
        while (!success) {
            try {
                for (WordEmbeddingTrainingInstance instance : this.trainingDataProducer.produceInstances(size)) {
                    Data data = new Data();
                    data.setSerializable(instance);
                    dataset.add(data);
                }
                success = true;
            } catch (TrainingDataProducerException e) {
                getLogger().warn("Error occurred while retrieving training data. Wait 5 seconds and try again.", e);
                success = false;
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e1) {
                    // should never happen
                    e1.printStackTrace();
                }
            }
        }

        tellSender(new DataActor.ReceiveDataMessage(getActor().getMessage(), dataset));
    }

    @Override
    public void receiveDataFromFactory(List<Data> dataset) {
        // TODO Auto-generated method stub

    }
}
