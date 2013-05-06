package yatan.distributed;

import yatan.ann.AnnGradient;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.common.LogUtility;

import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class ParameterActorContractImpl extends BaseActorContract implements ParameterActorContract {
    private static final String CLAZZ = ParameterActorContractImpl.class.getName();

    private static final double LEARNING_RATE = 0.1;
    private static DefaultAnnModel ANN_MODEL;

    static {
        AnnConfiguration configuration = new AnnConfiguration(16);
        configuration.addLayer(10, ActivationFunction.SIGMOID);
        configuration.addLayer(1, ActivationFunction.SIGMOID);
        ANN_MODEL = new DefaultAnnModel(configuration);
    }

    public ParameterActorContractImpl() {
    }

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        long startTime =
                LogUtility.logMethodEntrance(getLogger(), CLAZZ, "requestParameters", new String[] {"start", "end"},
                        start, end);

        Parameter parameter = new Parameter();
        parameter.setSerializable(ANN_MODEL);

        tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(), parameter));

        LogUtility.logMethodExit(getLogger(), CLAZZ, "requestParameters", startTime, false, null);
    }

    @Override
    public void updateGradient(Parameter gradient) {
        long startTime =
                LogUtility.logMethodEntrance(getLogger(), CLAZZ, "updateGradient", new String[] {"gradient"}, gradient);

        getLogger().debug("Update parameters with gradient " + gradient);

        AnnGradient annGradient = (AnnGradient) gradient.getSerializable();
        ANN_MODEL.update(annGradient, LEARNING_RATE);

        LogUtility.logMethodExit(getLogger(), CLAZZ, "updateGradient", startTime, false, null);
    }
}
