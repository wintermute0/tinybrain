package yatan.distributed.ml.ann;

import java.io.File;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.distributed.ml.ann.AnnTrainerConfiguration.LossFunction;
import yatan.distributed.ml.ann.contract.AnnParameterActorContract;
import yatan.distributed.ml.ann.contract.AnnTrainContract;
import yatan.distributed.ml.ann.contract.evaluate.AnnClassificationErrorEvaluateContract;

import yatan.distributed.ml.ann.data.MNISTHandWrittenEvaluatingDataProducer;
import yatan.distributed.ml.ann.data.MNISTHandWrittenTrainingDataProducer;
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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class AnnTrainer {
    private static final AnnTrainerConfiguration TRAINER_CONFIGURATION = new AnnTrainerConfiguration();
    private static final int TRAINING_ACTOR_COUNT = 4;

    private static final Dictionary DICTIONARY = Dictionary.create(new File("test_files/zh_dict.txt"), 500);

    static {
        TRAINER_CONFIGURATION.lossFunction = LossFunction.SoftmaxLoglikelyhood;
    }

    @SuppressWarnings("serial")
    public static void main(String[] args) {
        final Injector commonModuleInjector = Guice.createInjector(new CommonModule());
        final Injector trainingModuleInjector = commonModuleInjector.createChildInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = commonModuleInjector.createChildInjector(new EvaluatingModule());

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
                return evaluatingModuleInjector.getInstance(DataActor.class);
            }
        }), "test_data");
        system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return evaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor1");
    }

    public static class CommonModule extends AbstractModule {
        @Override
        protected void configure() {
            // bind trainer configuration
            bind(AnnTrainerConfiguration.class).toInstance(TRAINER_CONFIGURATION);
            // load dictionary
            bind(Dictionary.class).toInstance(DICTIONARY);
        }
    }

    public static class TrainingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(10000);

            // bind ann configuration
            AnnConfiguration annConfiguration = new AnnConfiguration(784);
            annConfiguration.addLayer(500, ActivationFunction.TANH);
            annConfiguration.addLayer(300, ActivationFunction.TANH);
            annConfiguration.addLayer(10, ActivationFunction.SOFTMAX);
            bind(AnnConfiguration.class).annotatedWith(Names.named("ann_configuration")).toInstance(annConfiguration);

            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            bind(ParameterActorContract.class).to(AnnParameterActorContract.class);
            bind(DataProducer.class).to(MNISTHandWrittenTrainingDataProducer.class);
            bind(ComputeActorContract.class).to(AnnTrainContract.class);
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(10000);

            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/test_data");

            bind(DataProducer.class).to(MNISTHandWrittenEvaluatingDataProducer.class);
            bind(ComputeActorContract.class).to(AnnClassificationErrorEvaluateContract.class);
        }
    }
}
