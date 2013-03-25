package yatan.distributed.akka;

import yatan.distributed.DistributedComputerException;

@SuppressWarnings("serial")
public class ActorInvocationException extends DistributedComputerException {
    public ActorInvocationException(String message) {
        super(message);
    }

    public ActorInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
