package yatan.distributed;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import yatan.distributedcomputer.actors.AuditActor;
import yatan.distributedcomputer.actors.ComputeActor;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.actors.data.DataActor;
import yatan.distributedcomputer.contract.AuditActorContract;
import yatan.distributedcomputer.contract.ComputeActorContract;
import yatan.distributedcomputer.contract.ParameterActorContract;
import yatan.distributedcomputer.contract.data.DataActorContract;
import yatan.distributedcomputer.contract.data.DataFactoryActorContract;
import yatan.distributedcomputer.contract.data.impl.DataProducer;
import yatan.distributedcomputer.contract.data.impl.DefaultDataActorContract;
import yatan.distributedcomputer.contract.data.impl.DefaultDataFactoryActorContract;
import yatan.distributedcomputer.contract.impl.DefaultAuditActorContract;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

public class Demo {
    public static void main(String[] args) {
        final Injector trainingModuleInjector = Guice.createInjector(new TrainingModule());
        final Injector evaluatingModuleInjector = Guice.createInjector(new EvaluatingModule());

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

        for (int i = 0; i < 4; i++) {
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
    }

    public static class TrainingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ParameterActorContract.class).to(ParameterActorContractImpl.class);
            bind(DataProducer.class).to(LetterRecognitionDataProducer.class);
            bind(ComputeActorContract.class).to(ComputeActorContractTrainingImpl.class);
        }
    }

    public static class EvaluatingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ComputeActorContract.class).to(ComputeActorContractEvaluatorImpl.class);
        }
    }
}
