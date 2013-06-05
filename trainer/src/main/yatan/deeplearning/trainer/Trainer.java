package yatan.deeplearning.trainer;

import java.io.Serializable;

public interface Trainer {
    public void start();

    public void start(TrainerProgress progress);

    public void stop();

    public TrainerProgress progress();

    public boolean isCompleted();

    @SuppressWarnings("serial")
    public static class TrainerProgress implements Comparable<TrainerProgress>, Serializable {
        private int epoch = 1;
        private double percentage;

        public int getEpoch() {
            return epoch;
        }

        public void setEpoch(int epoch) {
            this.epoch = epoch;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }

        @Override
        public int compareTo(TrainerProgress progress) {
            if (this.epoch == progress.getEpoch()) {
                return this.percentage - progress.getPercentage() > 0 ? 1 : -1;
            } else {
                return this.epoch - progress.getEpoch();
            }
        }
    }
}
