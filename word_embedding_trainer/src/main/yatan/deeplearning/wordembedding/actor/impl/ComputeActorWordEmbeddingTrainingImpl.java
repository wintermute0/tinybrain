package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class ComputeActorWordEmbeddingTrainingImpl extends AbstractComputeActorContractImpl {
    private static final double LEARNING_RATE = ParameterActorWordEmbeddingImpl.ADAGRAD_LEARNING_RATE_LAMPDA / 10;
    private static final int MINIBATCH_SIZE = 10;

    @Override
    protected int requestDataSize() {
        return 100;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        DefaultAnnModel annModel = (DefaultAnnModel) parameters[1];

        AnnGradient totalGradient = null;
        AnnGradient batchGradient = null;
        HashMap<Integer, Double[]> totalWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        int batchCount = 0;
        // reuse this
        AnnGradient newGradient = null;
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            batchCount++;

            // first convert input data into word embedding
            // FIXME: could reuse an array, no need to allocate it every time
            double[] annInput = wordEmbedding.lookup(instance.getInput());
            AnnData annData = new AnnData(annInput, new double[] {instance.getOutput()});

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), sum);
            newGradient = wordEmbeddingBackpropagate(annModel, annData, output, sum, newGradient);

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

    // private AnnGradient wordEmbeddingBackpropagate(AnnModel model, AnnData data, double[][] output, double[][] sum,
    // AnnGradient annGradient) {
    // // this controls the direction of the gradient, it's either 1 or -1
    // double factor = (int) data.getOutput()[0];
    //
    // // compute delta U
    // Matrix deltaU =
    // annGradient == null ? new Matrix(model.getLayer(1).rowSize(), model.getLayer(1).columnSize())
    // : annGradient.getGradients().get(1);
    // for (int i = 0; i < model.getLayer(1).rowSize() - 1; i++) {
    // // the -2 * U is L2 regularazation - 2 *model.getLayer(1).getData()[i][0]
    // deltaU.getData()[i][0] = output[0][i] * factor;
    // }
    //
    // // compute delta W
    // double[] delta = new double[model.getLayer(1).rowSize()];
    // Matrix deltaW =
    // annGradient == null ? new Matrix(model.getLayer(0).rowSize(), model.getLayer(0).columnSize())
    // : annGradient.getGradients().get(0);
    // SingleFunction<Double, Double> activation =
    // AnnActivationFunctions.activationFunction(model.getConfiguration().activationFunctionOfLayer(0));
    // for (int i = 0; i < deltaW.columnSize(); i++) {
    // delta[i] = model.getLayer(1).getData()[i][0] * activation.derivative(sum[0][i]) * factor;
    // for (int j = 0; j < deltaW.rowSize() - 1; j++) {
    // // the -2 * W is L2 regularazation: - 2 * model.getLayer(0).getData()[j][i] * factor
    // deltaW.getData()[j][i] = delta[i] * data.getInput()[j];
    // }
    // // the -2 * b is L2 regularazation: - 2 * model.getLayer(0).getData()[deltaW.rowSize() - 1][i] * factor
    // deltaW.getData()[deltaW.rowSize() - 1][i] = delta[i];
    // }
    //
    // // compute delta X
    // double[] deltaX = model.getLayer(0).multiply(delta);
    //
    // // regularize x
    // // for (int i = 0; i < deltaX.length - 1; i++) { deltaX[i] -= 10 * data.getInput()[i] * (int) //
    // // data.getOutput()[0]; }
    //
    // // output
    // if (annGradient != null) {
    // annGradient.setDeltaForInputLayer(deltaX);
    // return annGradient;
    // } else {
    // List<Matrix> gradients = new ArrayList<Matrix>();
    // gradients.add(deltaW);
    // gradients.add(deltaU);
    // return new AnnGradient(gradients, deltaX);
    // }
    // }

    private AnnGradient wordEmbeddingBackpropagate(DefaultAnnModel model, AnnData data, double[][] output,
            double[][] sum, AnnGradient reuseGradient) {
        data.setOutput(data.getOutput()[0] > 0 ? new double[] {1, 0} : new double[] {0, 1});
        return new AnnTrainer().backpropagateSoftmaxLogLikelyhood(model, data, output, sum, null, reuseGradient);
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
