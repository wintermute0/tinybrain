package yatan.distributed;

import java.util.ArrayList;

import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class ComputeActorContractEvaluatorImpl extends AbstractComputeActorContractImpl {
    protected int requestDataSize() {
        return 1500;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        List<AnnData> annDataset = new ArrayList<AnnData>();
        for (Data data : dataset) {
            annDataset.add((AnnData) data.getSerializable());
        }

        double precision = evaluate((DefaultAnnModel) parameter.getSerializable(), annDataset);

        getLogger().info("Precision: " + precision);

        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(1);
        result.setGradient(null);

        return result;
    }

    private static double evaluate(DefaultAnnModel model, List<AnnData> testSet) {
        AnnTrainer trainer = new AnnTrainer();
        int accurate = 0;
        for (AnnData data : testSet) {
            double[][] output = trainer.run(model, data.getInput(), new double[model.getLayerCount()][]);
            if (Math.round(output[model.getLayerCount() - 1][0]) == data.getOutput()[0]) {
                accurate++;
            }
        }

        return 1.0 * accurate / testSet.size();
    }
}
