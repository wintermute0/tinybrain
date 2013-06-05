package yatan.deeplearning.autoencoder.data.chinesewordembedding;

import java.util.Collections;
import java.util.List;

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
    @Named("frequency_rank_bound")
    private int frequencyRankBound = -1;

    @Inject
    private Dictionary dictionary;

    @Inject
    public BakeOffDataProducer(@Named("training") boolean training, WordSegmentationInstancePool instancePool) {
        super(training, instancePool);
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        // add negative instances
        List<Data> dataset = Lists.newArrayList();
        while (dataset.size() < size) {
            for (Data data : super.produceData(size / 2)) {
                WordEmbeddingTrainingInstance positiveInstance = (WordEmbeddingTrainingInstance) data.getSerializable();
                // validate positive instance
                boolean invalidWindow = false;
                for (int index : positiveInstance.getInput()) {
                    invalidWindow = isWordInvalid(index);
                    if (invalidWindow) {
                        break;
                    }
                }

                if (invalidWindow) {
                    continue;
                }

                positiveInstance.setOutput(1);
                dataset.add(data);

                // generate negative instance
                // WordEmbeddingTrainingInstance negativeInstance = new WordEmbeddingTrainingInstance();
                // negativeInstance.setInput(Lists.newArrayList(positiveInstance.getInput()));
                // negativeInstance.setOutput(-1);
                // // generate negative word
                // int negativeWord =
                // this.dictionary
                // .sampleWordUniformlyAboveFrequenceRank(ZhWikiTrainingDataProducer.FREQUENCEY_RANK_BOUND);
                // while (negativeWord == negativeInstance.getInput().get(negativeInstance.getInput().size() / 2)) {
                // negativeWord =
                // this.dictionary
                // .sampleWordUniformlyAboveFrequenceRank(ZhWikiTrainingDataProducer.FREQUENCEY_RANK_BOUND);
                // }
                // // set negative word
                // negativeInstance.getInput().set(negativeInstance.getInput().size() / 2, negativeWord);
                // dataset.add(new Data(negativeInstance));
            }
        }

        Collections.shuffle(dataset);

        return dataset;
    }

    private boolean isWordInvalid(int wordIndex) {
        return wordIndex == this.dictionary.indexOf(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)
                || (this.frequencyRankBound > 0 && this.dictionary.frenquencyRank(wordIndex) > this.frequencyRankBound);
    }
}
