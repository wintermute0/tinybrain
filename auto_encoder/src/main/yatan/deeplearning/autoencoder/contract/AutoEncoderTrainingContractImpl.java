package yatan.deeplearning.autoencoder.contract;

import java.io.Serializable;

import java.util.List;

import scala.actors.threadpool.Arrays;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.AnnTrainer.OutputPostProcessor;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AutoEncoderTrainingContractImpl extends AbstractComputeActorContractImpl {
    private static final int BATCH_SIZE = 100;

    @Override
    protected int requestDataSize() {
        return BATCH_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        WordEmbedding wordEmbedding = (WordEmbedding) ((Object[]) parameter.getSerializable())[0];
        final AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[1];

        AnnTrainer trainer = new AnnTrainer();
        AnnGradient batchGradient = null;
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            AnnData annData = (AnnData) Helper.convertToAnnData(wordEmbedding, instance);

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
            double[][] output = trainer.run(annModel, annData.getInput(), sum, postProcessor);

            if (postProcessor != null) {
                annData = new AnnData(annData.getInput(), postProcessor.getCleanData());
            }
            AnnGradient newGradient = trainer.backpropagateAutoEncoderLeastSqure(annModel, annData, output, sum);

            batchGradient = saveGradient(batchGradient, newGradient);
        }

        // average batch gradient
        batchGradient.averageBy(BATCH_SIZE);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(new Serializable[] {batchGradient});
        result.setGradient(gradientWrapper);

        return result;
    }

    private static AnnGradient saveGradient(AnnGradient gradient, AnnGradient newGradient) {
        if (gradient == null) {
            return newGradient.clone();
        } else {
            gradient.updateByPlus(newGradient);
            return gradient;
        }
    }
}
