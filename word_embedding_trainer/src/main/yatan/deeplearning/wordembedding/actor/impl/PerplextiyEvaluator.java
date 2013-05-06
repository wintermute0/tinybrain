package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class PerplextiyEvaluator extends AbstractComputeActorContractImpl {
    private static final int REQUEST_DATA_SIZE = 1000;
    private static final int REPEAT_DELAY_IN_SECONDS = 20 * 60;

    private final Dictionary dictionary;

    @Inject
    public PerplextiyEvaluator(Dictionary dictionary) {
        Preconditions.checkArgument(dictionary != null);

        this.dictionary = dictionary;
    }

    @Override
    protected int requestDataSize() {
        return REQUEST_DATA_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        long start = new Date().getTime();
        getLogger().info("Start calculating perplexity...");

        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        DefaultAnnModel annModel = (DefaultAnnModel) parameters[1];

        double crossEntropy = calculateCrossEntropy(this.dictionary, wordEmbedding, annModel, dataset);
        double perplexity = Math.pow(2, crossEntropy);
        getLogger().info(
                "Perplexity = " + perplexity + ", cross entropy = " + crossEntropy + ", calculation cost "
                        + (new Date().getTime() - start) / 1000.0f + "s.");
        System.out.println("Perplexity = " + perplexity + ", cross entropy = " + crossEntropy);

        ComputeResult result = new ComputeResult();
        result.setGradient(null);
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(REPEAT_DELAY_IN_SECONDS);
        return result;
    }

    /**
     * @param dictionary
     * @param wordEmbedding
     * @param annModel
     * @return
     * @throws Exception
     */
    private double calculateCrossEntropy(Dictionary dictionary, WordEmbedding wordEmbedding, DefaultAnnModel annModel,
            List<Data> dataset) {
        AnnTrainer trainer = new AnnTrainer();
        double[] outputs = new double[wordEmbedding.getDictionary().size()];
        double outputSum = 0;
        double hT = 0;
        int positiveInstanceCount = 0;
        int totalRank = 0;
        for (Data data : dataset) {
            List<Integer> outputRank = Lists.newArrayList();
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            if (instance.getOutput() < 0.000001) {
                // ignore negative case
                continue;
            }

            positiveInstanceCount++;

            outputSum = 0;
            int actualWordIndex = instance.getInput().get(instance.getInput().size() / 2);
            for (int i = 0; i < wordEmbedding.getDictionary().size(); i++) {
                instance.getInput().set(instance.getInput().size() / 2, i);
                double output = runWordEmbeddingInstance(wordEmbedding, annModel, trainer, instance);
                outputs[i] = output;
                outputSum += output;

                int rank = 0;
                while (rank < outputRank.size() && output < outputs[outputRank.get(rank)]) {
                    rank++;
                }
                outputRank.add(rank, i);
            }

            int rank = outputRank.indexOf(actualWordIndex);
            if (rank > 4000) {
                // ignore this because it's probably &NUM& or something like it
                continue;
            }

            totalRank += rank;

            double pw = outputs[actualWordIndex] / outputSum;
            // System.out.print(positiveInstanceCount + ": P(w) = " + pw + ". ");
            hT += Math.log(pw) / Math.log(2);
            double ce = -1.0 / positiveInstanceCount * hT;

            if (Double.isNaN(ce) || Double.isInfinite(ce)) {
                return ce;
            }
            // System.out.println("Cross entropy = " + ce + ". Perperlexity = " + Math.pow(2, ce));
        }

        System.out.println("Average actual word rank is: " + (1.0 * totalRank / positiveInstanceCount));
        getLogger().info("Average actual word rank is: " + (1.0 * totalRank / positiveInstanceCount));

        return -1.0 / positiveInstanceCount * hT;
    }

    private static double runWordEmbeddingInstance(WordEmbedding wordEmbedding, DefaultAnnModel annModel,
            AnnTrainer trainer, WordEmbeddingTrainingInstance instance) {
        // first convert input data into word embedding
        // FIXME: could reuse an array, no need to allocate it every time
        double[] input = new double[instance.getInput().size() * wordEmbedding.getWordVectorSize()];
        for (int i = 0; i < instance.getInput().size(); i++) {
            int index = instance.getInput().get(i);
            for (int j = 0; j < wordEmbedding.getWordVectorSize(); j++) {
                input[i * wordEmbedding.getWordVectorSize() + j] = wordEmbedding.getMatrix().getData()[j][index];
            }
        }

        // train with this ann data instance and update gradient
        double[][] output = trainer.run(annModel, input, new double[annModel.getLayerCount()][]);

        return output[annModel.getLayerCount() - 1][1];
    }
}
