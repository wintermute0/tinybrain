package yatan.ml.sequence;

import java.io.Closeable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;

import yatan.deeplearning.trainer.Trainer;
import yatan.deeplearning.trainer.Trainer.TrainerProgress;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class TrainingSequenceExecutor {
    private static final String DEFAULT_MODEL_FILE_PREFIX = "training_sequence_";
    private static final String MODEL_FOLDER = "test_files/results/";

    private static final Logger LOGGER = Logger.getLogger(TrainingSequenceExecutor.class);

    private TrainingSequence trainingSequence;
    private TrainingSequenceProgress progress;

    public void start() {
        Preconditions.checkState(this.trainingSequence != null);

        this.progress = loadProgress(this.trainingSequence);
        LOGGER.info("Sequence progress is " + this.progress);

        int taskId = this.progress.getTaskId();
        while (taskId < this.trainingSequence.size()) {
            // get the current task
            Trainer task = this.trainingSequence.getTask(taskId);
            synchronized (task) {
                // start the task is not completed
                LOGGER.info("Start training task#" + taskId + ": " + task);
                task.start(this.progress.getTrainerProgress());

                // wait for the task to complete
                while (!task.isCompleted()) {
                    try {
                        task.wait(60 * 1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    this.progress.setTrainerProgress(task.progress());
                    saveProgress(this.progress);
                }

                task.stop();
                LOGGER.info("Task#" + taskId + " has been stopped.");
            }
            // move forward to the next task
            taskId++;
            // update task progress
            this.progress.setTaskId(taskId);
            this.progress.setTrainerProgress(new TrainerProgress());
        }
    }

    protected static TrainingSequenceProgress loadProgress(TrainingSequence trainingSequence) {
        LOGGER.info("Trying to find persisted training sequence progress for identifier '"
                + trainingSequence.identifier() + "'...");
        File stateFile = null;
        File modelFolderFile = new File(MODEL_FOLDER);
        if (modelFolderFile.isDirectory()) {
            for (File file : modelFolderFile.listFiles()) {
                if (file.isFile() && Files.getFileExtension(file.getName()).equals("seq")
                        && (stateFile == null || stateFile.getName().compareTo(file.getName()) < 0)) {
                    stateFile = file;
                }
            }
        }

        if (stateFile != null) {
            LOGGER.info("Loading training sequence progress from " + stateFile + "...");
            FileInputStream is = null;
            InputStreamReader reader = null;
            try {
                is = new FileInputStream(stateFile);
                reader = new InputStreamReader(is, Charsets.UTF_8);
                return new Gson().fromJson(reader, TrainingSequenceProgress.class);
            } catch (IOException e) {
                LOGGER.error("Error occurred while trying to load parameter server state: " + e.getMessage(), e);
            } finally {
                close(reader, is);
            }
        } else {
            LOGGER.info("Can't find any persisted training sequence progress. Let's start from strach.");
        }

        TrainingSequenceProgress progress = new TrainingSequenceProgress();
        progress.setTrainingSequenceIdentifier(trainingSequence.identifier());
        return progress;
    }

    protected static void saveProgress(TrainingSequenceProgress progress) {
        File stateFile =
                new File(MODEL_FOLDER + DEFAULT_MODEL_FILE_PREFIX + progress.getTrainingSequenceIdentifier() + ".seq");
        LOGGER.info("Saving training sequence progress to " + stateFile + "...");
        FileWriterWithEncoding writer = null;
        try {
            writer = new FileWriterWithEncoding(stateFile, Charsets.UTF_8);
            String json = new Gson().toJson(progress);
            writer.write(json);
        } catch (IOException e) {
            LOGGER.error("Error occurred while trying to save parameter server state: " + e.getMessage(), e);
        } finally {
            close(writer);
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public TrainingSequence getTrainingSequence() {
        return trainingSequence;
    }

    public void setTrainingSequence(TrainingSequence trainingSequence) {
        this.trainingSequence = trainingSequence;
        this.progress = new TrainingSequenceProgress();
    }

    @SuppressWarnings("serial")
    public static class TrainingSequenceProgress implements Serializable {
        private String trainingSequenceIdentifier;
        private int taskId;
        private TrainerProgress trainerProgress = new TrainerProgress();

        public int getTaskId() {
            return taskId;
        }

        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        public TrainerProgress getTrainerProgress() {
            return trainerProgress;
        }

        public void setTrainerProgress(TrainerProgress trainerProgress) {
            this.trainerProgress = trainerProgress;
        }

        public String getTrainingSequenceIdentifier() {
            return trainingSequenceIdentifier;
        }

        public void setTrainingSequenceIdentifier(String trainingSequenceIdentifier) {
            this.trainingSequenceIdentifier = trainingSequenceIdentifier;
        }

        @Override
        public String toString() {
            return "TrainingSequenceProgress [trainingSequenceIdentifier=" + trainingSequenceIdentifier + ", taskId="
                    + taskId + ", trainerProgress=" + trainerProgress + "]";
        }
    }
}
