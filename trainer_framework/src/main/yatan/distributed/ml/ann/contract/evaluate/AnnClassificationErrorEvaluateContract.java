package yatan.distributed.ml.ann.contract.evaluate;

import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.deeplearning.wordembedding.utility.LogUtility;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AnnClassificationErrorEvaluateContract extends AbstractComputeActorContractImpl {
    private static final int EVALUATION_INTERVAL_IN_SECONDS = 60;

    @Override
    protected int requestDataSize() {
        return 1000;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[0];

        int accurate = 0;
        double totalLoss = 0;

        double activationFunctionCenterValue = 0.5;
        if (annModel.getConfiguration().activationFunctionOfLayer(annModel.getLayerCount() - 1) == ActivationFunction.TANH) {
            activationFunctionCenterValue = 0;
        }

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            double loss = 0;
            boolean error = false;
            AnnData annData = (AnnData) data.getSerializable();
            double[][] output = trainer.run(annModel, annData.getInput(), sum);
            for (int i = 0; i < output[output.length - 1].length; i++) {
                double expected = annData.getOutput()[i];
                double actual = output[output.length - 1][i];
                loss += Math.abs(expected - actual);
                if ((expected - activationFunctionCenterValue) * (actual - activationFunctionCenterValue) < 0) {
                    error = true;
                    break;
                }
            }

            if (!error) {
                accurate++;
            }

            totalLoss += loss / output[output.length - 1].length;
        }

        LogUtility.logAnnModel(getLogger(), annModel);
        System.out.println("Loss = " + (totalLoss / dataset.size()));

        String message = "Classification precision: " + 100.0 * accurate / dataset.size() + "%";
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
