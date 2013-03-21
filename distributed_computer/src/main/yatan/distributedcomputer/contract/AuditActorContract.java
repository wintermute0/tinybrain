package yatan.distributedcomputer.contract;

import com.google.inject.ImplementedBy;

import yatan.distributed.akka.ActorContract;
import yatan.distributedcomputer.AuditEntry;
import yatan.distributedcomputer.contract.impl.DefaultAuditActorContract;

@ImplementedBy(DefaultAuditActorContract.class)
public interface AuditActorContract extends ActorContract {
    public void audit(AuditEntry entry);
}
