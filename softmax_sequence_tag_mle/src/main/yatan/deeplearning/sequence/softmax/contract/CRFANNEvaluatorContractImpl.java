package yatan.deeplearning.sequence.softmax.contract;

import java.util.List;

import com.google.inject.Inject;

import yatan.ann.AnnModel;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class CRFANNEvaluatorContractImpl extends AbstractComputeActorContractImpl {
    @Inject
    private WordEmbedding wordEmbedding;

    @Override
    protected int requestDataSize() {
        return 20;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        // TODO Auto-generated method stub
        return null;
    }

    public static double[] tag(WordEmbedding wordEmbedding, AnnModel annModel, double[][] tagTransitionWeights,
            List<WordEmbeddingTrainingInstance> instances, int tagCount) {
        double[][][] sums = new double[instances.size()][][];
        double[][][] outputs = new double[instances.size()][][];
        double[][] annInputs = new double[instances.size()][];

        double[][] f =
                CRFANNTrainingContractImpl.computeF(wordEmbedding, annModel, instances, annInputs, sums, outputs);

        double[][] delta = new double[f.length][f[0].length];
        double[][] tagTrace = new double[f.length][f[0].length];
        for (int j = 0; j < delta[0].length; j++) {
            delta[0][j] = f[0][j] + Math.log(Math.exp(tagTransitionWeights[tagCount][j]));
        }
        for (int t = 1; t < delta.length; t++) {
                for (int k = 0; k < delta[t].length; k++) {
                    double maxPotential = -Integer.MAX_VALUE;
                    for (int i = 0; i < tagCount; i++) {
                        double potential = Math.exp(delta[t - 1][i] + tagTransitionWeights[i][k]);
                        if (potential > maxPotential) {
                            maxPotential = potential;
                            delta[t][k] = potential;
                            tagTrace[t][k] = i;
                        }
                    }
                    delta[t][k] = f[t][k] + Math.log(sumDeltaTMinusOnePlusAik);
                }
            }
        }

        // sum over the last delta
        double result = 0;
        for (int i = 0; i < delta[0].length; i++) {
            result += Math.exp(delta[delta.length - 1][i]);
        }

        return Math.log(result);
    }
}
