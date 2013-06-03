package yatan.deeplearning.softmax.contract.compute;

import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.DropoutAnnModel;
import yatan.deeplearning.softmax.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class SoftmaxClassificationTrainingContractImpl extends AbstractComputeActorContractImpl {
    private static final int MINIBATCH_SIZE = 20;

    @Inject(optional = false)
    private TrainerConfiguration trainerConfiguration;

    @Override
    protected int requestDataSize() {
        return MINIBATCH_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        AnnModel originalAnnModel = (DefaultAnnModel) parameters[1];

        // AnnGradient totalGradient = null;
        AnnGradient batchGradient = null;
        // HashMap<Integer, Double[]> totalWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        Multiset<Integer> wordAppearCount = HashMultiset.create();

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[originalAnnModel.getLayerCount()][];
        // int batchCount = 0;
        // reuse the matrices in the gradient
        AnnGradient newGradient = null;
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            // batchCount++;

            // use dropout ann model
            AnnModel annModel = originalAnnModel;
            if (this.trainerConfiguration.dropout) {
                annModel = new DropoutAnnModel(originalAnnModel, true);
            }

            // first convert input data into word embedding
            // FIXME: could reuse an array, no need to allocate it every time
            double[] annInput = wordEmbedding.lookup(instance.getInput());
            // covert output data into an binary array
            double[] annOutput = new double[annModel.getLayer(annModel.getLayerCount() - 1).columnSize()];
            annOutput[instance.getOutput()] = 1;
            AnnData annData = new AnnData(annInput, annOutput);

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), sum);
            newGradient =
                    trainer.backpropagateSoftmaxLogLikelyhood(annModel, annData, output, sum,
                            this.trainerConfiguration.l2Lambdas, newGradient);

            // save gradient
            batchGradient = saveGradient(batchGradient, newGradient);

            // save wordEmbeddingDelta
            saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(), annInput,
                    instance);
            // update word appear count
            for (Integer word : instance.getInput()) {
                wordAppearCount.add(word);
            }
        }

        // average batch gradient
        batchGradient.averageBy(MINIBATCH_SIZE);

        // average word embedding gradient
        for (Entry<Integer, Double[]> entry : batchWordEmbeddingDelta.entrySet()) {
            Double[] gradient = entry.getValue();
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] /= wordAppearCount.count(entry.getKey());
            }
        }

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(new Serializable[] {batchGradient, batchWordEmbeddingDelta});
        result.setGradient(gradientWrapper);

        return result;
    }

    private static AnnGradient saveGradient(AnnGradient gradient, AnnGradient newGradient) {
        if (gradient == null) {
            return newGradient.clone();
        } else {
            gradient.updateByPlus(newGradient);
            return gradient;
        }
    }

    private static void saveWordEmbeddingDelta(AnnGradient newGradient, HashMap<Integer, Double[]> wordEmbeddingDelta,
            int wordVectorSize, double[] input, WordEmbeddingTrainingInstance instance) {
        for (int i = 0; i < instance.getInput().size(); i++) {
            int index = instance.getInput().get(i);
            Double[] delta = wordEmbeddingDelta.get(index);
            if (delta == null) {
                delta = new Double[wordVectorSize];
                wordEmbeddingDelta.put(index, delta);
                for (int j = 0; j < wordVectorSize; j++) {
                    delta[j] = newGradient.getDeltaForInputLayer()[i * wordVectorSize + j];
                }
            } else {
                for (int j = 0; j < wordVectorSize; j++) {
                    delta[j] += newGradient.getDeltaForInputLayer()[i * wordVectorSize + j];
                }
            }
        }
    }
}
