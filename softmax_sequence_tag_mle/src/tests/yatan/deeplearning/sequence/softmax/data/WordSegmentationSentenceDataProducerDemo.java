package yatan.deeplearning.sequence.softmax.data;

import java.util.List;

import yatan.deeplearning.sequence.softmax.Trainer.CommonModule;
import yatan.deeplearning.sequence.softmax.Trainer.TrainingModule;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class WordSegmentationSentenceDataProducerDemo {
    public static void main(String[] args) throws Exception {
        final Injector commonModuleInjector = Guice.createInjector(new CommonModule());
        final Injector trainingModuleInjector = commonModuleInjector.createChildInjector(new TrainingModule());

        Dictionary dictionary = trainingModuleInjector.getInstance(Dictionary.class);

        WordSegmentationSentenceDataProducer producer =
                trainingModuleInjector.getInstance(WordSegmentationSentenceDataProducer.class);
        for (Data data : producer.produceData(1000)) {
            @SuppressWarnings("unchecked")
            List<WordEmbeddingTrainingInstance> instances =
                    (List<WordEmbeddingTrainingInstance>) data.getSerializable();
            for (WordEmbeddingTrainingInstance instance : instances) {
                System.out.print(dictionary.words().get(instance.getInput().get(instance.getInput().size() / 2)));
            }
            System.out.println();
        }
    }
}
