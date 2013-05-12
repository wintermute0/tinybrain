package yatan.deeplearning.autoencoder.contract;

import java.util.List;

import scala.actors.threadpool.Arrays;
import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class WordPredictionEvaluator extends AbstractComputeActorContractImpl {
    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Override
    protected int requestDataSize() {
        return 1000;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        WordEmbedding wordEmbedding = (WordEmbedding) ((Object[]) parameter.getSerializable())[0];
        final AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[1];

        int totalWordRank = 0;

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            AnnData annData = (AnnData) Helper.convertToAnnData(wordEmbedding, instance);

            double[] actualWord = Arrays.copyOfRange(annData.getInput(), 2 * 50, 3 * 50);

            // setup corruption post processor to corrupt the input
            if (annModel.getLayerCount() == 2) {
                // corrupt input
                for (int i = 2 * 50; i < 3 * 50; i++) {
                    annData.getInput()[i] = 0;
                }
            }

            // calculate ANN output
            double[][] output = trainer.run(annModel, annData.getInput(), sum);

            double[] predictedWord = Arrays.copyOfRange(output[output.length - 1], 2 * 50, 3 * 50);
            double predictedWordDistance = distance(actualWord, predictedWord);

            int wordRank = 0;
            double[] wordData = new double[wordEmbedding.getWordVectorSize()];
            for (int column = 0; column < wordEmbedding.getMatrix().columnSize(); column++) {
                for (int row = 0; row < wordEmbedding.getMatrix().rowSize(); row++) {
                    wordData[row] = wordEmbedding.getMatrix().getData()[row][column];
                }

                double distance = distance(predictedWord, wordData);
                if (distance < predictedWordDistance) {
                    wordRank++;
                }
            }

            totalWordRank += wordRank;
        }

        String message = "Average word rank: " + 1.0 * totalWordRank / dataset.size();
        getLogger().info(message);
        System.out.println(message);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }

    private double distance(double[] d1, double[] d2) {
        double distance = 0;
        for (int i = 0; i < d1.length; i++) {
            distance += Math.pow(d1[i] - d2[i], 2);
        }

        return distance;
    }
}
