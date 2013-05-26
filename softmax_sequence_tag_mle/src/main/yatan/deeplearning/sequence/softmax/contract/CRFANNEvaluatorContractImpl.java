package yatan.deeplearning.sequence.softmax.contract;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnModel;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class CRFANNEvaluatorContractImpl extends AbstractComputeActorContractImpl {
    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Inject
    @Named("tag_count")
    private int tagCount;
    @Inject
    @Named("training_data_evaluator")
    private boolean training;

    @Override
    protected int requestDataSize() {
        return 1000;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        Object[] serializables = (Object[]) parameter.getSerializable();
        WordEmbedding wordEmbedding = (WordEmbedding) serializables[0];
        AnnModel annModel = (AnnModel) serializables[1];
        double[][] tagTransitionWeights = (double[][]) serializables[2];

        int accurateTagCount = 0;
        int totalTagCount = 0;
        for (Data data : dataset) {
            @SuppressWarnings("unchecked")
            List<WordEmbeddingTrainingInstance> instances =
                    (List<WordEmbeddingTrainingInstance>) data.getSerializable();
            int[] tags = tag(wordEmbedding, annModel, tagTransitionWeights, instances, tagCount);
            for (int i = 0; i < instances.size(); i++) {
                if (tags[i] == instances.get(i).getOutput()) {
                    accurateTagCount++;
                }
            }
            totalTagCount += instances.size();
        }

        LogUtility.logWordEmbedding(getLogger(), wordEmbedding);
        LogUtility.logAnnModel(getLogger(), annModel);
        getLogger().info("Tag transition weights: " + LogUtility.buildLogString(tagTransitionWeights));
        System.out.println("Tag precision" + (this.training ? "(training)" : "") + " = " + 100.0 * accurateTagCount
                / totalTagCount + "%.");

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }

    public static int[] tag(WordEmbedding wordEmbedding, AnnModel annModel, double[][] tagTransitionWeights,
            List<WordEmbeddingTrainingInstance> instances, int tagCount) {
        double[][][] sums = new double[instances.size()][][];
        double[][][] outputs = new double[instances.size()][][];
        double[][] annInputs = new double[instances.size()][];

        double[][] f =
                CRFANNTrainingContractImpl.computeF(wordEmbedding, annModel, instances, annInputs, sums, outputs);

        double[][] delta = new double[f.length][f[0].length];
        int[][] tagTrace = new int[f.length][f[0].length];
        for (int j = 0; j < delta[0].length; j++) {
            delta[0][j] = f[0][j] + tagTransitionWeights[tagCount][j];
        }
        for (int t = 1; t < delta.length; t++) {
            for (int k = 0; k < tagCount; k++) {
                double maxPotential = -Integer.MAX_VALUE;
                for (int i = 0; i < tagCount; i++) {
                    double potential = delta[t - 1][i] + tagTransitionWeights[i][k];
                    if (potential > maxPotential) {
                        maxPotential = potential;
                        delta[t][k] = potential;
                        tagTrace[t][k] = i;
                    }
                }
                delta[t][k] += f[t][k];
            }
        }

        // sum over the last delta
        double maxPotential = -Integer.MAX_VALUE;
        int[] tags = new int[f.length];
        for (int i = 0; i < tagCount; i++) {
            double potential = delta[delta.length - 1][i] + tagTransitionWeights[i][tagCount + 1];
            if (potential > maxPotential) {
                maxPotential = potential;
                tags[tags.length - 1] = i;
            }
        }

        // back trace the tag
        for (int i = tags.length - 2; i >= 0; i--) {
            tags[i] = tagTrace[i + 1][tags[i + 1]];
        }

        return tags;
    }
}
