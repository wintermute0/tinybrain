package yatan.distributed;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class ComputeActorContractTrainingImpl extends AbstractComputeActorContractImpl {
    private static final int MINIBATCH_SIZE = 20;
    private static AtomicInteger TOTAL = new AtomicInteger();

    protected int requestDataSize() {
        return 200;
    }

    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        getLogger().info("Doing some actual computing...");

        AnnGradient gradient = null;

        AnnTrainer trainer = new AnnTrainer();
        List<AnnData> batch = new ArrayList<AnnData>();
        for (Data data : dataset) {
            batch.add((AnnData) data.getSerializable());
            if (batch.size() == MINIBATCH_SIZE) {
                AnnGradient newGradient = trainer.trainWithMiniBatch((DefaultAnnModel) parameter.getSerializable(), batch);
                if (gradient == null) {
                    gradient = newGradient;
                } else {
                    gradient.updateByPlus(newGradient);
                }
                batch.clear();
            }
        }

        if (TOTAL.addAndGet(dataset.size()) % (50 * dataset.size()) == 0) {
            // getLogger().info("Total: " + TOTAL.get());
        }

        ComputeResult result = new ComputeResult();
        result.setRepeat(true);
        Parameter gradientWrapper = new Parameter();
        gradientWrapper.setSerializable(gradient);
        result.setGradient(gradientWrapper);

        return result;
    }
}
