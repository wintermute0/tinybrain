package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.AnnTrainer.LayerPostProcessor;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.wordembedding.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class ComputeActorWordEmbeddingTrainingImpl extends AbstractComputeActorContractImpl {
    private static final int MINIBATCH_SIZE = 20;
    private final Random random = new Random(new Date().getTime());

    private int[][] activeCounts;

    @Inject(optional = true)
    @Named("fix_bottom_layers")
    private int fixBottomLayers;

    @Inject
    private TrainerConfiguration trainerConfiguration;

    @Override
    protected int requestDataSize() {
        return MINIBATCH_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        AnnModel annModel = (DefaultAnnModel) parameters[1];

        // AnnGradient totalGradient = null;
        AnnGradient batchGradient = null;
        // HashMap<Integer, Double[]> totalWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        int[] wordEmbeddingPositionActivationCount = new int[annModel.getConfiguration().inputDegree];
        HashMap<Integer, Integer[]> wordEmbeddingActivationCount = Maps.newHashMap();
        Multiset<Integer> wordAppearCount = HashMultiset.create();

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        // int batchCount = 0;
        // reuse the matrices in the gradient
        AnnGradient newGradient = null;
        LayerPostProcessor dropoutPostProcessor = null;

        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            // batchCount++;

            // use dropout ann model
            if (this.trainerConfiguration.dropout) {
                dropoutPostProcessor = new DropoutPostProcessor(annModel);
            }

            // first convert input data into word embedding
            AnnData annData = Helper.convertToSoftmaxAnnData(wordEmbedding, instance);

            // dropout word embedding
            boolean dropMasks[] = null;
            if (this.trainerConfiguration.wordEmbeddingDropout) {
                dropMasks = new boolean[annData.getInput().length];
                for (int i = 0; i < dropMasks.length; i++) {
                    if (this.random.nextDouble() < this.trainerConfiguration.wordEmbeddingDropoutRate) {
                        dropMasks[i] = true;
                        annData.getInput()[i] = 0;
                    } else {
                        wordEmbeddingPositionActivationCount[i]++;

                        int wordIndex = instance.getInput().get(i / wordEmbedding.getWordVectorSize());
                        Integer[] wordActivationCount = wordEmbeddingActivationCount.get(wordIndex);
                        if (wordActivationCount == null) {
                            wordActivationCount = new Integer[wordEmbedding.getWordVectorSize()];
                            wordEmbeddingActivationCount.put(wordIndex, wordActivationCount);
                        }

                        if (wordActivationCount[i % wordEmbedding.getWordVectorSize()] == null) {
                            wordActivationCount[i % wordEmbedding.getWordVectorSize()] = 1;
                        } else {
                            wordActivationCount[i % wordEmbedding.getWordVectorSize()]++;
                        }
                    }
                }
            }

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), sum, dropoutPostProcessor);

            // bp
            newGradient =
                    trainer.backpropagateSoftmaxLogLikelyhood(annModel, annData, output, sum,
                            this.trainerConfiguration.l2Lambdas, newGradient, dropoutPostProcessor);
            if (this.fixBottomLayers > 0) {
                newGradient.setDeltaForInputLayer(null);
            }

            // drop some gradient
            if (this.trainerConfiguration.wordEmbeddingDropout) {
                for (int i = 0; i < dropMasks.length; i++) {
                    if (dropMasks[i]) {
                        newGradient.getDeltaForInputLayer()[i] = 0;
                    }
                }
            }

            // save gradient
            batchGradient = saveGradient(batchGradient, newGradient);

            // save wordEmbeddingDelta
            if (newGradient.getDeltaForInputLayer() != null) {
                saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
                        annData.getInput(), instance);

                // update word appear count
                for (Integer word : instance.getInput()) {
                    wordAppearCount.add(word);
                }
            }
        }

        // System.out.println(LogUtility.buildLogString(new double[][] {this.rollingActivation}));

        // average batch gradient
        if (this.trainerConfiguration.dropout) {
            for (int layer = 0; layer < batchGradient.getGradients().size(); layer++) {
                Matrix matrix = batchGradient.getGradients().get(layer);
                double[][] data = matrix.getData();
                for (int i = 0; i < matrix.rowSize(); i++) {
                    int inputActivationCount;
                    if (i == matrix.rowSize() - 1) {
                        inputActivationCount = MINIBATCH_SIZE;
                    } else {
                        inputActivationCount =
                                layer == 0 ? wordEmbeddingPositionActivationCount[i] : this.activeCounts[layer - 1][i];
                    }

                    for (int j = 0; j < matrix.columnSize(); j++) {
                        int neuralActivationCount =
                                layer < annModel.getLayerCount() - 1 ? this.activeCounts[layer][j] : MINIBATCH_SIZE;
                        int actualCount = Math.min(neuralActivationCount, inputActivationCount);
                        if (actualCount > 0) {
                            data[i][j] /= actualCount;
                        }
                    }
                }
            }
        } else {
            batchGradient.averageBy(MINIBATCH_SIZE);
        }

        // average word embedding gradient
        for (Entry<Integer, Double[]> entry : batchWordEmbeddingDelta.entrySet()) {
            Double[] gradient = entry.getValue();
            if (this.trainerConfiguration.wordEmbeddingDropout) {
                Integer[] wordActivationCount = wordEmbeddingActivationCount.get(entry.getKey());
                if (wordActivationCount != null) {
                    for (int i = 0; i < gradient.length; i++) {
                        if (wordActivationCount[i] != null) {
                            gradient[i] /= wordActivationCount[i];
                        }
                    }
                }
            } else {
                for (int i = 0; i < gradient.length; i++) {
                    gradient[i] /= wordAppearCount.count(entry.getKey());
                }
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

    // private AnnGradient wordEmbeddingBackpropagate(AnnModel model, AnnData data, double[][] output, double[][] sum,
    // AnnGradient annGradient) {
    // double l2Lambda = 0;
    //
    // // this controls the direction of the gradient, it's either 1 or -1
    // double factor = (int) data.getOutput()[0];
    //
    // // compute delta U
    // Matrix deltaU =
    // annGradient == null ? new Matrix(model.getLayer(1).rowSize(), model.getLayer(1).columnSize())
    // : annGradient.getGradients().get(1);
    // for (int i = 0; i < model.getLayer(1).rowSize() - 1; i++) {
    // // the -2 * U is L2 regularazation - 2 *model.getLayer(1).getData()[i][0]
    // deltaU.getData()[i][0] = output[0][i] * factor - l2Lambda * model.getLayer(1).getData()[i][0];
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
    // deltaW.getData()[j][i] =
    // delta[i] * data.getInput()[j] - l2Lambda * model.getLayer(0).getData()[j][i] * factor;
    // }
    // // the -2 * b is L2 regularazation: - 2 * model.getLayer(0).getData()[deltaW.rowSize() - 1][i] * factor
    // deltaW.getData()[deltaW.rowSize() - 1][i] =
    // delta[i] - l2Lambda * model.getLayer(0).getData()[deltaW.rowSize() - 1][i] * factor;
    // }
    //
    // // compute delta X
    // double[] deltaX = model.getLayer(0).multiply(Arrays.copyOf(delta, delta.length - 1));
    //
    // // regularize x
    // for (int i = 0; i < deltaX.length - 1; i++) {
    // deltaX[i] -= l2Lambda * data.getInput()[i] * factor;
    // }
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

    private AnnGradient saveGradient(AnnGradient gradient, AnnGradient newGradient) {
        AnnGradient batchGradient;
        if (gradient == null) {
            batchGradient = newGradient.clone();
        } else {
            gradient.updateByPlus(newGradient);
            batchGradient = gradient;
        }

        for (int i = 0; i < this.fixBottomLayers; i++) {
            batchGradient.getGradients().set(i, null);
        }

        return batchGradient;
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

    private class DropoutPostProcessor implements LayerPostProcessor {
        private final boolean[][] dropoutMasks;

        public DropoutPostProcessor(AnnModel annModel) {
            if (activeCounts == null) {
                activeCounts = new int[annModel.getLayerCount() - 1][];
            }

            this.dropoutMasks = new boolean[annModel.getLayerCount() - 1][];
            for (int i = 0; i < dropoutMasks.length; i++) {
                if (activeCounts[i] == null) {
                    activeCounts[i] = new int[annModel.getLayer(i).columnSize()];
                }
                dropoutMasks[i] = new boolean[annModel.getLayer(i).columnSize()];
                for (int j = 0; j < dropoutMasks[i].length; j++) {
                    if (random.nextDouble() < 0.5) {
                        dropoutMasks[i][j] = true;
                    } else {
                        activeCounts[i][j]++;
                    }
                }
            }
        }

        @Override
        public void process(int layer, double[] output) {
            if (layer < this.dropoutMasks.length) {
                for (int i = 0; i < output.length; i++) {
                    if (this.dropoutMasks[layer][i]) {
                        output[i] = 0;
                    }
                }
            }
        }
    }
}
