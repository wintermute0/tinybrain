package yatan.deeplearning.softmax.contract.parameter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.FileWriterWithEncoding;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.dispatch.Future;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.distributed.akka.BaseActorContract;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class WordEmbeddingAnnParameterActorContractImpl2 extends BaseActorContract implements ParameterActorContract {
    private static final Timeout SLICE_UPDATE_TIMEOUT = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    private static final double STATE_SAVING_INTERVAL_MINUTES = 10;

    private static final String DEFAULT_MODEL_FILE_PREFIX = "softmax_model_";

    private static final String MODEL_FOLDER = "test_files/results/";

    private static Date lastSaveTime = new Date();

    private final List<ActorRef> sliceUpdateActors = Lists.newArrayList();

    @Inject(optional = true)
    @Named("model_file_prefix")
    private String modelFilePrefix = DEFAULT_MODEL_FILE_PREFIX;

    @Inject
    @Named("parameter_actor_update_slice")
    private int updateSlice;

    @Inject
    private ParameterFactory parameterFactory;
    @Inject
    private ParameterUpdator parameterUpdator;

    private Parameter parameter;
    private Parameter parameterSnapshot;

    @Override
    public void preStart() {
        getLogger().info(
                "Initializing " + getClass().getSimpleName() + " with model file prefix = " + this.modelFilePrefix
                        + ", update slice = " + this.updateSlice);

        // load persisted state
        JsonObject jsonObject = loadState();

        // init parameter factory and create parameters
        this.parameterFactory.load(jsonObject);
        this.parameter = this.parameterFactory.initializeParameter();
        this.parameterSnapshot = this.parameterFactory.newEmptyParameter();
        this.parameterFactory.cloneParameter(this.parameterSnapshot, 0, 1);

        // init parameter updateor
        this.parameterUpdator.load(jsonObject);

        // create slice update actors
        for (int i = 0; i < updateSlice; i++) {
            this.sliceUpdateActors.add(getActor().context().actorOf(
                    new Props(new SliceUpdateActorFactory(i, updateSlice)), "slice_updator_" + i));
        }
    }

    private class SliceUpdateActorFactory implements UntypedActorFactory {
        private static final long serialVersionUID = 5348017784800584586L;
        private final int sliceId;
        private final int totalSlice;

        public SliceUpdateActorFactory(int sliceId, int totalSlice) {
            Preconditions.checkArgument(sliceId < totalSlice && sliceId >= 0);

            this.sliceId = sliceId;
            this.totalSlice = totalSlice;
        }

        @Override
        public Actor create() throws Exception {
            return new SliceUpdateActor(sliceId, totalSlice);
        }
    }

    private class SliceUpdateActor extends UntypedActor {
        private final int sliceId;
        private final int totalSlice;

        public SliceUpdateActor(int sliceId, int totalSlice) {
            Preconditions.checkArgument(sliceId < totalSlice && sliceId >= 0);

            this.sliceId = sliceId;
            this.totalSlice = totalSlice;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            Parameter gradient = (Parameter) message;

            parameterUpdator.update(WordEmbeddingAnnParameterActorContractImpl2.this.parameter, gradient, this.sliceId,
                    this.totalSlice);
            parameterFactory.cloneParameter(parameterSnapshot, this.sliceId, this.totalSlice);

            getSender().tell("done.");
        }
    }

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(), this.parameterSnapshot));
        // tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(),
        // this.parameterFactory.cloneParameter()));
    }

    @Override
    public void updateGradient(Parameter gradient) {
        Preconditions.checkArgument(gradient != null, "Gradient cannot be null.");

        // create a new parameter snapshot for this update
        this.parameterSnapshot = this.parameterFactory.newEmptyParameter();

        final List<Future<Object>> futures = Lists.newArrayList();
        for (int i = 0; i < this.sliceUpdateActors.size(); i++) {
            futures.add(Patterns.ask(this.sliceUpdateActors.get(i), gradient, SLICE_UPDATE_TIMEOUT));
        }

        // wait till all update slice is done
        final Future<Iterable<Object>> aggregatedSliceUpdateFutures =
                Futures.sequence(futures, getActor().getContext().dispatcher());
        aggregatedSliceUpdateFutures.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable arg0, Iterable<Object> arg1) throws Throwable {
                synchronized (aggregatedSliceUpdateFutures) {
                    aggregatedSliceUpdateFutures.notify();
                }
            }
        });

        try {
            synchronized (aggregatedSliceUpdateFutures) {
                while (!aggregatedSliceUpdateFutures.isCompleted()) {
                    aggregatedSliceUpdateFutures.wait();
                }
            }
        } catch (InterruptedException e) {
            getLogger().error("Interrupted while waiting for the slice update actors.", e);
        }

        // save state if necessary
        if (new Date().getTime() - lastSaveTime.getTime() > STATE_SAVING_INTERVAL_MINUTES * 60 * 1000) {
            lastSaveTime = new Date();
            saveState();
        }
    }

    private void saveState() {
        StateContainer stateContainer = new StateContainer();
        this.parameterFactory.save(stateContainer.states);
        this.parameterUpdator.save(stateContainer.states);

        File stateFile = new File(MODEL_FOLDER + modelFilePrefix + (new Date().getTime()) + ".json");
        getLogger().info("Saving parameter server state to " + stateFile + "...");
        FileWriterWithEncoding writer = null;
        try {
            writer = new FileWriterWithEncoding(stateFile, Charsets.UTF_8);
            String json = new Gson().toJson(stateContainer.states);
            writer.write(json);
        } catch (IOException e) {
            getLogger().error("Error occurred while trying to save parameter server state: " + e.getMessage(), e);
        } finally {
            close(writer);
        }
    }

    private JsonObject loadState() {
        getLogger().info("Trying to find persisted parameter server state...");
        File stateFile = null;
        File modelFolderFile = new File(MODEL_FOLDER);
        if (modelFolderFile.isDirectory()) {
            for (File file : modelFolderFile.listFiles()) {
                if (file.isFile() && Files.getFileExtension(file.getName()).equals("json")
                        && (stateFile == null || stateFile.getName().compareTo(file.getName()) < 0)) {
                    stateFile = file;
                }
            }
        }

        if (stateFile != null) {
            getLogger().info("Loading parameter server state from " + stateFile + "...");
            FileInputStream is = null;
            InputStreamReader reader = null;
            try {
                is = new FileInputStream(stateFile);
                reader = new InputStreamReader(is, Charsets.UTF_8);
                return new JsonParser().parse(reader).getAsJsonObject();
            } catch (IOException e) {
                getLogger().error("Error occurred while trying to load parameter server state: " + e.getMessage(), e);
                return new JsonObject();
            } finally {
                close(reader, is);
            }
        } else {
            getLogger().info("Can't find any persisted parameter sever state. Let's start from strach.");
            return new JsonObject();
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

    public static class StateContainer {
        public Map<String, Serializable> states = Maps.newHashMap();
    }
}
