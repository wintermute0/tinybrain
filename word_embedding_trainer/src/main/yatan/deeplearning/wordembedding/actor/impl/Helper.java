package yatan.deeplearning.wordembedding.actor.impl;

import yatan.ann.AnnData;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

public class Helper {
    private Helper() {
    }

    public static AnnData convertToSoftmaxAnnData(WordEmbedding wordEmbedding, WordEmbeddingTrainingInstance instance) {
        double[] annInput = wordEmbedding.lookup(instance.getInput());
        double[] annOutput = instance.getOutput() > 0 ? new double[] {1, 0} : new double[] {0, 1};
        return new AnnData(annInput, annOutput);
    }
}
