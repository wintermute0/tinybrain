package yatan.deeplearning.softmax.contract;

import java.io.Serializable;

import java.util.HashMap;
import java.util.List;

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
    // public static final double NETWORK_LEARNING_RATE =
    // WordEmbeddingAnnParameterActorContractImpl.ADAGRAD_NETWORK_LEARNING_RATE_LAMPDA / 10;
    // public static final double WORD_EMBEDDING_LEARNING_RATE =
    // WordEmbeddingAnnParameterActorContractImpl.ADAGRAD_WORD_EMBEDDING_LEARNING_RATE_LAMPDA / 10;
    private static final int MINIBATCH_SIZE = 100;

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

            // if we get to batch size, update model
            // // FIXME: potential bug: what if requestDataSize % MINIBATCH_SIZE != 0?
            // if (batchCount == MINIBATCH_SIZE) {
            // // update ann model
            // originalAnnModel.update(batchGradient, NETWORK_LEARNING_RATE);
            // // update word embedding
            // wordEmbedding.update(batchWordEmbeddingDelta, WORD_EMBEDDING_LEARNING_RATE);
            //
            // // save total gradient and wordEmbeddingDelta
            // totalGradient = saveGradient(totalGradient, batchGradient);
            // for (Entry<Integer, Double[]> batchEmbeddingDelta : batchWordEmbeddingDelta.entrySet()) {
            // Double[] delta = totalWordEmbeddingDelta.get(batchEmbeddingDelta.getKey());
            // if (delta == null) {
            // delta = new Double[wordEmbedding.getWordVectorSize()];
            // totalWordEmbeddingDelta.put(batchEmbeddingDelta.getKey(), delta);
            // for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
            // delta[i] = batchEmbeddingDelta.getValue()[i];
            // }
            // } else {
            // for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
            // delta[i] += batchEmbeddingDelta.getValue()[i];
            // }
            // }
            // }
            //
            // // clear batch data
            // batchCount = 0;
            //
            // batchGradient = null;
            // batchWordEmbeddingDelta.clear();
            // }
        }

        // average batch gradient
        batchGradient.averageBy(MINIBATCH_SIZE);

        // average word embedding gradient
        for (Double[] gradient : batchWordEmbeddingDelta.values()) {
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] /= MINIBATCH_SIZE;
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
