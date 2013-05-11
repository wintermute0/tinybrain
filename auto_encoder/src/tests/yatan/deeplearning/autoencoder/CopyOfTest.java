package yatan.deeplearning.autoencoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import scala.actors.threadpool.Arrays;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnConfiguration.ActivationFunction;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.autoencoder.contract.Helper;
import yatan.deeplearning.autoencoder.contract.AutoEncoderParameterActorContractImpl.PersistableState;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.data.ZhWikiTrainingDataProducer;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;

import com.google.common.base.Charsets;
import com.google.gson.Gson;

public class CopyOfTest {
    public static void main(String[] args) throws Exception {
        File stateFile = new File("test_files/results/ae_model_1367906643041.json");

        FileInputStream is = null;
        InputStreamReader reader = null;

        is = new FileInputStream(stateFile);
        reader = new InputStreamReader(is, Charsets.UTF_8);
        PersistableState state = new Gson().fromJson(reader, PersistableState.class);

        DefaultAnnModel autoEncoder = state.annModel;

        // train the new model
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"), 500);
        // ZhWikiTrainingDataProducer.WINDOWS_SIZE = 5;
        ZhWikiTrainingDataProducer producer = new ZhWikiTrainingDataProducer(dictionary);
        AnnTrainer trainer = new AnnTrainer();

        List<Data> datas = producer.produceData(1);
        for (Data data : datas) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            // AnnData annData = Helper.convertToAnnData(dictionary, instance);
            // System.out.println(Arrays.toString(annData.getInput()));
            // System.out.println(Arrays.toString(trainer.run(autoEncoder, annData.getInput(), new double[10][])[1]));
        }
    }
}
