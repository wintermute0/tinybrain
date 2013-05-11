package yatan.deeplearning.autoencoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import yatan.ann.AnnConfiguration;
import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
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

public class Test {
    public static void main(String[] args) throws Exception {
        File stateFile = new File("test_files/results/ae_model_1367906643041.json");

        FileInputStream is = null;
        InputStreamReader reader = null;

        is = new FileInputStream(stateFile);
        reader = new InputStreamReader(is, Charsets.UTF_8);
        PersistableState state = new Gson().fromJson(reader, PersistableState.class);

        DefaultAnnModel autoEncoder = state.annModel;

        AnnConfiguration configuration = new AnnConfiguration(autoEncoder.getConfiguration().inputDegree);
        configuration.addLayer(autoEncoder.getConfiguration().layers.get(0), autoEncoder.getConfiguration()
                .activationFunctionOfLayer(0));
        configuration.addLayer(2, ActivationFunction.SOFTMAX);
        DefaultAnnModel annModel = new DefaultAnnModel(configuration);

        Matrix aeLayer0 = autoEncoder.getLayer(0);
        Matrix newLayer0 = annModel.getLayer(0);
        for (int i = 0; i < aeLayer0.getData().length; i++) {
            for (int j = 0; j < aeLayer0.getData()[0].length; j++) {
                newLayer0.getData()[i][j] = aeLayer0.getData()[i][j];
            }
        }

        // train the new model
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"), 500);
        // ZhWikiTrainingDataProducer.WINDOWS_SIZE = 5;
        ZhWikiTrainingDataProducer producer = new ZhWikiTrainingDataProducer(dictionary);
        AnnTrainer trainer = new AnnTrainer();

        double[][] sum = new double[annModel.getLayerCount()][];
        int i = 0;
        List<Data> datas = producer.produceData(10000);
        for (Data data : datas) {
            System.out.println(++i);
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
//            AnnData annData = Helper.convertToAnnData(dictionary, instance);
//            annData.setOutput(instance.getOutput() < 0 ? new double[] {1, 0} : new double[] {0, 1});
//            double[][] output = trainer.run(annModel, annData.getInput(), sum);
//            AnnGradient annGradient =
//                    trainer.backpropagateSoftmaxLogLikelyhood(annModel, annData, output, sum, null, null);
//            annModel.update(annGradient, 0.01);
        }

        // evaluate
        int accurate = 0;
        for (Data data : datas) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
//            AnnData annData = Helper.convertToAnnData(dictionary, instance);
//            double[][] output = trainer.run(annModel, annData.getInput(), sum);
//            if ((output[1][0] > 0.5 && instance.getOutput() < 0) || output[1][0] < 0.5 && instance.getOutput() > 0) {
//                accurate++;
//            }
        }

        System.out.println(accurate);
    }
}
