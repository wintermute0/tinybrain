package yatan.distributed.akka;

import org.apache.log4j.Logger;

import yatan.common.CheckUtility;

public abstract class BaseActorContract implements ActorContract {
    private final Logger logger = Logger.getLogger(getClass());
    private BaseActor<?> actor;

    @Override
    public void setActor(BaseActor<?> actor) {
        this.actor = actor;
    }

    @Override
    public BaseActor<?> getActor() {
        return this.actor;
    }

    @Override
    public void tellSender(Object message) {
        CheckUtility.checkState(this.actor, "actor");

        this.actor.getSender().tell(message);
    }

    @Override
    public void preStart() {
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
