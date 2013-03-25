package yatan.deeplearning.softmax;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import yatan.ann.AnnData;
import yatan.ann.AnnModel;
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
        AnnModel annModel = (AnnModel) model[1];
        AnnTrainer trainer = new AnnTrainer();

        for (TaggedSentence sentence : dataset.getSentences()) {
            tag(sentence, dictionary, wordEmbedding, annModel, trainer);
        }
    }

    private static void tag(TaggedSentence sentence, Dictionary dictionary, WordEmbedding wordEmbedding,
            AnnModel annModel, AnnTrainer trainer) {
        List<WordEmbeddingTrainingInstance> instances =
                WordSegmentationInstancePool.convertTaggedSentenceToWordEmbeddingTrainingInstance(dictionary, sentence);
        List<String> tags = Lists.newArrayList();
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
            String ch = dictionary.words().get(instance.getInput().get(instance.getInput().size() / 2));
            if (ch.equals(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)) {
                ch = "*";
            }
            System.out.print(ch);
            if (tag.equals("L") || tag.equals("U")) {
                System.out.print("  ");
            }
        }

        System.out.println();
    }
}
