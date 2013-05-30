package yatan.deeplearning.wordembedding;

import java.io.File;
import java.io.IOException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.data.parser.bakeoff2005.ICWB2Parser;
import yatan.data.sequence.TaggedSentenceDataset;
import yatan.deeplearning.wordembedding.actor.impl.ComputeActorWordEmbeddingEvaluatorImpl;
import yatan.deeplearning.wordembedding.actor.impl.ComputeActorWordEmbeddingTrainingImpl;
import yatan.deeplearning.wordembedding.actor.impl.ParameterActorWordEmbeddingImpl;
import yatan.deeplearning.wordembedding.actor.impl.PerplextiyEvaluator;
import yatan.deeplearning.wordembedding.data.BakeOffDataProducer;
import yatan.deeplearning.wordembedding.data.ZhWikiTrainingDataProducer;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.distributedcomputer.actors.AuditActor;
import yatan.distributedcomputer.actors.ComputeActor;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.actors.data.DataActor;
import yatan.distributedcomputer.contract.ComputeActorContract;
import yatan.distributedcomputer.contract.ParameterActorContract;
import yatan.distributedcomputer.contract.data.impl.DataProducer;
import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

public class WordEmbeddingTrainer {
    private static final TrainerConfiguration TRAINER_CONFIGURATION = new TrainerConfiguration();
    private static final int TRAINING_ACTOR_COUNT = 2;

    static {
        // TRAINER_CONFIGURATION.l2Lambdas = new double[] {0.00001, 0.00001, 0.00001};
        // TRAINER_CONFIGURATION.l2Lambdas = new double[] {0, 0};

        TRAINER_CONFIGURATION.hiddenLayerSize = 150;
        TRAINER_CONFIGURATION.wordVectorSize = 50;

        TRAINER_CONFIGURATION.dropout = false;
    }

    @SuppressWarnings("serial")
    public static void main(String[] args) {
        final Injector commonModuleInjector = Guice.createInjector(new CommonModule());
        final Injector trainingModuleInjector = commonModuleInjector.createChildInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = commonModuleInjector.createChildInjector(new EvaluatingModule());
        final Injector trainingEvaluatingModuleInjector =
                commonModuleInjector.createChildInjector(new TrainingEvaluatingModule());
        final Injector perplexityEvaluatingModuleInjector =
                commonModuleInjector.createChildInjector(new PerplexityEvaluatingModule());

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
                return evaluatingModuleInjector.getInstance(DataActor.class);
            }
        }), "evaluate_data");

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
                for (int i = 0; i < TRAINING_ACTOR_COUNT; i++) {
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
                return evaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor1");

        // system.actorOf(new Props(new UntypedActorFactory() {
        // @Override
        // public Actor create() throws Exception {
        // return trainingEvaluatingModuleInjector.getInstance(ComputeActor.class);
        // }
        // }), "evalutor3");

        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return perplexityEvaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor2");
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
            bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(500000);

            // bind ann configuration
            AnnConfiguration annConfiguration =
                    new AnnConfiguration(TRAINER_CONFIGURATION.wordVectorSize * ZhWikiTrainingDataProducer.WINDOWS_SIZE);
            annConfiguration.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
            annConfiguration.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
            // annConfiguration.addLayer(2, ActivationFunction.SOFTMAX);
            annConfiguration.addLayer(1, ActivationFunction.SIGMOID);
            bind(AnnConfiguration.class).annotatedWith(Names.named("ann_configuration")).toInstance(annConfiguration);

            bind(ParameterActorContract.class).to(ParameterActorWordEmbeddingImpl.class);
            bind(ComputeActorContract.class).to(ComputeActorWordEmbeddingTrainingImpl.class);

            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            // wiki data
            bind(DataProducer.class).to(ZhWikiTrainingDataProducer.class);

            // bakeoff data
            // bind(boolean.class).annotatedWith(Names.named("training")).toInstance(true);
            // try {
            // bind(TaggedSentenceDataset.class).annotatedWith(Names.named("tagged_sentence_dataset")).toInstance(
            // new ICWB2Parser().parse(new File("data/icwb2-data/training/pku_training.utf8")));
            // bind(DataProducer.class).to(BakeOffDataProducer.class);
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ComputeActorContract.class).to(ComputeActorWordEmbeddingEvaluatorImpl.class);

            // wiki data
            // bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            // bakeoff data
            bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(500000);
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/evaluate_data");

            bind(boolean.class).annotatedWith(Names.named("training")).toInstance(false);
            try {
                bind(TaggedSentenceDataset.class).annotatedWith(Names.named("tagged_sentence_dataset")).toInstance(
                        new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8")));
                bind(DataProducer.class).to(BakeOffDataProducer.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TrainingEvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ComputeActorContract.class).to(ComputeActorWordEmbeddingEvaluatorImpl.class);

            // wiki data
            // bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            // bakeoff data
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");
        }
    }

    public static class PerplexityEvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ComputeActorContract.class).to(PerplextiyEvaluator.class);

            // wiki data
            // bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            // bakeoff data
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/evaluate_data");
        }
    }
}
