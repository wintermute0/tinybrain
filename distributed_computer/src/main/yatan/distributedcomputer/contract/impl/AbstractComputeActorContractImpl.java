package yatan.distributedcomputer.contract.impl;

import java.util.Date;

import java.util.Random;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.inject.name.Named;

import com.google.inject.Inject;

import akka.dispatch.Future;
import akka.dispatch.OnFailure;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;

import yatan.common.LogUtility;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributed.akka.message.ActorResponseInvokeMessage;
import yatan.distributedcomputer.AuditEntry;
import yatan.distributedcomputer.ComputeInstruction;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.actors.AuditActor;
import yatan.distributedcomputer.actors.ComputeActor;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.actors.data.DataActor;
import yatan.distributedcomputer.contract.ComputeActorContract;

public abstract class AbstractComputeActorContractImpl extends BaseActorContract implements ComputeActorContract {
    private static final int RETRY_COMPUTE_DELAY_SECONDS = 10;

    private static final Timeout REQUEST_TIMEOUT = new Timeout(Duration.create(60 * 10, TimeUnit.SECONDS));

    public static final String PARAMETER_ACTOR_PATH = "/user/parameter";
    public static final String AUDIT_ACTOR_PATH = "/user/audit";

    private final String CLAZZ = getClass().getName();

    @Inject
    @Named("data_actor_path")
    private String dataActorPath = "/user/data";

    private State state = State.READY;

    private List<Data> data;
    private Parameter parameter;

    private long dataRequestMessageId;
    private long parameterRequestMessageId;

    public static enum State {
        READY, WAIT_FOR_DATA_AND_PARAMETERS, COMPUTING
    }

    @Override
    public void preStart() {
        Random random = new Random(new Date().getTime());
        int bootstrapDelay = 1 + random.nextInt(5);
        getLogger().info(
                "Bootstrap message for compute actor " + this + " is scheduled to be sent in " + bootstrapDelay
                        + " seconds.");
        getActor().getContext().system().scheduler()
                .scheduleOnce(Duration.create(bootstrapDelay, TimeUnit.SECONDS), new Runnable() {
                    @Override
                    public void run() {
                        getActor().getSelf().tell(new ComputeActor.ComputeMessage(new ComputeInstruction()));
                        getLogger().info("Bootstrap message for compute actor " + this + " has been sent.");
                        getLogger().info("The data actor path of this compute actor is " + dataActorPath);
                    }
                });
    }

    @Override
    public void compute(final ComputeInstruction computeInstruction) {
        long startTime =
                LogUtility.logMethodEntrance(getLogger(), CLAZZ, "compute", new String[] {"computeInstruction"},
                        computeInstruction);

        // clear internal state
        reset();

        // request the parameters
        ActorInvokeMessage message = new ParameterActor.RequestParameterMessage(null, null);
        this.parameterRequestMessageId = message.getId();
        Future<Object> future =
                Patterns.ask(getActor().getContext().actorFor(PARAMETER_ACTOR_PATH), message, REQUEST_TIMEOUT);
        future.onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable e) throws Throwable {
                // if parameter request failed, wait 10 seconds and try again
                reset();
                getLogger().warn(
                        "Failed to retrieve parameters. Try to execute compute message " + computeInstruction
                                + " later.", e);
                getActor().getContext().system().scheduler()
                        .scheduleOnce(Duration.create(RETRY_COMPUTE_DELAY_SECONDS, TimeUnit.SECONDS), new Runnable() {
                            @Override
                            public void run() {
                                getLogger().info("Try to execute compute message " + computeInstruction + " again.");
                                getActor().getSelf().tell(new ComputeActor.ComputeMessage(computeInstruction));
                            }
                        });
            }
        });
        Patterns.pipe(future).to(getActor().getSelf());

        // request some data
        message = new DataActor.RequestDataMessage(requestDataSize());
        this.dataRequestMessageId = message.getId();
        future = Patterns.ask(getActor().getContext().actorFor(dataActorPath), message, REQUEST_TIMEOUT);
        future.onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable e) throws Throwable {
                // if data request failed, wait 10 seconds and try again
                reset();
                getLogger().warn(
                        "Failed to retrieve data. Try to execute compute message " + computeInstruction + " later.", e);
                getActor().getContext().system().scheduler()
                        .scheduleOnce(Duration.create(RETRY_COMPUTE_DELAY_SECONDS, TimeUnit.SECONDS), new Runnable() {
                            @Override
                            public void run() {
                                getLogger().info("Try to execute compute message " + computeInstruction + " again.");
                                getActor().getSelf().tell(new ComputeActor.ComputeMessage(computeInstruction));
                            }
                        });
            }
        });
        Patterns.pipe(future).to(getActor().getSelf());

        // change state to wait for data and parameters
        this.state = State.WAIT_FOR_DATA_AND_PARAMETERS;

        LogUtility.logMethodExit(getLogger(), CLAZZ, "compute", startTime, false, null);
    }

    @Override
    public void abort() {
        long startTime = LogUtility.logMethodEntrance(getLogger(), CLAZZ, "abort", null);

        reset();

        LogUtility.logMethodExit(getLogger(), CLAZZ, "abort", startTime, false, null);
    }

    @Override
    public void receiveData(List<Data> data) {
        long startTime =
                LogUtility
                        .logMethodEntrance(getLogger(), CLAZZ, "receiveData", new String[] {"data.size"}, data.size());

        if (this.state != State.WAIT_FOR_DATA_AND_PARAMETERS) {
            getLogger().debug("Recieved some data, but we're not waiting for it. Simply ingore.");
            return;
        }
        if (((ActorResponseInvokeMessage) getActor().getMessage()).getOriginalMessageId() != this.dataRequestMessageId) {
            getLogger().debug("Recieved some data, but it's not for the lasted request. Simply ignore.");
            return;
        }

        this.data = data;

        computeIfReady();

        LogUtility.logMethodExit(getLogger(), CLAZZ, "receiveData", startTime, false, null);
    }

    @Override
    public void receiveParameter(Parameter parameter) {
        long startTime =
                LogUtility.logMethodEntrance(getLogger(), CLAZZ, "receiveParameters", new String[] {"parameter"},
                        parameter);

        if (this.state != State.WAIT_FOR_DATA_AND_PARAMETERS) {
            getLogger().debug("Recieved some parameters, but we're not waiting for it. Simply ingore.");
            return;
        }
        if (((ActorResponseInvokeMessage) getActor().getMessage()).getOriginalMessageId() != this.parameterRequestMessageId) {
            getLogger().debug("Recieved some parameters, but it's not for the lasted request. Simply ignore.");
            return;
        }

        this.parameter = parameter;

        computeIfReady();

        LogUtility.logMethodExit(getLogger(), CLAZZ, "receiveParameters", startTime, false, null);
    }

    private void reset() {
        this.state = State.READY;
        this.data = null;
        this.parameter = null;
        this.dataRequestMessageId = 0;
        this.parameterRequestMessageId = 0;
    }

    private void computeIfReady() {
        if (this.data != null && this.parameter != null) {
            long startTime = new Date().getTime();

            // change state to computing
            this.state = State.COMPUTING;

            // do actual computing
            ComputeResult result = doCompute(this.data, this.parameter);

            // update parameter if necessary
            if (result.getGradient() != null) {
                getActor().getContext().actorFor(PARAMETER_ACTOR_PATH)
                        .tell(new ParameterActor.UpdateGradientMessage(result.getGradient()));
            }

            // audit
            if (result.isAudit()) {
                AuditEntry auditEntry = new AuditEntry();
                auditEntry.setProcessedInstanceCount(this.data.size());
                auditEntry.setTimeCost(new Date().getTime() - startTime);
                getActor().getContext().actorFor(AUDIT_ACTOR_PATH).tell(new AuditActor.AuditMessage(auditEntry));
            }

            // after actual computing is completed, reset
            reset();

            // tell myself to compute again is necessary
            if (result.isRepeat()) {
                if (result.getRepeatDelayInSeconds() == 0) {
                    getActor().getSelf().tell(new ComputeActor.ComputeMessage(new ComputeInstruction()));
                } else {
                    getActor()
                            .getContext()
                            .system()
                            .scheduler()
                            .scheduleOnce(Duration.create(result.getRepeatDelayInSeconds(), TimeUnit.SECONDS),
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            getActor().getSelf().tell(
                                                    new ComputeActor.ComputeMessage(new ComputeInstruction()));
                                        }
                                    });
                }
            }
        }
    }

    public static class ComputeResult {
        private boolean repeat;
        private int repeatDelayInSeconds;
        private Parameter gradient;
        private boolean audit = true;

        public boolean isRepeat() {
            return repeat;
        }

        public void setRepeat(boolean repeat) {
            this.repeat = repeat;
        }

        public Parameter getGradient() {
            return gradient;
        }

        public void setGradient(Parameter gradient) {
            this.gradient = gradient;
        }

        public int getRepeatDelayInSeconds() {
            return repeatDelayInSeconds;
        }

        public void setRepeatDelayInSeconds(int repeatDelayInSeconds) {
            this.repeatDelayInSeconds = repeatDelayInSeconds;
        }

        public boolean isAudit() {
            return audit;
        }

        public void setAudit(boolean audit) {
            this.audit = audit;
        }
    }

    protected abstract int requestDataSize();

    protected abstract ComputeResult doCompute(List<Data> dataset, Parameter parameter);
}
