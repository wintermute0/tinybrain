package yatan.deeplearning.softmax.contract;

import java.io.Serializable;

import java.util.Date;
import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class SoftmaxClassificationEvaluatorContractImpl extends AbstractComputeActorContractImpl {
    public static final int REQUEST_DATA_SIZE = 10000;
    // public static final int REQUEST_DATA_SIZE = 5000;

    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;
    private int count;

    @Override
    protected int requestDataSize() {
        return REQUEST_DATA_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        long startTime = new Date().getTime();
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        AnnModel annModel = (AnnModel) parameters[1];

        int accurateCount = 0;

        AnnTrainer trainer = new AnnTrainer();
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();

            // first convert input data into word embedding
            // FIXME: could reuse an array, no need to allocate it every time
            double[] input = new double[instance.getInput().size() * wordEmbedding.getWordVectorSize()];
            for (int i = 0; i < instance.getInput().size(); i++) {
                int index = instance.getInput().get(i);
                for (int j = 0; j < wordEmbedding.getWordVectorSize(); j++) {
                    input[i * wordEmbedding.getWordVectorSize() + j] = wordEmbedding.getMatrix().getData()[j][index];
                }
            }

            AnnData annData = new AnnData(input, new double[] {instance.getOutput()});

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), new double[annModel.getLayerCount()][]);
            // System.out.println(output[annModel.getLayerCount() - 1][0]);
            boolean accurate = true;
            // System.out.println(instance.getOutput() + ": " + Arrays.toString(output[annModel.getLayerCount() - 1]));
            for (int i = 0; i < output[annModel.getLayerCount() - 1].length; i++) {
                if (i == instance.getOutput()) {
                    continue;
                }

                double tagProbability = output[annModel.getLayerCount() - 1][i];
                if (tagProbability > output[annModel.getLayerCount() - 1][instance.getOutput()]) {
                    accurate = false;
                    break;
                }
            }

            if (accurate) {
                accurateCount++;
            }
        }

        logWordEmbedding(wordEmbedding, "吴");
        logWordEmbedding(wordEmbedding, "的");
        getLogger().info("Precision: " + 100.0 * accurateCount / dataset.size() + "%");
        getLogger().info("Evaluating cost " + (new Date().getTime() - startTime) / 1000.0 + "s");
        System.out.println(++count + ": " + 100.0 * accurateCount / dataset.size() + "%");

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);

        return result;
    }

    private void logWordEmbedding(WordEmbedding wordEmbedding, String word) {
        StringBuilder sb = new StringBuilder();
        int index = wordEmbedding.indexOf(word);
        for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
            sb.append(wordEmbedding.getMatrix().getData()[i][index]).append(" ");
        }
        getLogger().info(sb.toString());
    }
}
