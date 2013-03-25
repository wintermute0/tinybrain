package yatan.distributed.akka.message;

import java.util.concurrent.atomic.AtomicLong;

public class BaseMessage {
    private static final AtomicLong MESSAGE_ID_COUNTER = new AtomicLong(1);
    private long id;

    public BaseMessage() {
        // FIXME: this id is only unique within the local JVM, but that's good enough for now
        // FIXME: during desrialization this constructor will also get called, that's also fine for now
        this.id = MESSAGE_ID_COUNTER.getAndAdd(1);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
