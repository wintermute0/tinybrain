package yatan.deeplearning.softmax;

import java.io.File;

import org.apache.log4j.Logger;

import akka.actor.Actor;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import yatan.ann.AnnConfiguration;
import yatan.data.parser.bakeoff2005.ICWB2Parser;
import yatan.data.sequence.TaggedSentenceDataset;
import yatan.deeplearning.softmax.contract.compute.SoftmaxClassificationEvaluatorContractImpl;
import yatan.deeplearning.softmax.contract.compute.SoftmaxClassificationTrainingContractImpl;
import yatan.deeplearning.softmax.contract.parameter.ParameterFactory;
import yatan.deeplearning.softmax.contract.parameter.ParameterUpdator;
import yatan.deeplearning.softmax.contract.parameter.WordEmbeddingAnnParameterActorContractImpl2;
import yatan.deeplearning.softmax.contract.parameter.factory.WordEmbeddingANNParameterFactory;
import yatan.deeplearning.softmax.contract.parameter.updator.AdaGradParameterUpdator;
import yatan.deeplearning.softmax.data.producer.ProgressReporter;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer;
import yatan.deeplearning.trainer.Trainer;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.distributedcomputer.actors.AuditActor;
import yatan.distributedcomputer.actors.ComputeActor;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.actors.data.DataActor;
import yatan.distributedcomputer.contract.ComputeActorContract;
import yatan.distributedcomputer.contract.ParameterActorContract;
import yatan.distributedcomputer.contract.data.impl.DataProducer;

public class SoftmaxTrainer implements Trainer {
    private final WordEmbeddingTrainerConfiguration trainerConfiguration;
    private final TrainerProgress completeCondition;
    private ActorSystem system;

    // ANN_CONFIGURATION = new AnnConfiguration(TRAINER_CONFIGURATION.wordVectorSize * WINDOW_SIZE);
    // ANN_CONFIGURATION.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
    // ANN_CONFIGURATION.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
    // ANN_CONFIGURATION.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
    // ANN_CONFIGURATION.addLayer(WordSegmentationInstancePool.TAGS.size(), ActivationFunction.SOFTMAX);

    public SoftmaxTrainer(WordEmbeddingTrainerConfiguration configuration, TrainerProgress completeCondition) {
        Preconditions.checkArgument(configuration != null);
        Preconditions.checkArgument(completeCondition != null);

        this.trainerConfiguration = configuration;
        this.completeCondition = completeCondition;
    }

    @SuppressWarnings("serial")
    @Override
    public void start() {
        final Injector commonModuleInjector = Guice.createInjector(new CommonModule());
        final Injector trainingModuleInjector = commonModuleInjector.createChildInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = commonModuleInjector.createChildInjector(new EvaluatingModule());
        final Injector trainingDataEvaluatingModuleInjector =
                commonModuleInjector.createChildInjector(new TrainingDataEvaluatingModule());

        system = ActorSystem.create();

        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingModuleInjector.getInstance(ParameterActor.class);
            }
        }), "parameter");
        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingModuleInjector.getInstance(DataActor.class);
            }
        }), "data");
        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingModuleInjector.getInstance(AuditActor.class);
            }
        }), "audit");

        new Thread(new Runnable() {
            @Override
            public void run() {
                int newThreadInterval = 64;
                for (int i = 0; i < trainerConfiguration.trainingActorCount; i++) {
                    synchronized (SoftmaxTrainer.this) {
                        if (system == null) {
                            break;
                        }
                        system.actorOf(new Props(new UntypedActorFactory() {
                            @Override
                            public Actor create() throws Exception {
                                return trainingModuleInjector.getInstance(ComputeActor.class);
                            }
                        }), "compute" + i);
                    }

                    try {
                        Thread.sleep(newThreadInterval * 1000);
                        newThreadInterval = Math.max(8, newThreadInterval / 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return evaluatingModuleInjector.getInstance(DataActor.class);
            }
        }), "evaluate_data");
        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return evaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor1");
        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingDataEvaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evaluator2");
    }

    @Override
    public void start(TrainerProgress progress) {
        ProgressReporter.instance().report(progress.getEpoch(), progress.getPercentage());

        start();
    }

    @Override
    public synchronized void stop() {
        if (this.system != null) {
            this.system.shutdown();
            this.system = null;
        }
    }

    @Override
    public TrainerProgress progress() {
        TrainerProgress progress = new TrainerProgress();

        progress.setEpoch(ProgressReporter.instance().getEpoch());
        progress.setPercentage(ProgressReporter.instance().getPercentage());

        return progress;
    }

    @Override
    public boolean isCompleted() {
        TrainerProgress progress = progress();

        return progress.compareTo(this.completeCondition) > 0;
    }

    public WordEmbeddingTrainerConfiguration getTrainerConfiguration() {
        return trainerConfiguration;
    }

    public TrainerProgress getCompleteCondition() {
        return completeCondition;
    }

    public class CommonModule extends AbstractModule {
        @Override
        protected void configure() {
            // bind window size
            bind(Integer.class).annotatedWith(Names.named("window_size")).toInstance(trainerConfiguration.windowSize);
            bind(Integer.class).annotatedWith(Names.named("frequency_rank_bound")).toInstance(
                    trainerConfiguration.wordFrequencyRankLowerBound);
            // bind trainer configuration
            bind(WordEmbeddingTrainerConfiguration.class).toInstance(trainerConfiguration);
            // load dictionary
            bind(Dictionary.class).toInstance(Dictionary.create(new File("test_files/zh_dict_better.txt")));
            // set word vector size
            bind(Integer.class).annotatedWith(Names.named("word_vector_size")).toInstance(
                    trainerConfiguration.wordVectorSize);
        }
    }

    public class TrainingModule extends AbstractModule {
        @Override
        protected void configure() {
            try {
                bind(boolean.class).annotatedWith(Names.named("training")).toInstance(true);
                bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(100000);

                // bind training data set
                bind(TaggedSentenceDataset.class).annotatedWith(Names.named("tagged_sentence_dataset")).toInstance(
                        new ICWB2Parser().parse(new File("data/icwb2-data/training/pku_training.utf8")));

                // bind data producer
                bind(DataProducer.class).to(WordSegmentationDataProducer.class);

                // bind ann configuration
                bind(AnnConfiguration.class).toInstance(trainerConfiguration.annConfiguration);

                // bind parameter actor impl
                bind(Integer.class).annotatedWith(Names.named("parameter_actor_update_slice")).toInstance(
                        trainerConfiguration.parameterActorUpdateSlice);

                bind(Double.class).annotatedWith(Names.named("word_embedding_lambda")).toInstance(
                        trainerConfiguration.wordEmbeddingLambda);
                bind(Double.class).annotatedWith(Names.named("ann_lambda")).toInstance(trainerConfiguration.annLambda);
                bind(ParameterFactory.class).to(WordEmbeddingANNParameterFactory.class);
                bind(ParameterUpdator.class).to(AdaGradParameterUpdator.class);
                bind(ParameterActorContract.class).to(WordEmbeddingAnnParameterActorContractImpl2.class);

                // set data actor path of to evaluate data actor
                bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

                // bing compute actor impl
                bind(ComputeActorContract.class).to(SoftmaxClassificationTrainingContractImpl.class);
            } catch (Exception e) {
                Logger.getLogger(TrainingModule.class).error(
                        "Error occurred while configuring training module: " + e.getMessage(), e);
            }
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            try {
                bind(boolean.class).annotatedWith(Names.named("training")).toInstance(false);
                bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(100000);

                // bind evaluating data set
                bind(TaggedSentenceDataset.class).annotatedWith(Names.named("tagged_sentence_dataset")).toInstance(
                        new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8")));

                // bind data producer
                bind(DataProducer.class).to(WordSegmentationDataProducer.class);

                // set data actor path of to evaluate data actor
                bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/evaluate_data");

                // bind compute actor impl
                bind(boolean.class).annotatedWith(Names.named("training_data_evaluator")).toInstance(false);
                bind(ComputeActorContract.class).to(SoftmaxClassificationEvaluatorContractImpl.class);
            } catch (Exception e) {
                Logger.getLogger(EvaluatingModule.class).error(
                        "Error occurred while configuring evaluating module: " + e.getMessage(), e);
            }
        }
    }

    public static class TrainingDataEvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            try {
                // bind compute actor impl
                bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");
                bind(boolean.class).annotatedWith(Names.named("training_data_evaluator")).toInstance(true);
                bind(ComputeActorContract.class).to(SoftmaxClassificationEvaluatorContractImpl.class);
            } catch (Exception e) {
                Logger.getLogger(EvaluatingModule.class).error(
                        "Error occurred while configuring evaluating module: " + e.getMessage(), e);
            }
        }
    }
}
