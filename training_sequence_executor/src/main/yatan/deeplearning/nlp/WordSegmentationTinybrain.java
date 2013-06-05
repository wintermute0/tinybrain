package yatan.deeplearning.nlp;

import java.util.List;

import com.google.common.collect.Lists;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.deeplearning.softmax.SoftmaxTrainer;
import yatan.deeplearning.softmax.WordEmbeddingTrainerConfiguration;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.trainer.Trainer;
import yatan.deeplearning.trainer.Trainer.TrainerProgress;
import yatan.deeplearning.wordembedding.WordEmbeddingTrainer;
import yatan.ml.sequence.TrainingSequence;
import yatan.ml.sequence.TrainingSequenceExecutor;

public class WordSegmentationTinybrain {
    private static final int WORD_EMBEDDING_HIDDEN_LAYER = 300;
    private static final int SOFTMAX_HIDDEN_LAYER = 400;

    public static void main(String[] args) {
        TrainingSequenceExecutor executor = new TrainingSequenceExecutor();
        executor.setTrainingSequence(new WordSegmentationTrainingSequence());
        executor.start();
    }

    public static class WordSegmentationTrainingSequence implements TrainingSequence {
        private static WordEmbeddingTrainerConfiguration createCommonConfiguration() {
            WordEmbeddingTrainerConfiguration configuration = new WordEmbeddingTrainerConfiguration();

            configuration.trainingActorCount = 16;
            configuration.parameterActorUpdateSlice = 8;

            configuration.wordVectorSize = 100;

            configuration.windowSize = 11;
            configuration.hiddenLayerSize = WORD_EMBEDDING_HIDDEN_LAYER;

            return configuration;
        }

        private static WordEmbeddingTrainer getWordEmbeddingTrainer1() {
            WordEmbeddingTrainerConfiguration configuration = createCommonConfiguration();

            configuration.modelFilePrefix = "a_word_embedding_";
            configuration.wordFrequencyRankLowerBound = 500;

            configuration.annConfiguration =
                    new AnnConfiguration(configuration.wordVectorSize * configuration.windowSize);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(1, ActivationFunction.SIGMOID);

            TrainerProgress completeCondition = new TrainerProgress();
            completeCondition.setEpoch(7);
            completeCondition.setPercentage(0);
            return new WordEmbeddingTrainer(configuration, completeCondition);
        }

        private static WordEmbeddingTrainer getWordEmbeddingTrainer2() {
            WordEmbeddingTrainer trainer = getWordEmbeddingTrainer1();

            trainer.getTrainerConfiguration().wordFrequencyRankLowerBound = 1000;

            return trainer;
        }

        private static WordEmbeddingTrainer getWordEmbeddingTrainer3() {
            WordEmbeddingTrainer trainer = getWordEmbeddingTrainer1();

            trainer.getTrainerConfiguration().wordFrequencyRankLowerBound = 2000;

            return trainer;
        }

        private static WordEmbeddingTrainer getWordEmbeddingTrainer4() {
            WordEmbeddingTrainer trainer = getWordEmbeddingTrainer1();

            trainer.getTrainerConfiguration().wordFrequencyRankLowerBound = 3000;

            return trainer;
        }

        private static WordEmbeddingTrainer getWordEmbeddingTrainer5() {
            WordEmbeddingTrainer trainer = getWordEmbeddingTrainer1();

            trainer.getTrainerConfiguration().wordFrequencyRankLowerBound = -1;

            return trainer;
        }

        private static WordEmbeddingTrainer getStackingWordEmbeddingTrainer1() {
            WordEmbeddingTrainerConfiguration configuration = createCommonConfiguration();
            configuration.modelFilePrefix = "b_stacking_";
            configuration.wordFrequencyRankLowerBound = -1;

            configuration.windowSize = 5;
            configuration.hiddenLayerSize = SOFTMAX_HIDDEN_LAYER;

            configuration.annConfiguration =
                    new AnnConfiguration(configuration.wordVectorSize * configuration.windowSize);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(1, ActivationFunction.SIGMOID);

            TrainerProgress completeCondition = new TrainerProgress();
            completeCondition.setEpoch(10);
            completeCondition.setPercentage(0);

            return new WordEmbeddingTrainer(configuration, completeCondition);
        }

        private static WordEmbeddingTrainer getStackingWordEmbeddingTrainer2() {
            WordEmbeddingTrainer trainer = getStackingWordEmbeddingTrainer1();
            WordEmbeddingTrainerConfiguration configuration = trainer.getTrainerConfiguration();

            configuration.annConfiguration =
                    new AnnConfiguration(configuration.wordVectorSize * configuration.windowSize);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(1, ActivationFunction.SIGMOID);

            return trainer;
        }

        private static WordEmbeddingTrainer getStackingWordEmbeddingTrainer3() {
            WordEmbeddingTrainer trainer = getStackingWordEmbeddingTrainer1();
            WordEmbeddingTrainerConfiguration configuration = trainer.getTrainerConfiguration();

            configuration.annConfiguration =
                    new AnnConfiguration(configuration.wordVectorSize * configuration.windowSize);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(1, ActivationFunction.SIGMOID);

            return trainer;
        }

        private static SoftmaxTrainer getSoftmaxTrainer() {
            WordEmbeddingTrainerConfiguration configuration = createCommonConfiguration();
            configuration.modelFilePrefix = "c_softmax_model_";
            configuration.wordFrequencyRankLowerBound = -1;

            configuration.windowSize = 5;
            configuration.hiddenLayerSize = SOFTMAX_HIDDEN_LAYER;

            configuration.annConfiguration =
                    new AnnConfiguration(configuration.wordVectorSize * configuration.windowSize);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(configuration.hiddenLayerSize, ActivationFunction.TANH);
            configuration.annConfiguration.addLayer(WordSegmentationInstancePool.TAGS.size(),
                    ActivationFunction.SOFTMAX);

            TrainerProgress completeCondition = new TrainerProgress();
            completeCondition.setEpoch(300);
            completeCondition.setPercentage(0);

            return new SoftmaxTrainer(configuration, completeCondition);
        }

        private static final List<? extends Trainer> TRAINERS = Lists.newArrayList(getWordEmbeddingTrainer1(),
                getWordEmbeddingTrainer2(), getWordEmbeddingTrainer3(), getWordEmbeddingTrainer4(),
                getWordEmbeddingTrainer5(), getStackingWordEmbeddingTrainer1(), getStackingWordEmbeddingTrainer2(),
                getStackingWordEmbeddingTrainer3(), getSoftmaxTrainer());

        @Override
        public String identifier() {
            return "chinese_word_segmentation";
        }

        @Override
        public Trainer getTask(int id) {
            return TRAINERS.get(id);
        }

        @Override
        public int size() {
            return TRAINERS.size();
        }
    }
}
