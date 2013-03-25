package yatan.deeplearning.softmax.contract;

import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class SoftmaxClassificationTrainingContractImpl extends AbstractComputeActorContractImpl {
    public static final double LEARNING_RATE =
            WordEmbeddingAnnParameterActorContractImpl.ADAGRAD_LEARNING_RATE_LAMPDA / 10;
    private static final int MINIBATCH_SIZE = 10;

    @Override
    protected int requestDataSize() {
        return 100;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        AnnModel annModel = (AnnModel) parameters[1];

        AnnGradient totalGradient = null;
        AnnGradient batchGradient = null;
        HashMap<Integer, Double[]> totalWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        int batchCount = 0;
        // reuse the matrices in the gradient
        AnnGradient newGradient = null;
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            batchCount++;

            // first convert input data into word embedding
            // FIXME: could reuse an array, no need to allocate it every time
            double[] annInput = wordEmbedding.lookup(instance.getInput());
            // covert output data into an binary array
            double[] annOutput = new double[annModel.getLayer(annModel.getLayerCount() - 1).columnSize()];
            annOutput[instance.getOutput()] = 1;
            AnnData annData = new AnnData(annInput, annOutput);

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), sum);
            newGradient = trainer.backpropagateSoftmaxLogLikelyhood(annModel, annData, output, sum, newGradient);

            // save gradient
            batchGradient = saveGradient(batchGradient, newGradient);

            // save wordEmbeddingDelta
            saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(), annInput,
                    instance);

            // if we get to batch size, update model
            // FIXME: potential bug: what if requestDataSize % MINIBATCH_SIZE != 0?
            if (batchCount == MINIBATCH_SIZE) {
                // update ann model
                annModel.update(batchGradient, LEARNING_RATE);
                // update word embedding
                wordEmbedding.update(batchWordEmbeddingDelta, LEARNING_RATE);

                // save total gradient and wordEmbeddingDelta
                totalGradient = saveGradient(totalGradient, batchGradient);
                for (Entry<Integer, Double[]> batchEmbeddingDelta : batchWordEmbeddingDelta.entrySet()) {
                    Double[] delta = totalWordEmbeddingDelta.get(batchEmbeddingDelta.getKey());
                    if (delta == null) {
                        delta = new Double[wordEmbedding.getWordVectorSize()];
                        totalWordEmbeddingDelta.put(batchEmbeddingDelta.getKey(), delta);
                        for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
                            delta[i] = batchEmbeddingDelta.getValue()[i];
                        }
                    } else {
                        for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
                            delta[i] += batchEmbeddingDelta.getValue()[i];
                        }
                    }
                }

                // clear batch data
                batchCount = 0;

                batchGradient = null;
                batchWordEmbeddingDelta.clear();
            }
        }

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(new Serializable[] {totalGradient, totalWordEmbeddingDelta});
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
