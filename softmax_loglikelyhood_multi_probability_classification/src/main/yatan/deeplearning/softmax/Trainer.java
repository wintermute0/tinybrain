package yatan.deeplearning.softmax;

import java.io.File;

import org.apache.log4j.Logger;

import akka.actor.Actor;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.data.parser.bakeoff2005.ICWB2Parser;
import yatan.data.sequence.TaggedSentenceDataset;
import yatan.deeplearning.softmax.contract.SoftmaxClassificationEvaluatorContractImpl;
import yatan.deeplearning.softmax.contract.WordEmbeddingAnnParameterActorContractImpl;
import yatan.deeplearning.softmax.contract.SoftmaxClassificationTrainingContractImpl;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.distributedcomputer.actors.AuditActor;
import yatan.distributedcomputer.actors.ComputeActor;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.actors.data.DataActor;
import yatan.distributedcomputer.contract.ComputeActorContract;
import yatan.distributedcomputer.contract.ParameterActorContract;
import yatan.distributedcomputer.contract.data.impl.DataProducer;

public class Trainer {
    public static final TrainerConfiguration TRAINER_CONFIGURATION = new TrainerConfiguration();

    static {
        TRAINER_CONFIGURATION.l2Lambdas = new double[] {0.0001, 0.0001, 0.0001, 0.0001, 0.0001};
        // TRAINER_CONFIGURATION.l2Lambdas = new double[] {0.001, 0.001, 0.001, 0.0001, 0.0001};
        // TRAINER_CONFIGURATION.l2Lambdas = new double[] {0, 0, 0, 0, 0};
        // TRAINER_CONFIGURATION.l2Lambdas = new double[] {0, 0, 0};

        TRAINER_CONFIGURATION.hiddenLayerSize = 100;
        TRAINER_CONFIGURATION.wordVectorSize = 50;

        TRAINER_CONFIGURATION.dropout = false;
    }

    @SuppressWarnings("serial")
    public static void main(String[] args) {
        final Injector commonModuleInjector = Guice.createInjector(new CommonModule());
        final Injector trainingModuleInjector = commonModuleInjector.createChildInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = commonModuleInjector.createChildInjector(new EvaluatingModule());
        final Injector trainingDataEvaluatingModuleInjector =
                commonModuleInjector.createChildInjector(new TrainingDataEvaluatingModule());

        final ActorSystem system = ActorSystem.create();

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
                for (int i = 0; i < 16; i++) {
                    system.actorOf(new Props(new UntypedActorFactory() {
                        @Override
                        public Actor create() throws Exception {
                            return trainingModuleInjector.getInstance(ComputeActor.class);
                        }
                    }), "compute" + i);
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

    public static class CommonModule extends AbstractModule {
        @Override
        protected void configure() {
            // bind trainer configuration
            bind(TrainerConfiguration.class).toInstance(TRAINER_CONFIGURATION);
            // load dictionary
            bind(Dictionary.class).toInstance(Dictionary.create(new File("test_files/zh_dict_better.txt")));
            // set word vector size
            bind(Integer.class).annotatedWith(Names.named("word_vector_size")).toInstance(
                    TRAINER_CONFIGURATION.wordVectorSize);
        }
    }

    public static class TrainingModule extends AbstractModule {
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
                AnnConfiguration annConfiguration =
                        new AnnConfiguration(TRAINER_CONFIGURATION.wordVectorSize
                                * WordSegmentationDataProducer.WINDOWS_SIZE);
                annConfiguration.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
                // annConfiguration.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
                // annConfiguration.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
                annConfiguration.addLayer(WordSegmentationInstancePool.TAGS.size(), ActivationFunction.SOFTMAX);
                bind(AnnConfiguration.class).annotatedWith(Names.named("ann_configuration")).toInstance(
                        annConfiguration);

                // bind parameter actor impl
                bind(ParameterActorContract.class).to(WordEmbeddingAnnParameterActorContractImpl.class);

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
