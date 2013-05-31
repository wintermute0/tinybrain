package yatan.deeplearning.autoencoder.contract;

import java.util.Arrays;
import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.AnnTrainer.LayerPostProcessor;
import yatan.deeplearning.autoencoder.contract.AutoEncoderTrainingContractImpl.NoisingLayerPostProcessor;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AutoEncoderEvaluatorContractImpl extends AbstractComputeActorContractImpl {
    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Override
    protected int requestDataSize() {
        return 10000;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        WordEmbedding wordEmbedding = (WordEmbedding) ((Object[]) parameter.getSerializable())[0];
        final AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[1];

        double totalError = 0;
        double totalRelativeError = 0;

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            if (instance.getOutput() < 0) {
                continue;
            }

            AnnData annData = (AnnData) Helper.convertToAnnData(wordEmbedding, instance);

            // // setup corruption post processor to corrupt the input
            NoisingLayerPostProcessor postProcessor = null;
            if (annModel.getLayerCount() == 2) {
                // corrupt input
                Helper.corruptWithMask(annData.getInput());
            } else {
                postProcessor = new NoisingLayerPostProcessor(annModel.getLayerCount() - 3);
            }
            // calculate ANN output
            double[][] output = trainer.run(annModel, annData.getInput(), sum, postProcessor);
            // set up ann data output to the clean input
            if (postProcessor != null) {
                annData = new AnnData(annData.getInput(), postProcessor.getCleanData());
            }

            double squreError = 0;
            double squre = 0;
            for (int i = 0; i < output[output.length - 1].length; i++) {
                squreError += Math.pow(output[output.length - 1][i] - annData.getOutput()[i], 2);
                squre += Math.pow(annData.getOutput()[i], 2);
            }
            totalError += Math.sqrt(squreError / output[output.length - 1].length);
            totalRelativeError +=
                    Math.sqrt(squreError / output[output.length - 1].length)
                            / Math.sqrt(squre / output[output.length - 1].length);
        }

        String message =
                "Average error: " + totalError / dataset.size() + ". Relative error: " + totalRelativeError
                        / dataset.size();
        getLogger().info(message);
        System.out.println(message);

        // evaluateWordDistanceRank(wordEmbedding, "看", "见", "视", "瞧", "瞄", "目", "相", "窥", "探", "扫", "瞪", "望");
        // evaluateWordDistanceRank(wordEmbedding, "吴", "赵", "钱", "孙", "李", "周", "郑", "王", "冯", "陈", "褚", "卫");
        // LogUtility.logWordEmbedding(getLogger(), wordEmbedding);

        LogUtility.logAnnModel(getLogger(), annModel);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }

    // private void evaluateWordDistanceRank(WordEmbedding wordEmbedding, String word, String... candidates) {
    // int[] rank = new int[candidates.length];
    // int wordIndex = wordEmbedding.getDictionary().indexOf(word);
    // double[] distances = new double[wordEmbedding.getDictionary().size()];
    // for (int i = 0; i < distances.length; i++) {
    // distances[i] = wordEmbedding.distanceBetween(wordIndex, i);
    // }
    //
    // for (int i = 0; i < candidates.length; i++) {
    // double distance =
    // wordEmbedding.distanceBetween(wordIndex, wordEmbedding.getDictionary().indexOf(candidates[i]));
    // for (double otherDistance : distances) {
    // if (otherDistance < distance) {
    // rank[i]++;
    // }
    // }
    // }
    //
    // StringBuilder sb = new StringBuilder("Distance ranking for '" + word + "': ");
    // for (int i = 0; i < candidates.length; i++) {
    // sb.append(candidates[i] + " = " + rank[i] + ", ");
    // }
    // getLogger().info(sb.toString());
    // System.out.println(sb.toString());
    // }
}
