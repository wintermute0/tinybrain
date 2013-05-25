package yatan.deeplearning.sequence.softmax.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;

public class WordSegmentationSentenceDataProducer extends WordSegmentationDataProducer {
    private Random random = new Random(new Date().getTime());

    @Inject
    public WordSegmentationSentenceDataProducer(@Named("training") boolean training,
            WordSegmentationInstancePool instancePool) {
        super(training, instancePool);

        WINDOWS_SIZE = 5;
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        // add negative instances
        List<List<WordEmbeddingTrainingInstance>> sentences = getInstancePool().getSentences();

        List<Data> dataset = Lists.newArrayList();
        while (dataset.size() < size) {
            Data data = new Data();
            data.setSerializable((Serializable) sentences.get(this.random.nextInt(sentences.size())));
            dataset.add(data);
        }

        return dataset;
    }
}
