package yatan.deeplearning.wordembedding;

import java.io.File;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import yatan.deeplearning.wordembedding.actor.impl.ComputeActorWordEmbeddingEvaluatorImpl;
import yatan.deeplearning.wordembedding.actor.impl.ComputeActorWordEmbeddingTrainingImpl;
import yatan.deeplearning.wordembedding.actor.impl.ParameterActorWordEmbeddingImpl;
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
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

public class WordEmbeddingTrainer {
    @SuppressWarnings("serial")
    public static void main(String[] args) {
        final Injector trainingModuleInjector = Guice.createInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = Guice.createInjector(new EvaluatingModule());
        final Injector perplexityEvaluatingModuleInjector = Guice.createInjector(new PerplexityEvaluatingModule());

        ActorSystem system = ActorSystem.create();
        ActorRef parametersActor = system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingModuleInjector.getInstance(ParameterActor.class);
            }
        }), "parameter");
        ActorRef dataActor = system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingModuleInjector.getInstance(DataActor.class);
            }
        }), "data");

        ActorRef auditActor = system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return trainingModuleInjector.getInstance(AuditActor.class);
            }
        }), "audit");

        for (int i = 0; i < 8; i++) {
            ActorRef computeActor1 = system.actorOf(new Props(new UntypedActorFactory() {
                @Override
                public Actor create() throws Exception {
                    return trainingModuleInjector.getInstance(ComputeActor.class);
                }
            }), "compute" + i);
        }

        ActorRef evaluatorActor1 = system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return evaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor1");

        ActorRef evaluatorActor2 = system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() throws Exception {
                return perplexityEvaluatingModuleInjector.getInstance(ComputeActor.class);
            }
        }), "evalutor2");
    }

    public static class TrainingModule extends AbstractModule {
        private static Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"));

        @Override
        protected void configure() {
            bind(Dictionary.class).toInstance(dictionary);
            bind(Integer.class).annotatedWith(Names.named("data_produce_batch_size")).toInstance(200000);

            bind(ParameterActorContract.class).to(ParameterActorWordEmbeddingImpl.class);
            bind(DataProducer.class).to(ZhWikiTrainingDataProducer.class);
            bind(ComputeActorContract.class).to(ComputeActorWordEmbeddingTrainingImpl.class);
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ComputeActorContract.class).to(ComputeActorWordEmbeddingEvaluatorImpl.class);
        }
    }

    public static class PerplexityEvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Dictionary.class).toInstance(TrainingModule.dictionary);

            bind(ComputeActorContract.class).to(PerplextiyEvaluator.class);
        }
    }
}
