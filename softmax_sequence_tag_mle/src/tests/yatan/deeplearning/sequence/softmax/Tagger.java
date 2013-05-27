package yatan.deeplearning.sequence.softmax;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;
import yatan.ann.DefaultAnnModel;
import yatan.data.parser.bakeoff2005.ICWB2Parser;
import yatan.data.sequence.TaggedSentence;
import yatan.data.sequence.TaggedSentenceDataset;
import yatan.deeplearning.sequence.softmax.contract.CRFANNEvaluatorContractImpl;
import yatan.deeplearning.sequence.softmax.contract.CRFANNParameterActorContractImpl.PersistableState;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class Tagger {
    private static final String MODEL_FOLDER = "test_files/results/";

    private static List<List<String>> segementedWords = Lists.newArrayList();

    public static void main(String[] args) throws Exception {
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict_better.txt"));
        TaggedSentenceDataset dataset = new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8"));
        PersistableState state = loadState();
        WordEmbedding wordEmbedding = state.wordEmbedding;
        DefaultAnnModel annModel = state.annModel;

        // TaggedSentence taggedSentence = new TaggedSentence();
        // taggedSentence.addWord("工", "B");
        // taggedSentence.addWord("程", "L");
        // taggedSentence.addWord("工", "B");
        // taggedSentence.addWord("地", "L");
        // viterbi(taggedSentence, dictionary, wordEmbedding, annModel, trainer);

        int i = 0;
        for (TaggedSentence sentence : dataset.getSentences()) {
            // tag(sentence, dictionary, wordEmbedding, annModel, trainer, writer);
            viterbi(sentence, dictionary, wordEmbedding, annModel, state.tagTransitionWeights);
            System.out.println(++i * 100.0 / dataset.getSentences().size());
        }

        System.out.println("Post processing...");

        System.out.println("Output...");
        Writer writer = new BufferedWriter(new FileWriterWithEncoding("test_files/tag_result.txt", Charsets.UTF_8));
        output(writer);
        writer.close();
        System.out.println("Done.");
    }

    private static PersistableState loadState() throws Exception {
        System.out.println("Trying to find persisted parameter server state...");
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
            System.out.println("Loading parameter server state from " + stateFile + "...");
            FileInputStream is = null;
            InputStreamReader reader = null;
            is = new FileInputStream(stateFile);
            reader = new InputStreamReader(is, Charsets.UTF_8);
            return new Gson().fromJson(reader, PersistableState.class);
        }

        return null;
    }

    private static void viterbi(TaggedSentence sentence, Dictionary dictionary, WordEmbedding wordEmbedding,
            DefaultAnnModel annModel, double[][] tagTransitionWeights) throws Exception {
        List<String> originalWords = Lists.newArrayList();
        List<WordEmbeddingTrainingInstance> list =
                WordSegmentationInstancePool.convertTaggedSentenceToWordEmbeddingTrainingInstance(dictionary, sentence,
                        originalWords);

        List<String> words = Lists.newArrayList();
        int originalWordIndex = 0;
        for (List<WordEmbeddingTrainingInstance> instances : WordSegmentationInstancePool.breakSentences(dictionary,
                list)) {
            int[] tagIndices =
                    CRFANNEvaluatorContractImpl.tag(wordEmbedding, annModel, tagTransitionWeights, instances,
                            WordSegmentationInstancePool.TAGS.size());

            if (!CRFANNEvaluatorContractImpl.isTagsValid(tagIndices)) {
                System.out.println(originalWords);
            }

            // write out tag
            StringBuilder wordBuilder = new StringBuilder();
            for (int i = 0; i < tagIndices.length; i++) {
                wordBuilder.append(originalWords.get(originalWordIndex++));
                String tag = WordSegmentationInstancePool.TAGS.get(tagIndices[i]);
                if (tag.equals("L") || tag.equals("U")) {
                    // got a new word
                    String word = wordBuilder.toString();
                    words.add(word);

                    // reset word builder
                    wordBuilder = new StringBuilder();
                }
            }
        }

        segementedWords.add(words);
    }

    private static void output(Writer writer) throws IOException {
        for (List<String> words : segementedWords) {
            for (String word : words) {
                // write out words
                writer.write(word);
                writer.write("  ");
            }
            writer.write("\n");
        }
    }
}
