package yatan.deeplearning.softmax;

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;

import scala.actors.threadpool.Arrays;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import yatan.ann.AnnData;
import yatan.ann.DefaultAnnModel;
import yatan.ann.AnnTrainer;
import yatan.deeplearning.softmax.data.TaggedSentence;
import yatan.deeplearning.softmax.data.TaggedSentenceDataset;
import yatan.deeplearning.softmax.data.parser.ICWB2Parser;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

public class WordSegmentationTagger {
    public static void main(String[] args) throws Exception {
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"));
        TaggedSentenceDataset dataset = new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8"));
        Object[] model = WordSegmentationEvaluator.loadState();
        WordEmbedding wordEmbedding = (WordEmbedding) model[0];
        DefaultAnnModel annModel = (DefaultAnnModel) model[1];
        AnnTrainer trainer = new AnnTrainer();

        Writer writer = new BufferedWriter(new FileWriterWithEncoding("test_files/tag_result.txt", Charsets.UTF_8));
        int i = 0;
        for (TaggedSentence sentence : dataset.getSentences()) {
            // tag(sentence, dictionary, wordEmbedding, annModel, trainer, writer);
            viterbi(sentence, dictionary, wordEmbedding, annModel, trainer, writer);
            System.out.println(++i * 100.0 / dataset.getSentences().size());
        }
        writer.close();
    }

    private static void tag(TaggedSentence sentence, Dictionary dictionary, WordEmbedding wordEmbedding,
            DefaultAnnModel annModel, AnnTrainer trainer, Writer writer) throws Exception {
        List<String> originalWords = Lists.newArrayList();
        List<WordEmbeddingTrainingInstance> instances =
                WordSegmentationInstancePool.convertTaggedSentenceToWordEmbeddingTrainingInstance(dictionary, sentence,
                        originalWords);
        List<String> tags = Lists.newArrayList();

        int characterIndex = 0;
        for (WordEmbeddingTrainingInstance instance : instances) {
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
            final double[] tagProbability = output[annModel.getLayerCount() - 1];
            List<String> availableTags = Lists.newArrayList(WordSegmentationInstancePool.TAGS);
            Collections.sort(availableTags, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    int i1 = WordSegmentationInstancePool.TAGS.indexOf(o1);
                    int i2 = WordSegmentationInstancePool.TAGS.indexOf(o2);

                    return tagProbability[i2] - tagProbability[i1] > 0 ? 1 : -1;
                }
            });
            String lastTag = tags.isEmpty() ? null : Lists.reverse(tags).get(0);
            if (lastTag == null) {
                availableTags.remove("L");
                availableTags.remove("I");
            } else if (lastTag.equals("B")) {
                availableTags.remove("U");
                availableTags.remove("B");
            } else if (lastTag.equals("I")) {
                availableTags.remove("U");
                availableTags.remove("B");
            } else if (lastTag.equals("L")) {
                availableTags.remove("L");
            }

            String tag = availableTags.get(0);
            tags.add(tag);
            // String ch = dictionary.words().get(instance.getInput().get(instance.getInput().size() / 2));
            // if (ch.equals(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)) {
            // ch = "*";
            // }
            // System.out.print(ch);
            writer.write(originalWords.get(characterIndex++));
            if (tag.equals("L") || tag.equals("U")) {
                writer.write("  ");
            }
        }

        writer.write("\n");
    }

    private static void viterbi(TaggedSentence sentence, Dictionary dictionary, WordEmbedding wordEmbedding,
            DefaultAnnModel annModel, AnnTrainer trainer, Writer writer) throws Exception {
        List<String> originalWords = Lists.newArrayList();
        List<WordEmbeddingTrainingInstance> instances =
                WordSegmentationInstancePool.convertTaggedSentenceToWordEmbeddingTrainingInstance(dictionary, sentence,
                        originalWords);
        double[][] sequenceProbability = new double[instances.size()][WordSegmentationInstancePool.TAGS.size()];
        int[][] sequenceTagTrace = new int[instances.size()][WordSegmentationInstancePool.TAGS.size()];

        int wordIndex = 0;
        for (WordEmbeddingTrainingInstance instance : instances) {
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
            final double[] tagProbability = output[annModel.getLayerCount() - 1];

            // update by DP
            if (wordIndex == 0) {
                sequenceProbability[0] = Arrays.copyOf(tagProbability, tagProbability.length);
            } else {
                // last tag is U
                updateDP(0, new String[] {"B", "U"}, wordIndex, sequenceProbability, sequenceTagTrace, tagProbability);
                // last tag is B
                updateDP(1, new String[] {"I", "L"}, wordIndex, sequenceProbability, sequenceTagTrace, tagProbability);
                // last tag is L
                updateDP(2, new String[] {"B", "U"}, wordIndex, sequenceProbability, sequenceTagTrace, tagProbability);
                // last tag is I
                updateDP(3, new String[] {"I", "L"}, wordIndex, sequenceProbability, sequenceTagTrace, tagProbability);
            }

            wordIndex++;
        }

        // trace back
        int tagIndex = 0;
        double maxProbabilty = 0;
        int i = sequenceProbability.length - 1;
        for (int j = 0; j < sequenceProbability[i].length; j++) {
            if (sequenceProbability[i][j] > maxProbabilty) {
                tagIndex = j;
                maxProbabilty = sequenceProbability[i][j];
            }
        }

        List<Integer> tagIndices = Lists.newArrayList();
        for (i = sequenceTagTrace.length - 1; i >= 0; i--) {
            tagIndices.add(0, tagIndex);
            tagIndex = sequenceTagTrace[i][tagIndex];
        }

        // write out tag
        for (i = 0; i < tagIndices.size(); i++) {
            writer.write(originalWords.get(i));
            String tag = WordSegmentationInstancePool.TAGS.get(tagIndices.get(i));
            if (tag.equals("L") || tag.equals("U")) {
                writer.write("  ");
            }
        }

        writer.write("\n");
    }

    private static void updateDP(int lastTagIndex, String[] allowedTags, int wordIndex, double[][] sequenceProbability,
            int[][] sequenceTagTrace, double[] tagProbabilty) {
        for (int currentTagIndex = 0; currentTagIndex < WordSegmentationInstancePool.TAGS.size(); currentTagIndex++) {
            String tag = WordSegmentationInstancePool.TAGS.get(currentTagIndex);
            if (!Arrays.asList(allowedTags).contains(tag)) {
                // tag not allowed
                continue;
            }

            if (sequenceProbability[wordIndex - 1][lastTagIndex] * tagProbabilty[currentTagIndex] > sequenceProbability[wordIndex][currentTagIndex]) {
                sequenceProbability[wordIndex][currentTagIndex] =
                        sequenceProbability[wordIndex - 1][lastTagIndex] * tagProbabilty[currentTagIndex];
                sequenceTagTrace[wordIndex][currentTagIndex] = lastTagIndex;
            }
        }
    }
}
