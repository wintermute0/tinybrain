package yatan.deeplearning.softmax.contract.compute;

import java.io.Serializable;

import java.util.Date;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.DropoutAnnModel;
import yatan.deeplearning.softmax.TrainerConfiguration;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class SoftmaxClassificationEvaluatorContractImpl extends AbstractComputeActorContractImpl {
    public static final int REQUEST_DATA_SIZE = 90000;
    // public static final int REQUEST_DATA_SIZE = 5000;

    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;
    private int count;

    @Inject
    @Named("training_data_evaluator")
    private boolean trainingDataEvaluator;

    @Inject(optional = false)
    private TrainerConfiguration trainerConfiguration;

    @Override
    protected int requestDataSize() {
        return REQUEST_DATA_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        long startTime = new Date().getTime();
        Serializable[] parameters = (Serializable[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) parameters[0];
        AnnModel originalAnnModel = (DefaultAnnModel) parameters[1];

        int accurateCount = 0;

        AnnTrainer trainer = new AnnTrainer();
        // use dropout ann model if necessary
        AnnModel annModel = originalAnnModel;
        if (this.trainerConfiguration.dropout) {
            annModel = new DropoutAnnModel(originalAnnModel, false);
        }

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

        if (!this.trainingDataEvaluator) {
            LogUtility.logWordEmbedding(getLogger(), wordEmbedding, "吴");
            LogUtility.logWordEmbedding(getLogger(), wordEmbedding, "的");
            LogUtility.logWordEmbedding(getLogger(), wordEmbedding);
            LogUtility.logAnnModel(getLogger(), (DefaultAnnModel) parameters[1]);
            // logAnnModel(annModel);
            getLogger().info("Precision: " + 100.0 * accurateCount / dataset.size() + "%");
            getLogger().info("Evaluating cost " + (new Date().getTime() - startTime) / 1000.0 + "s");
            System.out.println(++count + ": " + 100.0 * accurateCount / dataset.size() + "%");
        } else {
            getLogger().info("Training Precision: " + 100.0 * accurateCount / dataset.size() + "%");
            getLogger().info("Training evaluating cost " + (new Date().getTime() - startTime) / 1000.0 + "s");
            System.out.println(++count + " training: " + 100.0 * accurateCount / dataset.size() + "%");
        }

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }
}
