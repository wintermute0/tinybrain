package yatan.deeplearning;

import java.io.File;

import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import yatan.deeplearning.wordembedding.model.Dictionary;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

public class DictionaryBuilder {
    public static void main(String[] args) throws Exception {
        final Multiset<String> dictionary = HashMultiset.create();

        buildDict(dictionary, "data/icwb2-data/training/pku_training.utf8");
        buildDict(dictionary, "data/icwb2-data/training/msr_training.utf8");

        List<String> sortedWord = Lists.newArrayList(dictionary.elementSet());
        Collections.sort(sortedWord, new Comparator<String>() {
            @Override
            public int compare(String arg0, String arg1) {
                return dictionary.count(arg1) - dictionary.count(arg0);
            }
        });
        for (String w : sortedWord) {
            System.out.println(w + ", " + 1.0 * dictionary.count(w) / dictionary.size());
        }
    }

    private static void buildDict(Multiset<String> dictionary, String file) throws Exception {
        FileReader reader = new FileReader(new File(file));
        int word = reader.read();
        while (word != -1) {
            char ch = (char) word;
            if (!Character.isWhitespace(ch)) {
                if (Character.isDigit(ch)) {
                    dictionary.add(Dictionary.NUMBER_WORD);
                } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                    dictionary.add(Dictionary.EN_WORD);
                } else {
                    dictionary.add(String.valueOf(ch));
                }
            }

            word = reader.read();
        }
        reader.close();
    }
}
