package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.data.EnWikiTrainingDataProducer;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;

public class ComputeActorWordEmbeddingTrainingImplUnitTests {
    private ComputeActorWordEmbeddingTrainingImpl instance;

    @Before
    public void setUp() {
        this.instance = new ComputeActorWordEmbeddingTrainingImpl();
    }

    @After
    public void tearDown() {
        this.instance = null;
    }

    @Test
    public void testDoCompute() throws Exception {
//        WordEmbedding wordEmbedding = new WordEmbedding(Dictionary.words(), 50);
//
//        Configuration configuration =
//                new Configuration(wordEmbedding.getWordVectorSize() * EnWikiTrainingDataProducer.WINDOWS_SIZE);
//        configuration.addLayer(100, ActivationFunction.TANH);
//        configuration.addLayer(1, ActivationFunction.Y_EQUALS_X, false);
//        DefaultAnnModel annModel = new DefaultAnnModel(configuration);
//
//        Parameter parameter = new Parameter();
//        parameter.setSerializable(new Serializable[] {wordEmbedding, annModel});
//
//        while (true) {
//            List<Data> dataset = new ArrayList<Data>();
//            for (WordEmbeddingTrainingInstance instance : new EnWikiTrainingDataProducer().produceInstances(1000)) {
//                Data data = new Data();
//                data.setSerializable(instance);
//                dataset.add(data);
//            }
//            this.instance.doCompute(dataset, parameter);
//            System.out.print("The: ");
//            for (double v : wordEmbedding.getMatrix().getData()[wordEmbedding.getDictionary().indexOf("the")]) {
//                System.out.print(v + ", ");
//            }
//            System.out.println("Last layer model: ");
//            System.out.println(annModel.getLayer(annModel.getLayerCount() - 1));
//            if (Double.isNaN(annModel.getLayer(annModel.getLayerCount() - 1).getData()[0][0])) {
//                System.out.println("Model layer 0: " + annModel.getLayer(0));
//            }
//        }
    }
}
