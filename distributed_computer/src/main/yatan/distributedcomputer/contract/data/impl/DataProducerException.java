package yatan.distributedcomputer.contract.data.impl;

@SuppressWarnings("serial")
public class DataProducerException extends Exception {
    public DataProducerException(String message) {
        super(message);
    }

    public DataProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
