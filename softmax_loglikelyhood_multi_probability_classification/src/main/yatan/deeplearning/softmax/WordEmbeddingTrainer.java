package yatan.deeplearning.softmax;

import java.io.File;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.deeplearning.softmax.contract.SoftmaxClassificationTrainingContractImpl;
import yatan.deeplearning.softmax.contract.WordEmbeddingAnnParameterActorContractImpl;
import yatan.deeplearning.wordembedding.actor.impl.ComputeActorWordEmbeddingEvaluatorImpl;
import yatan.deeplearning.wordembedding.actor.impl.PerplextiyEvaluator;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.data.ZhWikiTrainingDataProducer;
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
    private static final int TRAINING_ACTOR_COUNT = 4;

    static {
        // TRAINER_CONFIGURATION.l2Lambdas = new double[] {0.001, 0.001, 0.001};
        TRAINER_CONFIGURATION.l2Lambdas = new double[] {0, 0, 0};

        TRAINER_CONFIGURATION.hiddenLayerSize = 100;
        TRAINER_CONFIGURATION.wordVectorSize = 50;

        TRAINER_CONFIGURATION.dropout = false;
    }

    @SuppressWarnings("serial")
    public static void main(String[] args) {
        final Injector commonModuleInjector = Guice.createInjector(new CommonModule());
        final Injector trainingModuleInjector = commonModuleInjector.createChildInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = commonModuleInjector.createChildInjector(new EvaluatingModule());
        final Injector perplexityEvaluatingModuleInjector =
                commonModuleInjector.createChildInjector(new PerplexityEvaluatingModule());

        ActorSystem system = ActorSystem.create();
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

        for (int i = 0; i < TRAINING_ACTOR_COUNT; i++) {
            system.actorOf(new Props(new UntypedActorFactory() {
                @Override
                public Actor create() throws Exception {
                    return trainingModuleInjector.getInstance(ComputeActor.class);
                }
            }), "compute" + i);
        }

        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return evaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor1");

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
            bind(Dictionary.class).toInstance(Dictionary.create(new File("test_files/zh_dict.txt")));
            // set word vector size
            bind(Integer.class).annotatedWith(Names.named("word_vector_size")).toInstance(
                    TRAINER_CONFIGURATION.wordVectorSize);
        }
    }

    public static class TrainingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(200000);

            // bind ann configuration
            AnnConfiguration annConfiguration =
                    new AnnConfiguration(TRAINER_CONFIGURATION.wordVectorSize * ZhWikiTrainingDataProducer.WINDOWS_SIZE);
            annConfiguration.addLayer(TRAINER_CONFIGURATION.hiddenLayerSize, ActivationFunction.TANH);
            annConfiguration.addLayer(2, ActivationFunction.SOFTMAX);
            bind(AnnConfiguration.class).annotatedWith(Names.named("ann_configuration")).toInstance(annConfiguration);

            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            bind(ParameterActorContract.class).to(WordEmbeddingAnnParameterActorContractImpl.class);
            bind(DataProducer.class).to(ZhWikiTrainingDataProducer.class);
            bind(ComputeActorContract.class).to(SoftmaxClassificationTrainingContractImpl.class);
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            bind(ComputeActorContract.class).to(ComputeActorWordEmbeddingEvaluatorImpl.class);
        }
    }

    public static class PerplexityEvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            bind(ComputeActorContract.class).to(PerplextiyEvaluator.class);
        }
    }
}
