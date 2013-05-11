package yatan.deeplearning.autoencoder.contract;

import java.util.List;

import scala.actors.threadpool.Arrays;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.AnnTrainer.OutputPostProcessor;
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

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            AnnData annData = (AnnData) Helper.convertToAnnData(wordEmbedding, instance);

            // setup corruption post processor to corrupt the input
            OutputPostProcessor postProcessor = null;
            if (annModel.getLayerCount() == 2) {
                // corrupt input
                Helper.corrupt(annData.getInput());
            } else {
                postProcessor = new OutputPostProcessor() {
                    private double[] uncorruptedData;

                    @Override
                    public void process(double[] output) {
                        this.uncorruptedData = Arrays.copyOf(output, output.length);

                        Helper.corrupt(output);
                    }

                    @Override
                    public int layer() {
                        return annModel.getLayerCount() - 3;
                    }

                    @Override
                    public double[] getCleanData() {
                        return uncorruptedData;
                    }
                };
            }
            // calculate ANN output
            double[][] output = trainer.run(annModel, annData.getInput(), sum, postProcessor);
            // set up ann data output to the clean input
            if (postProcessor != null) {
                annData = new AnnData(annData.getInput(), postProcessor.getCleanData());
            }

            double squreError = 0;
            for (int i = 0; i < output[output.length - 1].length; i++) {
                squreError += Math.pow(output[output.length - 1][i] - annData.getOutput()[i], 2);
            }
            totalError += Math.sqrt(squreError / output[output.length - 1].length);
        }

        String message = "Average error: " + totalError / dataset.size();
        getLogger().info(message);
        System.out.println(message);

        LogUtility.logAnnModel(getLogger(), annModel);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }
}
