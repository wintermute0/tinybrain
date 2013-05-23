package yatan.deeplearning.autoencoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import yatan.ann.AnnData;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.autoencoder.contract.AutoEncoderParameterActorContractImpl.PersistableState;
import yatan.deeplearning.autoencoder.contract.Helper;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

public class TestSparseAutoencoder {
    public static void main(String[] args) throws Exception {
        File stateFile = new File("test_files/ae_model_1369236308218.json");
        FileInputStream is = new FileInputStream(stateFile);
        InputStreamReader reader = new InputStreamReader(is, Charsets.UTF_8);
        PersistableState state = new Gson().fromJson(reader, PersistableState.class);

        WordEmbeddingTrainingInstance instance = toWordEmbeddingTrainingInstance(state.wordEmbedding, "");
        AnnData annData = Helper.convertToAnnData(state.wordEmbedding, instance);

        AnnTrainer trainer = new AnnTrainer();
        double[][] sum = new double[2][];
        trainer.run(state.annModel, annData.getInput(), sum, null);

    }

    private static WordEmbeddingTrainingInstance toWordEmbeddingTrainingInstance(WordEmbedding wordEmbedding,
            String... strings) {
        List<Integer> indices = Lists.newArrayList();
        for (String str : strings) {
            indices.add(wordEmbedding.indexOf(str));
        }

        WordEmbeddingTrainingInstance instance = new WordEmbeddingTrainingInstance();
        instance.setInput(indices);
        return instance;
    }
}
