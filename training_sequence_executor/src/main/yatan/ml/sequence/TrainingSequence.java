package yatan.ml.sequence;

import yatan.deeplearning.trainer.Trainer;

public interface TrainingSequence {
    public String identifier();

    public Trainer getTask(int id);

    public int size();
}
