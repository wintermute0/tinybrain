package yatan.deeplearning.wordembedding.data;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;

public class ZhWikiTrainingDataProducerUnitTests {
    private ZhWikiTrainingDataProducer producer;
    private Dictionary dictionary;

    @Before
    public void setUp() {
        this.dictionary = Dictionary.create(new File("test_files/zh_dict.txt"), 250);
        this.producer = new ZhWikiTrainingDataProducer(this.dictionary);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testProduceInstances() throws Exception {
        for (Data data : this.producer.produceData(1000)) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            System.out.print(instance.getOutput() + ": ");
            for (int index : instance.getInput()) {
                System.out.print(this.dictionary.words().get(index).equals(Dictionary.NO_SUCH_WORD_PLACE_HOLDER) ? "_ "
                        : this.dictionary.words().get(index) + " ");
            }
            System.out.println();
        }
    }
}
