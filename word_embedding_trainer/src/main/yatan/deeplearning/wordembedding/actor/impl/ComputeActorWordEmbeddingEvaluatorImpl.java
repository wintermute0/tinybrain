package yatan.deeplearning.wordembedding.actor.impl;

import java.io.Serializable;

import java.util.List;

import com.google.inject.Inject;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.DropoutAnnModel;
import yatan.deeplearning.wordembedding.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class ComputeActorWordEmbeddingEvaluatorImpl extends AbstractComputeActorContractImpl {
    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Inject
    private TrainerConfiguration trainerConfiguration;

    @Override
    protected int requestDataSize() {
        return 50000;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        AnnModel annModel = (AnnModel) parameters[1];

        if (this.trainerConfiguration.dropout) {
            annModel = new DropoutAnnModel(annModel, false);
        }

        double totalPositiveScore = 0;
        int positiveCount = 0;
        double totalNegativeScore = 0;
        int negativeCount = 0;

        AnnTrainer trainer = new AnnTrainer();
        int accurate = 0;
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();

            // first convert input data into word embedding
            AnnData annData = Helper.convertToSoftmaxAnnData(wordEmbedding, instance);

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), new double[annModel.getLayerCount()][]);
            // System.out.println(output[annModel.getLayerCount() - 1][0]);
            if (instance.getOutput() > 0) {
                if (output[annModel.getLayerCount() - 1][0] > 0.5) {
                    accurate++;
                }

                totalPositiveScore += output[annModel.getLayerCount() - 1][0];
                positiveCount++;
            } else {
                if (output[annModel.getLayerCount() - 1][0] < 0.5) {
                    accurate++;
                }

                totalNegativeScore += output[annModel.getLayerCount() - 1][0];
                negativeCount++;
            }
        }

        getLogger().info("Average positive score: " + totalPositiveScore / positiveCount);
        getLogger().info("Average negative score: " + totalNegativeScore / negativeCount);
        getLogger().info("Precision: " + 100.0 * accurate / requestDataSize() + "%");
        LogUtility.logWordEmbedding(getLogger(), wordEmbedding, "的");
        LogUtility.logWordEmbedding(getLogger(), wordEmbedding, "吴");
        LogUtility.logWordEmbedding(getLogger(), wordEmbedding, "煮");
        LogUtility.logWordEmbedding(getLogger(), wordEmbedding);
        LogUtility.logAnnModel(getLogger(), annModel);

        // evaluate some word distance rank
        // evaluateWordDistanceRank(wordEmbedding, "france", "germany", "greece", "spain", "america", "canada", "china",
        // "denmark", "egypt", "australia", "brazil");
        evaluateWordDistanceRank(wordEmbedding, "看", "见", "视", "瞧", "瞄", "目", "相", "窥", "探", "扫", "瞪", "望");
        evaluateWordDistanceRank(wordEmbedding, "吴", "赵", "钱", "孙", "李", "周", "郑", "王", "冯", "陈", "褚", "卫");

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }

    private void evaluateWordDistanceRank(WordEmbedding wordEmbedding, String word, String... candidates) {
        int[] rank = new int[candidates.length];
        int wordIndex = wordEmbedding.getDictionary().indexOf(word);
        double[] distances = new double[wordEmbedding.getDictionary().size()];
        for (int i = 0; i < distances.length; i++) {
            distances[i] = wordEmbedding.distanceBetween(wordIndex, i);
        }

        for (int i = 0; i < candidates.length; i++) {
            double distance =
                    wordEmbedding.distanceBetween(wordIndex, wordEmbedding.getDictionary().indexOf(candidates[i]));
            for (double otherDistance : distances) {
                if (otherDistance < distance) {
                    rank[i]++;
                }
            }
        }

        StringBuilder sb = new StringBuilder("Distance ranking for '" + word + "': ");
        for (int i = 0; i < candidates.length; i++) {
            sb.append(candidates[i] + " = " + rank[i] + ", ");
        }
        getLogger().info(sb.toString());
        System.out.println(sb.toString());
    }
}
