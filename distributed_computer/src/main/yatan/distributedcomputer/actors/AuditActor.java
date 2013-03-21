package yatan.distributedcomputer.actors;

import javax.inject.Inject;

import yatan.distributed.akka.BaseActor;
import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributedcomputer.AuditEntry;
import yatan.distributedcomputer.contract.AuditActorContract;

public class AuditActor extends BaseActor<AuditActorContract> {
    @Inject
    AuditActor(AuditActorContract impl) {
        super(impl);
    }

    public static class AuditMessage extends ActorInvokeMessage {
        public AuditMessage(AuditEntry entry) {
            setCommand("audit");
            setArguments(entry);
        }
    }
}
