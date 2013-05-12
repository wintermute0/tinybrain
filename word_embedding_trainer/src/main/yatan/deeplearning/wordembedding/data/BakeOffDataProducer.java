package yatan.deeplearning.wordembedding.data;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;

public class BakeOffDataProducer extends WordSegmentationDataProducer {
    @Inject
    private Dictionary dictionary;
    private Random random = new Random(new Date().getTime());

    @Inject
    public BakeOffDataProducer(@Named("training") boolean training, WordSegmentationInstancePool instancePool) {
        super(training, instancePool);

        WINDOWS_SIZE = 11;
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        // add negative instances
        List<Data> dataset = Lists.newArrayList();
        for (Data data : super.produceData(size / 2)) {
            WordEmbeddingTrainingInstance positiveInstance = (WordEmbeddingTrainingInstance) data.getSerializable();
            positiveInstance.setOutput(1);
            dataset.add(data);

            WordEmbeddingTrainingInstance negativeInstance = new WordEmbeddingTrainingInstance();
            negativeInstance.setInput(Lists.newArrayList(positiveInstance.getInput()));
            negativeInstance.setOutput(-1);
            // generate negative word
            int negativeWord = this.random.nextInt(this.dictionary.size());
            while (negativeWord == negativeInstance.getInput().get(negativeInstance.getInput().size() / 2)) {
                negativeWord = this.random.nextInt(this.dictionary.size());
            }
            // set negative word
            negativeInstance.getInput().set(negativeInstance.getInput().size() / 2, negativeWord);
            dataset.add(new Data(negativeInstance));
        }

        Collections.shuffle(dataset);

        return dataset;
    }
}
