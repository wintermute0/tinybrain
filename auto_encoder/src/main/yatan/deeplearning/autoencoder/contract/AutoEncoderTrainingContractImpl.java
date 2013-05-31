package yatan.deeplearning.autoencoder.contract;

import java.io.Serializable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.AnnTrainer.LayerPostProcessor;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AutoEncoderTrainingContractImpl extends AbstractComputeActorContractImpl {
    private static final int BATCH_SIZE = 10;

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

            NoisingLayerPostProcessor postProcessor = null;
            if (annModel.getLayerCount() == 2) {
                // corrupt input
                Helper.corruptWithMask(annData.getInput());
            } else {
                postProcessor = new NoisingLayerPostProcessor(annModel.getLayerCount() - 3);
            }
            double[][] output = trainer.run(annModel, annData.getInput(), sum, postProcessor);

            if (postProcessor != null) {
                annData = new AnnData(annData.getInput(), postProcessor.getCleanData());
            }
            AnnGradient newGradient = trainer.backpropagateAutoEncoderLeastSqure(annModel, annData, output, sum);

            batchGradient = saveGradient(batchGradient, newGradient);

            // save wordEmbeddingDelta if necessary
            // if (annModel.getLayerCount() == 2) {
            // // remove masked input
            // double[] wordEmbeddingDelta = newGradient.getDeltaForInputLayer();
            // for (int i = 0; i < wordEmbeddingDelta.length; i++) {
            // if (mask[i]) {
            // wordEmbeddingDelta[i] = 0;
            // }
            // }
            //
            // first save word embedding delta
            // List<Integer> input = Lists.newArrayList(instance.getInput());
            // input.remove(input.size() / 2);
            // saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
            // input);

            // // construct a negative example
            // java.util.Arrays.fill(annData.getOutput(), 0);
            //
            // // bp negative example
            // newGradient = trainer.backpropagateAutoEncoderLeastSqure(annModel, annData, output, sum, true);
            // batchGradient = saveGradient(batchGradient, newGradient);
            // saveWordEmbeddingDelta(newGradient, batchWordEmbeddingDelta, wordEmbedding.getWordVectorSize(),
            // instance);
            // }
        }

        // average batch gradient
        batchGradient.averageBy(BATCH_SIZE);

        // average word embedding gradient
        // for (Double[] gradient : batchWordEmbeddingDelta.values()) {
        // for (int i = 0; i < gradient.length; i++) {
        // gradient[i] /= BATCH_SIZE;
        // }
        // }

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

    public static class NoisingLayerPostProcessor implements LayerPostProcessor {
        private final int targetLayer;
        private double[] uncorruptedData;

        public NoisingLayerPostProcessor(int targetLayer) {
            this.targetLayer = targetLayer;
        }

        @Override
        public void process(int layer, double[] output) {
            if (this.targetLayer == layer) {
                this.uncorruptedData = Arrays.copyOf(output, output.length);
                Helper.corruptWithMask(output);
            }
        }

        public double[] getCleanData() {
            return uncorruptedData;
        }
    }

    // private static void saveWordEmbeddingDelta(AnnGradient newGradient, HashMap<Integer, Double[]>
    // wordEmbeddingDelta,
    // int wordVectorSize, List<Integer> inputWordIndices) {
    // for (int i = 0; i < inputWordIndices.size(); i++) {
    // int index = inputWordIndices.get(i);
    // Double[] delta = wordEmbeddingDelta.get(index);
    // if (delta == null) {
    // delta = new Double[wordVectorSize];
    // wordEmbeddingDelta.put(index, delta);
    // for (int j = 0; j < wordVectorSize; j++) {
    // delta[j] = newGradient.getDeltaForInputLayer()[i * wordVectorSize + j];
    // }
    // } else {
    // for (int j = 0; j < wordVectorSize; j++) {
    // delta[j] += newGradient.getDeltaForInputLayer()[i * wordVectorSize + j];
    // }
    // }
    // }
    // }
}
