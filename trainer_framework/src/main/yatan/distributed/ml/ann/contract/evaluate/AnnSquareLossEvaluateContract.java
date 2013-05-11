package yatan.distributed.ml.ann.contract.evaluate;

import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AnnSquareLossEvaluateContract extends AbstractComputeActorContractImpl {

    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Override
    protected int requestDataSize() {
        return 10000;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[0];

        double totalError = 0;

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            AnnData annData = (AnnData) data.getSerializable();
            double[][] output = trainer.run(annModel, annData.getInput(), sum);
            double squreError = 0;
            for (int i = 0; i < output[output.length - 1].length; i++) {
                squreError += Math.pow(output[output.length - 1][i] - annData.getOutput()[i], 2);
            }
            totalError += Math.sqrt(squreError / output[output.length - 1].length);
        }

        String message = "Average error: " + totalError / dataset.size();
        getLogger().info(message);
        System.out.println(message);

        // LogUtility.logAnnModel(getLogger(), annModel);

        // return computation result
        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        result.setRepeatDelayInSeconds(EVALUATION_INTERVAL_IN_SECONDS);
        result.setGradient(null);
        result.setAudit(false);

        return result;
    }
}
