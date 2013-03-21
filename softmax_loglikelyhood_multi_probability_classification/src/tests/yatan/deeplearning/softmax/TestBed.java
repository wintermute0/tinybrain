package yatan.deeplearning.softmax;

import java.io.File;

import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import yatan.deeplearning.softmax.data.TaggedSentence;
import yatan.deeplearning.softmax.data.parser.ICWB2Parser;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer;
import yatan.deeplearning.softmax.data.producer.WordSegmentationDataProducer.WordSegmentationInstancePool;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;

public class TestBed {
    @Test
    public void test1() {
        Multiset<String> words = HashMultiset.create();
        for (TaggedSentence sentence : DataCenter.dataset().getSentences()) {
            for (String word : sentence.words()) {
                for (int i = 0; i < word.length(); i++) {
                    words.add(String.valueOf(word.charAt(i)));
                }
            }
        }

        int i = 1;
        long total = 0;
        for (String word : words.elementSet()) {
            total += words.count(word);
        }

        for (String word : Multisets.copyHighestCountFirst(words).elementSet()) {
            System.out.println(word + ": " + words.count(word) * 1.0 / total);
        }
    }

    @Test
    public void testInstanceProducer() throws Exception {
        for (TaggedSentence sentence : new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8"))
                .getSentences()) {
            System.out.println(sentence);
        }
    }

    @Test
    public void testWordEmbeddingInstanceProducer() throws Exception {
        Dictionary dictionary = Dictionary.create(new File("test_files/zh_dict.txt"));
        for (TaggedSentence sentence : new ICWB2Parser().parse(new File("data/icwb2-data/gold/pku_test_gold.utf8"))
                .getSentences()) {
            for (WordEmbeddingTrainingInstance instance : WordSegmentationInstancePool
                    .convertTaggedSentenceToWordEmbeddingTrainingInstance(dictionary, sentence)) {
                for (int index : instance.getInput()) {
                    System.out.print(dictionary.words().get(index) + " ");
                }
                System.out.println();
            }
        }
    }
}
