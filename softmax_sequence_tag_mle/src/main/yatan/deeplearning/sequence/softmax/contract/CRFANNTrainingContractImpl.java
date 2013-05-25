package yatan.deeplearning.sequence.softmax.contract;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import scala.actors.threadpool.Arrays;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
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
    private WordEmbedding wordEmbedding;
    @Inject
    private int tagCount;

    private Random random = new Random(new Date().getTime());

    @Override
    protected int requestDataSize() {
        return 1;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Object[] serializables = (Object[]) parameter.getSerializable();
        AnnModel annModel = (AnnModel) serializables[0];
        double[][] tagTransitionWeights = (double[][]) serializables[1];

        @SuppressWarnings("unchecked")
        List<WordEmbeddingTrainingInstance> instances =
                (List<WordEmbeddingTrainingInstance>) dataset.get(0).getSerializable();

        Object[] gradients = backpropagate(annModel, tagTransitionWeights, instances, tagCount);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(gradients);
        result.setGradient(gradientWrapper);

        return result;
    }

    private Object[] backpropagate(AnnModel annModel, double[][] tagTransitionWeights,
            List<WordEmbeddingTrainingInstance> instances, int tagCount) {
        double[][][] sums = new double[instances.size()][][];
        double[][][] outputs = new double[instances.size()][][];
        double[][] annInputs = new double[instances.size()][];

        // first compute all f
        double[][] f = computeF(this.wordEmbedding, annModel, instances, annInputs, sums, outputs);

        // generate a cd sample
        int[] tags = getTags(instances);
        int[] sample = gibbsSample(f, tagTransitionWeights, tags, tagCount);

        // compute transition score gradient
        double[][] transitaionGradient = new double[tagTransitionWeights.length][tagTransitionWeights[0].length];
        transitaionGradient[tagCount][tags[0]] = 1;
        transitaionGradient[tags[tags.length - 1]][tagCount + 1] = 1;
        transitaionGradient[tagCount][sample[0]] -= 1;
        transitaionGradient[sample[sample.length - 1]][tagCount + 1] -= 1;
        for (int i = 1; i < tags.length - 1; i++) {
            transitaionGradient[tags[i - 1]][tags[i]]++;
            transitaionGradient[sample[i - 1]][sample[i]]--;
        }

        // ann bp
        AnnTrainer trainer = new AnnTrainer();
        AnnGradient batchGradient = null;
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();
        Multiset<Integer> wordAppearCount = HashMultiset.create();

        AnnGradient newGradient = null;
        for (int i = 0; i < sample.length; i++) {
            double[] gradient = new double[tagCount];
            gradient[tags[i]] = 1;
            gradient[sample[i]] -= 1;

            newGradient =
                    trainer.backpropagateWithGradient(gradient, annModel, annInputs[i], outputs[i], sums[i], null,
                            newGradient);
            batchGradient = saveGradient(batchGradient, newGradient);

            saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
                    annInputs[i], instances.get(i));
            // update word appear count
            for (Integer word : instances.get(i).getInput()) {
                wordAppearCount.add(word);
            }
        }

        // average batch gradient
        if (batchGradient != null) {
            batchGradient.averageBy(instances.size());
        }

        // average word embedding gradient
        for (Entry<Integer, Double[]> entry : batchWordEmbeddingDelta.entrySet()) {
            Double[] gradient = entry.getValue();
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] /= wordAppearCount.count(entry.getKey());
            }
        }

        return new Object[] {batchGradient, batchGradient, transitaionGradient};
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

            outputs[i] = trainer.run(annModel, annInputs[i], sums[i]);
            results[i] = outputs[i][outputs[i].length - 1];
        }

        return results;
    }

    private int[] gibbsSample(double[][] f, double[][] tagTransitionWeight, int[] tags, int tagCount) {
        int[] sample = Arrays.copyOf(tags, tags.length);
        double[] factorValues = new double[tagCount];
        for (int i = 0; i < sample.length; i++) {
            double totalFactorValue = 0;
            // sample new yi
            for (int j = 0; j < factorValues.length; j++) {
                double potential1 = i == 0 ? tagTransitionWeight[tagCount][j] : tagTransitionWeight[i - 1][j];
                double potential2 =
                        i == sample.length - 1 ? tagTransitionWeight[tagCount + 1][j] : tagTransitionWeight[i + 1][j];
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
