package yatan.deeplearning.autoencoder;

import java.io.File;
import java.io.FileNotFoundException;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnConfiguration.ActivationFunction;

import yatan.deeplearning.autoencoder.contract.AutoEncoderEvaluatorContractImpl;
import yatan.deeplearning.autoencoder.contract.AutoEncoderParameterActorContractImpl;
import yatan.deeplearning.autoencoder.contract.AutoEncoderTrainingContractImpl;
import yatan.deeplearning.autoencoder.data.chinesewordembedding.ChineseWordEmbeddingAutoEncoderDataProvider;

import yatan.deeplearning.softmax.data.TaggedSentenceDataset;
import yatan.deeplearning.softmax.data.parser.ICWB2Parser;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer;
import yatan.deeplearning.wordembedding.data.Dictionary;

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

public class AutoEncoderTrainer {
    private static final TrainerConfiguration TRAINER_CONFIGURATION = new TrainerConfiguration();
    private static final int TRAINING_ACTOR_COUNT = 4;

    private static final Dictionary DICTIONARY = Dictionary.create(new File("test_files/zh_dict.txt"));

    static {
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
                return evaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor1");
    }

    public static class CommonModule extends AbstractModule {
        @Override
        protected void configure() {
            // bind trainer configuration
            bind(TrainerConfiguration.class).toInstance(TRAINER_CONFIGURATION);
            // load dictionary
            bind(Dictionary.class).toInstance(DICTIONARY);
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
                    new AnnConfiguration(TRAINER_CONFIGURATION.wordVectorSize
                            * ChineseWordEmbeddingAutoEncoderDataProvider.WINDOWS_SIZE);
            annConfiguration.addLayer(300, ActivationFunction.TANH);
            // annConfiguration.addLayer(300, ActivationFunction.TANH);
            // annConfiguration.addLayer(300, ActivationFunction.TANH);

            // annConfiguration.addLayer(300, ActivationFunction.TANH);
            annConfiguration.addLayer(annConfiguration.inputDegree, ActivationFunction.TANH);

            bind(AnnConfiguration.class).annotatedWith(Names.named("ann_configuration")).toInstance(annConfiguration);

            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            bind(ParameterActorContract.class).to(AutoEncoderParameterActorContractImpl.class);
            bind(ComputeActorContract.class).to(AutoEncoderTrainingContractImpl.class);

            // bind(DataProducer.class).to(ChineseWordEmbeddingAutoEncoderDataProvider.class);

            // bind training data set
            bind(boolean.class).annotatedWith(Names.named("training")).toInstance(true);
            try {
                bind(TaggedSentenceDataset.class).annotatedWith(Names.named("tagged_sentence_dataset")).toInstance(
                        new ICWB2Parser().parse(new File("data/icwb2-data/training/pku_training.utf8")));
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            bind(DataProducer.class).to(WordSegmentationDataProducer.class);
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            // set data actor path of to evaluate data actor
            bind(String.class).annotatedWith(Names.named("data_actor_path")).toInstance("/user/data");

            bind(ComputeActorContract.class).to(AutoEncoderEvaluatorContractImpl.class);
        }
    }
}
