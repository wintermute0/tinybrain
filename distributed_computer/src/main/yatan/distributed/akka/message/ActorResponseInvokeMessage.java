package yatan.distributed.akka.message;

public class ActorResponseInvokeMessage extends ActorInvokeMessage {
    private final long originalMessageId;

    public ActorResponseInvokeMessage(BaseMessage originalMessage) {
        this.originalMessageId = originalMessage.getId();
    }

    public long getOriginalMessageId() {
        return originalMessageId;
    }
}
