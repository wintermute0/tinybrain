package yatan.deeplearning.autoencoder.contract;

import java.io.Serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

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

    private Random random = new Random(new Date().getTime());

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
        HashMap<Integer, Double[]> batchWordEmbeddingDelta = new HashMap<Integer, Double[]>();

        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            AnnData annData = (AnnData) Helper.convertToAnnData(wordEmbedding, instance);

            OutputPostProcessor postProcessor = null;
            if (annModel.getLayerCount() == 2) {
                // corrupt input
                Helper.corruptWithSaltAndPepper(annData.getInput());
            } else {
                postProcessor = new OutputPostProcessor() {
                    private double[] uncorruptedData;

                    @Override
                    public void process(double[] output) {
                        this.uncorruptedData = Arrays.copyOf(output, output.length);

                        Helper.corruptWithMask(output);
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
            AnnGradient newGradient = trainer.backpropagateAutoEncoderLeastSqure(annModel, annData, output, sum, false);

            batchGradient = saveGradient(batchGradient, newGradient);

            // save wordEmbeddingDelta if necessary
            if (annModel.getLayerCount() == 2) {
                // first save word embedding delta
                saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
                        instance);

                // // construct a negative example
                // java.util.Arrays.fill(annData.getOutput(), 0);
                //
                // // bp negative example
                // newGradient = trainer.backpropagateAutoEncoderLeastSqure(annModel, annData, output, sum, true);
                // batchGradient = saveGradient(batchGradient, newGradient);
                // saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
                // instance);
            }
        }

        // average batch gradient
        batchGradient.averageBy(BATCH_SIZE);

        // average word embedding gradient
        for (Double[] gradient : batchWordEmbeddingDelta.values()) {
            for (int i = 0; i < gradient.length; i++) {
                gradient[i] /= BATCH_SIZE;
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

    private static AnnGradient saveGradient(AnnGradient gradient, AnnGradient newGradient) {
        if (gradient == null) {
            return newGradient.clone();
        } else {
            gradient.updateByPlus(newGradient);
            return gradient;
        }
    }

    private static void saveWordEmbeddingDelta(AnnGradient newGradient, HashMap<Integer, Double[]> wordEmbeddingDelta,
            int wordVectorSize, WordEmbeddingTrainingInstance instance) {
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
