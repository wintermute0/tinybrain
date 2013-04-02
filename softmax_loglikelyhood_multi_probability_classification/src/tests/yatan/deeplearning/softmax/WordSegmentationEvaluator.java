package yatan.deeplearning.softmax;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import scala.actors.threadpool.Arrays;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.softmax.data.TaggedSentenceDataset;
import yatan.deeplearning.softmax.data.parser.ICWB2Parser;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WordSegmentationEvaluator {
    public static void main(String[] args) throws Exception {
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"));
        TaggedSentenceDataset dataset = new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8"));
        Object[] model = loadState();
        WordEmbedding wordEmbedding = (WordEmbedding) model[0];
        AnnModel annModel = (AnnModel) model[1];
        AnnTrainer trainer = new AnnTrainer();

        WordSegmentationInstancePool instancePool = new WordSegmentationInstancePool(dictionary, dataset, false);
        int accurateCount = 0;
        int count = 0;
        for (WordEmbeddingTrainingInstance instance : instancePool.getInstances()) {
            // first convert input data into word embedding
            // FIXME: could reuse an array, no need to allocate it every time
            double[] input = new double[instance.getInput().size() * wordEmbedding.getWordVectorSize()];
            for (int i = 0; i < instance.getInput().size(); i++) {
                int index = instance.getInput().get(i);
                for (int j = 0; j < wordEmbedding.getWordVectorSize(); j++) {
                    input[i * wordEmbedding.getWordVectorSize() + j] = wordEmbedding.getMatrix().getData()[j][index];
                }
            }

            AnnData annData = new AnnData(input, new double[] {instance.getOutput()});

            // train with this ann data instance and update gradient
            double[][] output = trainer.run(annModel, annData.getInput(), new double[annModel.getLayerCount()][]);
            // System.out.println(output[annModel.getLayerCount() - 1][0]);
            boolean accurate = true;
            // System.out.println(instance.getOutput() + ": " + Arrays.toString(output[annModel.getLayerCount() - 1]));
            for (int i = 0; i < output[annModel.getLayerCount() - 1].length; i++) {
                if (i == instance.getOutput()) {
                    continue;
                }

                double tagProbability = output[annModel.getLayerCount() - 1][i];
                if (tagProbability > output[annModel.getLayerCount() - 1][instance.getOutput()]) {
                    System.out.println("Expected: " + WordSegmentationInstancePool.TAGS.get(instance.getOutput())
                            + " = " + output[annModel.getLayerCount() - 1][instance.getOutput()] + ", Actual: "
                            + Arrays.toString(output[annModel.getLayerCount() - 1]));
                    accurate = false;
                    break;
                }
            }

            count++;
            if (accurate) {
                accurateCount++;
            }

            // System.out.println(count + ": " + 100.0 * accurateCount / count + "%");
        }
    }

    protected static Object[] loadState() throws Exception {
        System.out.println("Trying to find persisted parameter server state...");
        File stateFile = null;
        for (File file : new File("test_files/results").listFiles()) {
            if (file.isFile() && Files.getFileExtension(file.getName()).equals("json")
                    && (stateFile == null || stateFile.getName().compareTo(file.getName()) < 0)) {
                stateFile = file;
            }
        }

        if (stateFile != null) {
            System.out.println("Loading parameter server state from " + stateFile + "...");
            FileInputStream is = null;
            InputStreamReader reader = null;
            try {
                is = new FileInputStream(stateFile);
                reader = new InputStreamReader(is, Charsets.UTF_8);
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                WordEmbedding wordEmbedding = new Gson().fromJson(json.get("wordEmbedding"), WordEmbedding.class);
                AnnModel annModel = new Gson().fromJson(json.get("annModel"), AnnModel.class);

                return new Object[] {wordEmbedding, annModel};
            } finally {
                close(reader, is);
            }
        }

        return null;
    }

    protected static void close(Closeable... closeables) {
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
}
