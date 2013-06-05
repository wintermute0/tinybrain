package yatan.deeplearning.softmax.data.producer;

public class ProgressReporter {
    private static final ProgressReporter PROGRESS_REPORTER = new ProgressReporter();

    private int epoch;
    private double percentage;

    public static ProgressReporter instance() {
        return PROGRESS_REPORTER;
    }

    public synchronized void report(int epoch, double percentage) {
        this.epoch = epoch;
        this.percentage = percentage;
    }

    public synchronized int getEpoch() {
        return epoch;
    }

    public synchronized double getPercentage() {
        return percentage;
    }
}
