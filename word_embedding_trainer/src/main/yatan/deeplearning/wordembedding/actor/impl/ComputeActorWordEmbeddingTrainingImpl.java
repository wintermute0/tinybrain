package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import scala.actors.threadpool.Arrays;

import com.google.inject.Inject;

import yatan.ann.AnnActivationFunctions;
import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.DropoutAnnModel;
import yatan.commons.matrix.Matrix;
import yatan.commons.ml.SingleFunction;
import yatan.deeplearning.wordembedding.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class ComputeActorWordEmbeddingTrainingImpl extends AbstractComputeActorContractImpl {
    private static final int MINIBATCH_SIZE = 200;

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
            AnnData annData = Helper.convertToSoftmaxAnnData(wordEmbedding, instance);

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), sum);

            // bp
            newGradient =
                    trainer.backpropagateSoftmaxLogLikelyhood(annModel, annData, output, sum,
                            this.trainerConfiguration.l2Lambdas, newGradient);

            // save gradient
            batchGradient = saveGradient(batchGradient, newGradient);

            // save wordEmbeddingDelta
            saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
                    annData.getInput(), instance);
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

    private AnnGradient wordEmbeddingBackpropagate(AnnModel model, AnnData data, double[][] output, double[][] sum,
            AnnGradient annGradient) {
        double l2Lambda = 0;

        // this controls the direction of the gradient, it's either 1 or -1
        double factor = (int) data.getOutput()[0];

        // compute delta U
        Matrix deltaU =
                annGradient == null ? new Matrix(model.getLayer(1).rowSize(), model.getLayer(1).columnSize())
                        : annGradient.getGradients().get(1);
        for (int i = 0; i < model.getLayer(1).rowSize() - 1; i++) {
            // the -2 * U is L2 regularazation - 2 *model.getLayer(1).getData()[i][0]
            deltaU.getData()[i][0] = output[0][i] * factor - l2Lambda * model.getLayer(1).getData()[i][0];
        }

        // compute delta W
        double[] delta = new double[model.getLayer(1).rowSize()];
        Matrix deltaW =
                annGradient == null ? new Matrix(model.getLayer(0).rowSize(), model.getLayer(0).columnSize())
                        : annGradient.getGradients().get(0);
        SingleFunction<Double, Double> activation =
                AnnActivationFunctions.activationFunction(model.getConfiguration().activationFunctionOfLayer(0));
        for (int i = 0; i < deltaW.columnSize(); i++) {
            delta[i] = model.getLayer(1).getData()[i][0] * activation.derivative(sum[0][i]) * factor;
            for (int j = 0; j < deltaW.rowSize() - 1; j++) {
                // the -2 * W is L2 regularazation: - 2 * model.getLayer(0).getData()[j][i] * factor
                deltaW.getData()[j][i] =
                        delta[i] * data.getInput()[j] - l2Lambda * model.getLayer(0).getData()[j][i] * factor;
            }
            // the -2 * b is L2 regularazation: - 2 * model.getLayer(0).getData()[deltaW.rowSize() - 1][i] * factor
            deltaW.getData()[deltaW.rowSize() - 1][i] =
                    delta[i] - l2Lambda * model.getLayer(0).getData()[deltaW.rowSize() - 1][i] * factor;
        }

        // compute delta X
        double[] deltaX = model.getLayer(0).multiply(Arrays.copyOf(delta, delta.length - 1));

        // regularize x
        for (int i = 0; i < deltaX.length - 1; i++) {
            deltaX[i] -= l2Lambda * data.getInput()[i] * factor;
        }

        // output
        if (annGradient != null) {
            annGradient.setDeltaForInputLayer(deltaX);
            return annGradient;
        } else {
            List<Matrix> gradients = new ArrayList<Matrix>();
            gradients.add(deltaW);
            gradients.add(deltaU);
            return new AnnGradient(gradients, deltaX);
        }
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
