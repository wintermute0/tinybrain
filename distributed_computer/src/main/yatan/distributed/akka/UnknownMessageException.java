package yatan.distributed.akka;

import yatan.distributed.DistributedComputerException;

@SuppressWarnings("serial")
public class UnknownMessageException extends DistributedComputerException {
    public UnknownMessageException(String message) {
        super(message);
    }

    public UnknownMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
