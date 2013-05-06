package yatan.deeplearning.autoencoder;

import java.util.List;

import com.google.inject.Inject;

import yatan.ann.AnnData;
import yatan.ann.AnnGradient;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.autoencoder.data.chinesewordembedding.ChineseWordEmbeddingAutoEncoderDataProvider;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.Parameter;
import yatan.distributedcomputer.contract.impl.AbstractComputeActorContractImpl;

public class AutoEncoderTrainingContractImpl extends AbstractComputeActorContractImpl {
    @Inject
    private Dictionary dictionary;

    @Override
    protected int requestDataSize() {
        return 100;
    }

    @Override
    protected ComputeResult doCompute(List<Data> dataset, Parameter parameter) {
        AnnModel annModel = (AnnModel) ((Object[]) parameter.getSerializable())[0];

        AnnTrainer trainer = new AnnTrainer();
        AnnGradient batchGradient = 
        double[][] sum = new double[annModel.getLayerCount()][];
        for (Data data : dataset) {
            WordEmbeddingTrainingInstance instance = (WordEmbeddingTrainingInstance) data.getSerializable();
            double[] annInput =
                    new double[ChineseWordEmbeddingAutoEncoderDataProvider.WINDOWS_SIZE * this.dictionary.size()];
            for (int i = 0; i < instance.getInput().size(); i++) {
                annInput[i * this.dictionary.size() + instance.getInput().get(i)] = 1;
            }

            double[][] output = trainer.run(annModel, annInput, sum);
            AnnGradient annGradient =
                    trainer.backpropagateLeastSqure(annModel, new AnnData(annInput, annInput), output, sum);
        }
    }
}
