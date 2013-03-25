package yatan.distributedcomputer.contract.impl;

import java.util.Date;

import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.AuditEntry;
import yatan.distributedcomputer.contract.AuditActorContract;

public class DefaultAuditActorContract extends BaseActorContract implements AuditActorContract {
    private static final int reportIntervalSeconds = 10;

    private static final Date SYSTEM_START_TIME = new Date();

    private static long TOTAL_PROCESSED_INSTANCE_COUNT = 0;
    private static long TOTAL_TIME_COST = 0;

    private static Date LAST_REPORT_TIME = new Date();

    @Override
    public void audit(AuditEntry entry) {
        TOTAL_PROCESSED_INSTANCE_COUNT += entry.getProcessedInstanceCount();
        TOTAL_TIME_COST += entry.getTimeCost();

        if (new Date().getTime() - LAST_REPORT_TIME.getTime() > reportIntervalSeconds * 1000) {
            report();
            LAST_REPORT_TIME = new Date();
        }
    }

    private void report() {
        getLogger()
                .info("Processing "
                        + (TOTAL_PROCESSED_INSTANCE_COUNT / ((new Date().getTime() - SYSTEM_START_TIME.getTime()) / 1000.0))
                        + " instances per second. Average single thread speed is "
                        + (TOTAL_PROCESSED_INSTANCE_COUNT / (TOTAL_TIME_COST / 1000.0)) + " instance per second.");
    }
}
