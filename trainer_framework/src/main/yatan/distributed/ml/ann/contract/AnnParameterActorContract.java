package yatan.distributed.ml.ann.contract;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnGradient;
import yatan.ann.DefaultAnnModel;
import yatan.commons.matrix.Matrix;
import yatan.distributed.akka.BaseActorContract;
import yatan.distributed.ml.ann.AnnTrainerConfiguration;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.Parameter.ParameterIndexPath;
import yatan.distributedcomputer.actors.ParameterActor;
import yatan.distributedcomputer.contract.ParameterActorContract;

public class AnnParameterActorContract extends BaseActorContract implements ParameterActorContract {
    public static final double ADA_DELTA_RHO = 0.95;
    public static final double ADA_DELTA_EPSILON = 0.000001;

    private static final double STATE_SAVING_INTERVAL_MINUTES = 10;

    private static final String MODEL_FOLDER = "test_files/results/";
    private static final String MODEL_FILE_PREFIX = "ae_model_";

    private final AnnConfiguration annConfiguration;

    private Date lastSaveTime = new Date();

    @Inject(optional = false)
    private AnnTrainerConfiguration trainerConfiguration;

    private DefaultAnnModel annModel;

    private List<Matrix> annDeltaGradientSumSquare;
    private List<Matrix> deltaAnnWeightSumSquare;

    @Inject
    public AnnParameterActorContract(@Named("ann_configuration") AnnConfiguration annConfiguration) {
        Preconditions.checkArgument(annConfiguration != null);

        this.annConfiguration = annConfiguration;
    }

    @Override
    public void requestParameters(ParameterIndexPath start, ParameterIndexPath end) {
        initIfNecessary();

        Parameter parameter = new Parameter();
        parameter.setSerializable(new Serializable[] {annModel});

        tellSender(new ParameterActor.ReceiveParameterMessage(getActor().getMessage(), parameter));
    }

    @Override
    public void updateGradient(Parameter gradient) {
        Preconditions.checkArgument(gradient != null, "Gradient cannot be null.");

        Serializable[] inputData = (Serializable[]) gradient.getSerializable();
        AnnGradient annGradient = (AnnGradient) inputData[0];
        annModel.update(annGradient, ADA_DELTA_RHO, ADA_DELTA_EPSILON, annDeltaGradientSumSquare,
                this.deltaAnnWeightSumSquare);

        // save state if necessary
        if (new Date().getTime() - lastSaveTime.getTime() > STATE_SAVING_INTERVAL_MINUTES * 60 * 1000) {
            lastSaveTime = new Date();
            saveState();
        }
    }

    private void initIfNecessary() {
        if (annModel == null) {
            getLogger().info("Initializing parameter actor...");
            // load persisted states
            loadState();

            if (annModel == null) {
                getLogger().info("Create new ANN model with configuration " + this.annConfiguration);
                // Configuration configuration =
                // new Configuration(wordEmbedding.getWordVectorSize() * TrainingInstanceProducer.WINDOWS_SIZE);
                // configuration.addLayer(300, ActivationFunction.TANH);
                // configuration.addLayer(DataCenter.tags().size(), ActivationFunction.SOFTMAX, false);
                annModel = new DefaultAnnModel(this.annConfiguration);
            }

            if (annDeltaGradientSumSquare == null || deltaAnnWeightSumSquare == null) {
                getLogger().info("Create new ANN gradient/delta sum squre.");
                this.annDeltaGradientSumSquare = Lists.newArrayList();
                this.deltaAnnWeightSumSquare = Lists.newArrayList();
                for (int i = 0; i < annModel.getLayerCount(); i++) {
                    Matrix layer = annModel.getLayer(i);
                    this.annDeltaGradientSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
                    this.deltaAnnWeightSumSquare.add(new Matrix(layer.rowSize(), layer.columnSize()));
                }
            }
        }
    }

    private void saveState() {
        File stateFile = new File(MODEL_FOLDER + MODEL_FILE_PREFIX + (new Date().getTime()) + ".json");
        getLogger().info("Saving parameter server state to " + stateFile + "...");
        FileWriterWithEncoding writer = null;
        try {
            writer = new FileWriterWithEncoding(stateFile, Charsets.UTF_8);
            String json =
                    new Gson().toJson(new PersistableState(annModel, annDeltaGradientSumSquare,
                            this.deltaAnnWeightSumSquare));
            writer.write(json);
        } catch (IOException e) {
            getLogger().error("Error occurred while trying to save parameter server state: " + e.getMessage(), e);
        } finally {
            close(writer);
        }
    }

    private boolean loadState() {
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
                PersistableState state = new Gson().fromJson(reader, PersistableState.class);

                // only reuse other saved states if the ANN model configuration is identical
                if (this.annConfiguration.equals(state.annModel.getConfiguration())) {
                    getLogger().info(
                            "Loading ANN and delta sum squre data because the ANN model configuration is identical.");
                    this.annModel = state.annModel;

                    this.annDeltaGradientSumSquare = state.annDeltaWeightSumSquare;
                    this.deltaAnnWeightSumSquare = state.deltaAnnSumSquare;
                } else {
                    getLogger().info(
                            "Ignore ANN and delta sum squre data because the ANN model configuration is differenct.");

                    // otherwise, we only reuse word embedding, to we need to scale it first
                    // double sigma = 1;
                    // getLogger().info("Scale word embedding with sigma " + sigma);
                    // scaleWordEmbedding(wordEmbedding, sigma);
                }
            } catch (IOException e) {
                getLogger().error("Error occurred while trying to load parameter server state: " + e.getMessage(), e);
                return false;
            } finally {
                close(reader, is);
            }

            return true;
        } else {
            getLogger().info("Can't find any persisted parameter sever state. Let's start from strach.");
            return false;
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

    public static class PersistableState {
        public DefaultAnnModel annModel;

        public List<Matrix> annDeltaWeightSumSquare;
        public List<Matrix> deltaAnnSumSquare;

        public PersistableState(DefaultAnnModel annModel, List<Matrix> annDeltaSumSquare, List<Matrix> deltaAnnSumSquare) {
            this.annModel = annModel;
            this.annDeltaWeightSumSquare = annDeltaSumSquare;
            this.deltaAnnSumSquare = deltaAnnSumSquare;
        }
    }
}
