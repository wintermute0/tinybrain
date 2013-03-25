package yatan.distributedcomputer;

public class AuditEntry {
    private long timeCost;
    private int processedInstanceCount;

    public long getTimeCost() {
        return timeCost;
    }

    public void setTimeCost(long timeCost) {
        this.timeCost = timeCost;
    }

    public int getProcessedInstanceCount() {
        return processedInstanceCount;
    }

    public void setProcessedInstanceCount(int processedInstanceCount) {
        this.processedInstanceCount = processedInstanceCount;
    }
}
