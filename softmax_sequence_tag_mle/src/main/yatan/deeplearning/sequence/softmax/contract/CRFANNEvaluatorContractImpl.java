package yatan.deeplearning.sequence.softmax.contract;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnModel;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class CRFANNEvaluatorContractImpl extends AbstractComputeActorContractImpl {
    private static final int TAG_B = WordSegmentationInstancePool.TAGS.indexOf("B");
    private static final int TAG_I = WordSegmentationInstancePool.TAGS.indexOf("I");
    private static final int TAG_L = WordSegmentationInstancePool.TAGS.indexOf("L");
    private static final int TAG_U = WordSegmentationInstancePool.TAGS.indexOf("U");

    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Inject
    @Named("tag_count")
    private int tagCount;
    @Inject
    @Named("training_data_evaluator")
    private boolean training_data;

    @Override
    protected int requestDataSize() {
        return 2000;
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

        if (!this.training_data) {
            LogUtility.logWordEmbedding(getLogger(), wordEmbedding);
            LogUtility.logAnnModel(getLogger(), annModel);
            getLogger().info("Tag transition weights: " + LogUtility.buildLogString(tagTransitionWeights));
        }

        String message =
                "Tag precision" + (this.training_data ? "(training)" : "") + " = " + 100.0 * accurateTagCount
                        / totalTagCount + "%.";
        // getLogger().info(message);
        System.out.println(message);

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
            if (j == TAG_B || j == TAG_U) {
                delta[0][j] = f[0][j] + tagTransitionWeights[tagCount][j];
            } else {
                delta[0][j] = Integer.MIN_VALUE;
            }
        }
        for (int t = 1; t < delta.length; t++) {
            for (int k = 0; k < tagCount; k++) {
                double maxPotential = -Integer.MAX_VALUE;
                for (int i = 0; i < tagCount; i++) {
                    if (isTagTransitionValid(i, k)) {
                        double potential = delta[t - 1][i] + tagTransitionWeights[i][k];
                        if (potential > maxPotential) {
                            maxPotential = potential;
                            delta[t][k] = potential;
                            tagTrace[t][k] = i;
                        }
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

    public static boolean isTagsValid(int[] tags) {
        if (tags[0] != TAG_B && tags[0] != TAG_U) {
            return false;
        }
        if (tags[tags.length - 1] != TAG_U && tags[tags.length - 1] != TAG_L) {
            return false;
        }

        for (int index = 1; index < tags.length; index++) {
            if (!isTagTransitionValid(tags[index - 1], tags[index])) {
                return false;
            }
        }

        return true;
    }

    public static boolean isTagTransitionValid(int from, int to) {
        if (from == TAG_B) {
            if (to != TAG_I && to != TAG_L) {
                return false;
            }
        } else if (from == TAG_I) {
            if (to != TAG_I && to != TAG_L) {
                return false;
            }
        } else if (from == TAG_L) {
            if (to != TAG_B && to != TAG_U) {
                return false;
            }
        } else if (from == TAG_U) {
            if (to != TAG_B && to != TAG_U) {
                return false;
            }
        }

        return true;
    }
}
