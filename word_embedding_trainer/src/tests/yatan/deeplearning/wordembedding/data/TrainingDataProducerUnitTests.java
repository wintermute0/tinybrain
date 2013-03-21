package yatan.deeplearning.wordembedding.data;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

public class TrainingDataProducerUnitTests {
    private EnWikiTrainingDataProducer instance;

    @Before
    public void setUp() {
        this.instance = new EnWikiTrainingDataProducer();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testProduceInstances() throws Exception {
        for (WordEmbeddingTrainingInstance instance : this.instance.produceInstances(1000)) {
            System.out.print(instance.getOutput() + ": ");
            for (int index : instance.getInput()) {
                System.out.print(Dictionary.words().get(index) + " ");
            }
            System.out.println();
        }
    }
}
