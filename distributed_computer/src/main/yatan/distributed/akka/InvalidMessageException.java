package yatan.distributed.akka;

import yatan.distributed.DistributedComputerException;

@SuppressWarnings("serial")
public class InvalidMessageException extends DistributedComputerException {
    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
