package yatan.deeplearning.softmax;

import java.io.File;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.softmax.data.TaggedSentenceDataset;
import yatan.deeplearning.softmax.data.parser.ICWB2Parser;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

public class WordEmbeddingEvaluator {
    private static final Logger LOGGER = Logger.getLogger(WordEmbeddingEvaluator.class);

    public static void main(String[] args) throws Exception {
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"));

        // 1365269964401(100 word embedding).json
        String wordEmbeddingFile = "softmax_model_1367787124327.json";

        Object[] models = loadWordEmbeddingFromFile(new File("test_files/results/" + wordEmbeddingFile));
        if (models.length == 0) {
            LOGGER.info("No word embedding found.");
            return;
        }

        WordEmbedding wordEmbedding = (WordEmbedding) models[0];
        DefaultAnnModel annModel = (DefaultAnnModel) models[1];

        double crossEntropy = calculateCrossEntropy(dictionary, wordEmbedding, annModel);
        LOGGER.info("CrossEntropy: " + crossEntropy);
    }

    /**
     * @param dictionary
     * @param wordEmbedding
     * @param annModel
     * @return
     * @throws Exception
     */
    private static double calculateCrossEntropy(Dictionary dictionary, WordEmbedding wordEmbedding,
            DefaultAnnModel annModel) throws Exception {
        AnnTrainer trainer = new AnnTrainer();
        double[] outputs = new double[wordEmbedding.getDictionary().size()];
        double outputSum = 0;
        double hT = 0;
        int instanceCount = 0;
        TaggedSentenceDataset dataset = new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8"));

        int excessiveSmallRankCount = 0;

        for (WordEmbeddingTrainingInstance instance : new WordSegmentationInstancePool(dictionary, dataset, false)
                .getInstances().subList(0, 1000)) {
            List<Integer> outputRank = Lists.newArrayList();
            if (instance.getOutput() < 0) {
                // ignore negative case
                continue;
            }

            instanceCount++;

            outputSum = 0;
            double actualWordOutput = 0;

            int possibleWordCount = 0;
            int actualWordIndex = instance.getInput().get(instance.getInput().size() / 2);
            for (int i = 0; i < wordEmbedding.getDictionary().size(); i++) {
                instance.getInput().set(instance.getInput().size() / 2, i);
                double output = runWordEmbeddingInstance(wordEmbedding, annModel, trainer, instance);
                if (i == actualWordIndex) {
                    actualWordOutput = output;
                }

                outputs[i] = output;
                outputSum += output;

                if (output > 0.5) {
                    possibleWordCount++;
                }

                int rank = 0;
                while (rank < outputRank.size() && output < outputs[outputRank.get(rank)]) {
                    rank++;
                }
                outputRank.add(rank, i);
            }

            int rank = outputRank.indexOf(actualWordIndex);
            System.out.print(dictionary.words().get(actualWordIndex) + ", Possible words: " + possibleWordCount
                    + ". Actual work rank = " + rank + ". " + "Actual word output = " + actualWordOutput);

            if (rank > 4000 || "$NUM$".equals(dictionary.words().get(actualWordIndex))) {
                excessiveSmallRankCount++;
                System.out.println(dictionary.words().get(actualWordIndex) + ": Ignore excessive small rank, total "
                        + excessiveSmallRankCount);
            } else {
                double pw = outputs[actualWordIndex] / outputSum;
                System.out.print(instanceCount + ": P(w) = " + pw + ". ");
                hT += Math.log(pw) / Math.log(2);
                double ce = (-1.0 / instanceCount * hT);
                System.out.println("Cross entropy = " + ce + ". Perperlexity = " + Math.pow(2, ce));
            }
        }

        return -1.0 / instanceCount * hT;
    }

    private static double runWordEmbeddingInstance(WordEmbedding wordEmbedding, DefaultAnnModel annModel,
            AnnTrainer trainer, WordEmbeddingTrainingInstance instance) {
        // first convert input data into word embedding
        // FIXME: could reuse an array, no need to allocate it every time
        double[] input = new double[instance.getInput().size() * wordEmbedding.getWordVectorSize()];
        for (int i = 0; i < instance.getInput().size(); i++) {
            int index = instance.getInput().get(i);
            for (int j = 0; j < wordEmbedding.getWordVectorSize(); j++) {
                input[i * wordEmbedding.getWordVectorSize() + j] = wordEmbedding.getMatrix().getData()[j][index];
            }
        }

        double[][] output = trainer.run(annModel, input, new double[annModel.getLayerCount()][]);

        return output[annModel.getLayerCount() - 1][1];
    }

    private static Object[] loadWordEmbedding() {
        File modelFolder = new File("test_files/results/");
        if (modelFolder.isDirectory()) {
            List<File> modelFiles = ImmutableList.copyOf(modelFolder.listFiles());
            modelFiles = Lists.newArrayList(Collections2.filter(modelFiles, new Predicate<File>() {
                public boolean apply(File file) {
                    return file.isFile() && Files.getFileExtension(file.getName()).equals("json");
                }
            }));

            if (!modelFiles.isEmpty()) {
                Collections.sort(modelFiles, new Comparator<File>() {
                    @Override
                    public int compare(File arg0, File arg1) {
                        return -arg0.getName().compareTo(arg1.getName());
                    }
                });
                File file = modelFiles.get(0);
                return loadWordEmbeddingFromFile(file);
            }
        }

        return null;
    }

    private static Object[] loadWordEmbeddingFromFile(File file) {
        LOGGER.info("Load word embedding from file " + file);
        try {
            String text = Files.toString(file, Charsets.UTF_8);
            JsonElement jsonElement = new JsonParser().parse(text);
            Gson gson = new Gson();
            WordEmbedding wordEmbedding =
                    gson.fromJson(jsonElement.getAsJsonObject().get("wordEmbedding"), WordEmbedding.class);
            DefaultAnnModel annModel =
                    gson.fromJson(jsonElement.getAsJsonObject().get("annModel"), DefaultAnnModel.class);

            return new Object[] {wordEmbedding, annModel};
        } catch (IOException e) {
            LOGGER.error("Error occurred while loading word embedding from file.", e);
        }

        return null;
    }
}
