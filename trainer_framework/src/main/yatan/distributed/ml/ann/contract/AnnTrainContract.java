package yatan.distributed.ml.ann.contract;

import java.io.Serializable;
import java.util.List;

import com.google.inject.Inject;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.distributed.ml.ann.AnnTrainerConfiguration;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AnnTrainContract extends AbstractComputeActorContractImpl {
    private static final int MINIBATCH_SIZE = 100;

    @Inject(optional = false)
    private AnnTrainerConfiguration configuration;

    private final AnnTrainer trainer = new AnnTrainer();

    @Override
    protected int requestDataSize() {
        return MINIBATCH_SIZE;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[0];

        AnnGradient batchGradient = null;
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            AnnData annData = (AnnData) data.getSerializable();
            double[][] output = trainer.run(annModel, annData.getInput(), sum);
            AnnGradient newGradient = computeGradient(annModel, annData, output, sum);

            batchGradient = saveGradient(batchGradient, newGradient);
        }

        // average batch gradient
        batchGradient.averageBy(MINIBATCH_SIZE);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(new Serializable[] {batchGradient});
        result.setGradient(gradientWrapper);

        return result;
    }

    protected AnnGradient computeGradient(AnnModel annModel, AnnData annData, double[][] output, double[][] sum) {
        switch (this.configuration.lossFunction) {
        case LeastSquare:
            return trainer.backpropagateLeastSqure(annModel, annData, output, sum);
        case SoftmaxLoglikelyhood:
            return trainer.backpropagateSoftmaxLogLikelyhood(annModel, annData, output, sum, null, null);
        default:
            throw new IllegalStateException("Unknown loss function: " + this.configuration.lossFunction);
        }
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
