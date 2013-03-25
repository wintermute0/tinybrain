package yatan.distributed;

@SuppressWarnings("serial")
public class DistributedComputerException extends Exception {
    public DistributedComputerException(String message) {
        super(message);
    }

    public DistributedComputerException(String message, Throwable cause) {
        super(message, cause);
    }
}
