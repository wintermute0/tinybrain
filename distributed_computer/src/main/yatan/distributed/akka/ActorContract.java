package yatan.distributed.akka;

public interface ActorContract {
    public void setActor(BaseActor<?> actor);

    public BaseActor<?> getActor();

    public void tellSender(Object message);

    public void preStart();
}
