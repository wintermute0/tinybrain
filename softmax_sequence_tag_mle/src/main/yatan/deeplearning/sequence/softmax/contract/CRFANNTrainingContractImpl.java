package yatan.deeplearning.sequence.softmax.contract;

import java.io.Serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import scala.actors.threadpool.Arrays;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.sequence.softmax.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class CRFANNTrainingContractImpl extends AbstractComputeActorContractImpl {
    @Inject
    private Dictionary dictionary;
    @Inject
    @Named("tag_count")
    private int tagCount;
    @Inject
    private TrainerConfiguration trainerConfiguration;

    private Random random = new Random(new Date().getTime());

    @Override
    protected int requestDataSize() {
        return 1;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Object[] serializables = (Object[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) serializables[0];
        AnnModel annModel = (AnnModel) serializables[1];
        double[][] tagTransitionWeights = (double[][]) serializables[2];

        @SuppressWarnings("unchecked")
        List<WordEmbeddingTrainingInstance> instances =
                (List<WordEmbeddingTrainingInstance>) dataset.get(0).getSerializable();

        Object[] gradients = backpropagate(wordEmbedding, annModel, tagTransitionWeights, instances, tagCount);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(gradients);
        result.setGradient(gradientWrapper);

        return result;
    }

    private Object[] backpropagate(WordEmbedding wordEmbedding, AnnModel annModel, double[][] tagTransitionWeights,
            List<WordEmbeddingTrainingInstance> instances, int tagCount) {
        double[][][] sums = new double[instances.size()][][];
        double[][][] outputs = new double[instances.size()][][];
        double[][] annInputs = new double[instances.size()][];

        // first compute all f
        double[][] f = computeF(wordEmbedding, annModel, instances, annInputs, sums, outputs);

        // generate a cd sample
        int[] tags = getTags(instances);
        // int[] sample = singleGibbsSample(f, tagTransitionWeights, tags, tagCount);
        Object[] sampleResults = gibbsSample(f, tagTransitionWeights, tagCount, 500);
        double[][] tagExpectation = (double[][]) sampleResults[0];
        double[][] transitionExpectation = (double[][]) sampleResults[1];

        // compute transition score gradient
        double[][] transitaionGradient = new double[tagTransitionWeights.length][tagTransitionWeights[0].length];
        transitaionGradient[tagCount][tags[0]] = 1;
        transitaionGradient[tags[tags.length - 1]][tagCount + 1] = 1;
        for (int i = 1; i < tags.length - 1; i++) {
            transitaionGradient[tags[i - 1]][tags[i]]++;
        }

        for (int i = 0; i < transitaionGradient.length; i++) {
            for (int j = 0; j < transitaionGradient[i].length; j++) {
                transitaionGradient[i][j] -= transitionExpectation[i][j];
            }
        }

        // // average transition gradient
        // for (int i = 0; i < tagCount; i++) {
        // for (int j = 0; j < tagCount; j++) {
        // transitaionGradient[i][j] /= instances.size();
        // }
        // }

        // ann bp
        AnnTrainer trainer = new AnnTrainer();
        AnnGradient batchGradient = null;
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        Multiset<Integer> wordAppearCount = HashMultiset.create();

        AnnGradient newGradient = null;
        for (int i = 0; i < tags.length; i++) {
            // if (tags[i] == sample[i]) {
            // continue;
            // }

            double[] gradient = new double[tagCount];
            gradient[tags[i]] = 1;
            for (int j = 0; j < gradient.length; j++) {
                gradient[j] -= tagExpectation[i][j];

                // double y = outputs[i][annModel.getLayerCount() - 1][j];
                // gradient[j] *= y * (1 - y);
            }

            newGradient =
                    trainer.backpropagateWithGradient(gradient, annModel, annInputs[i], outputs[i], sums[i],
                            this.trainerConfiguration.l2Lambdas, newGradient);
            batchGradient = saveGradient(batchGradient, newGradient);

            saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
                    annInputs[i], instances.get(i));
            // update word appear count
            for (Integer word : instances.get(i).getInput()) {
                wordAppearCount.add(word);
            }
        }

        // // average batch gradient
        // if (batchGradient != null) {
        // batchGradient.averageBy(instances.size());
        // }
        //
        // // average word embedding gradient
        // for (Entry<Integer, Double[]> entry : batchWordEmbeddingDelta.entrySet()) {
        // Double[] gradient = entry.getValue();
        // for (int i = 0; i < gradient.length; i++) {
        // gradient[i] /= wordAppearCount.count(entry.getKey());
        // }
        // }

        return new Serializable[] {batchGradient, batchWordEmbeddingDelta, transitaionGradient};
    }

    private int[] getTags(List<WordEmbeddingTrainingInstance> instances) {
        int[] tags = new int[instances.size()];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = instances.get(i).getOutput();
        }

        return tags;
    }

    public static double[][] computeF(WordEmbedding wordEmbedding, AnnModel annModel,
            List<WordEmbeddingTrainingInstance> instances, double[][] annInputs, double[][][] sums, double[][][] outputs) {

        AnnTrainer trainer = new AnnTrainer();
        double[][] results = new double[instances.size()][];
        for (int i = 0; i < instances.size(); i++) {
            WordEmbeddingTrainingInstance instance = instances.get(i);

            annInputs[i] = wordEmbedding.lookup(instance.getInput());

            sums[i] = new double[annModel.getLayerCount()][];
            outputs[i] = trainer.run(annModel, annInputs[i], sums[i]);
            results[i] = outputs[i][outputs[i].length - 1];
        }

        return results;
    }

    private Object[] gibbsSample(double[][] f, double[][] tagTransitionWeight, int tagCount, int step) {
        int[] tags = new int[f.length];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = this.random.nextInt(tagCount);
        }

        double[][] cumulativeTransition = new double[tagTransitionWeight.length][tagTransitionWeight[0].length];
        double[][] cumulativeSamples = new double[f.length][tagCount];
        int cumulatedCount = 0;
        for (int i = 0; i < step; i++) {
            tags = singleGibbsSample(f, tagTransitionWeight, tags, tagCount, true);
            if (i > step * 0.5) {
                cumulatedCount++;
                for (int j = 0; j < tags.length; j++) {
                    cumulativeSamples[j][tags[j]]++;
                    if (j == 0) {
                        cumulativeTransition[tagCount][tags[j]]++;
                    } else if (j == tags.length - 1) {
                        cumulativeTransition[tags[j]][tagCount + 1]++;
                    } else {
                        cumulativeTransition[tags[j - 1]][tags[j]]++;
                    }
                }
            }
        }

        for (int i = 0; i < tags.length; i++) {
            for (int j = 0; j < tagCount; j++) {
                cumulativeSamples[i][j] /= cumulatedCount;
            }
        }

        for (int i = 0; i < cumulativeTransition.length; i++) {
            for (int j = 0; j < cumulativeTransition[i].length; j++) {
                cumulativeTransition[i][j] /= cumulatedCount;
            }
        }

        return new Object[] {cumulativeSamples, cumulativeTransition};
    }

    private int[] singleGibbsSample(double[][] f, double[][] tagTransitionWeight, int[] tags, int tagCount,
            boolean reuseTags) {
        int[] sample = reuseTags ? tags : Arrays.copyOf(tags, tags.length);
        double[] factorValues = new double[tagCount];
        for (int i = 0; i < sample.length; i++) {
            double totalFactorValue = 0;
            // sample new yi
            for (int j = 0; j < factorValues.length; j++) {
                double potential1 = i == 0 ? tagTransitionWeight[tagCount][j] : tagTransitionWeight[sample[i - 1]][j];
                double potential2 =
                        i == sample.length - 1 ? tagTransitionWeight[tagCount + 1][j]
                                : tagTransitionWeight[sample[i + 1]][j];
                factorValues[j] = Math.exp(f[i][j] + potential1 + potential2);
                totalFactorValue += factorValues[j];
            }

            double nextValue = this.random.nextDouble();
            double valueSoFar = 0;
            int newYi = 0;
            for (; newYi < factorValues.length; newYi++) {
                valueSoFar += factorValues[newYi] / totalFactorValue;
                if (valueSoFar >= nextValue || newYi == factorValues.length - 1) {
                    break;
                }
            }

            sample[i] = newYi;
        }

        return sample;
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
    // private static double computeLogaddS(double[][] f, double[][] tagTransitionWeight) {
    // double[][] delta = new double[f.length][f[0].length];
    // for (int t = 0; t < delta.length; t++) {
    // if (t == 0) {
    // for (int j = 0; j < delta[t].length; j++) {
    // delta[t][j] = f[t][j] + Math.log(Math.exp(tagTransitionWeight[0][j]));
    // }
    // } else {
    // for (int k = 0; k < delta[t].length; k++) {
    // double sumDeltaTMinusOnePlusAik = 0;
    // for (int i = 1; i < tagTransitionWeight.length; i++) {
    // sumDeltaTMinusOnePlusAik += Math.exp(delta[t - 1][i] + tagTransitionWeight[i][k]);
    // }
    // delta[t][k] = f[t][k] + Math.log(sumDeltaTMinusOnePlusAik);
    // }
    // }
    // }
    //
    // // sum over the last delta
    // double result = 0;
    // for (int i = 0; i < delta[0].length; i++) {
    // result += Math.exp(delta[delta.length - 1][i]);
    // }
    //
    // return Math.log(result);
    // }
    //
    // private static double computeScore(double[][] f, double[][] tagTransitionWeight, int[] tags) {
    // double score = 0;
    // int lastTag = 0;
    // for (int i = 0; i < tags.length; i++) {
    // int tag = tags[i];
    // score += tagTransitionWeight[lastTag][tag] + f[i][tag];
    //
    // lastTag = tag;
    // }
    //
    // return score;
    // }
}
