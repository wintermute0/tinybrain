package yatan.deeplearning.wordembedding.data;

import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.wordembedding.model.WordEmbedding;

public class WordEmbeddingQuerier {
    public static void main(String[] args) throws Exception {
        WordEmbedding wordEmbedding = (WordEmbedding) loadWordEmbedding()[0];

        wordEmbedding = scaleWordEmbedding(wordEmbedding, 0.1);

        String query = "吴";
        int rank = 0;
        for (String word : query(wordEmbedding, query)) {
            System.out.print(rank++ + ": " + word + ": " + distanceBetween(wordEmbedding, word, query) + ", ");
            for (int i = 0; i < 10; i++) {
                System.out.print(MessageFormat.format("{0,number,#.#####}, ",
                        wordEmbedding.getMatrix().getData()[i][wordEmbedding.getDictionary().indexOf(word)]));
            }
            System.out.println();
        }
    }

    private static List<String> query(final WordEmbedding wordEmbedding, final String word) {
        List<String> words = new ArrayList<String>(wordEmbedding.getDictionary());
        Collections.sort(words, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return distanceBetween(wordEmbedding, word, o1) - distanceBetween(wordEmbedding, word, o2) > 0 ? 1 : -1;
            }
        });

        return words;
    }

    private static double distanceBetween(WordEmbedding wordEmbedding, String word1, String word2) {
        int index1 = wordEmbedding.indexOf(word1);
        int index2 = wordEmbedding.indexOf(word2);
        double distance = 0;
        for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
            distance +=
                    Math.pow(wordEmbedding.getMatrix().getData()[i][index1]
                            - wordEmbedding.getMatrix().getData()[i][index2], 2);
        }

        return distance;
    }

    private static WordEmbedding scaleWordEmbedding(WordEmbedding wordEmbedding, double sigma) {
        double total = 0;
        Matrix matrix = wordEmbedding.getMatrix();
        double[][] data = matrix.getData();
        for (int i = 0; i < matrix.rowSize(); i++) {
            for (int j = 0; j < matrix.columnSize(); j++) {
                total += data[i][j];
            }
        }

        double mean = total / (matrix.rowSize() * matrix.columnSize());
        double dv = 0;
        for (int i = 0; i < matrix.rowSize(); i++) {
            for (int j = 0; j < matrix.columnSize(); j++) {
                dv += Math.pow(data[i][j] - mean, 2);
            }
        }

        double sdv = Math.sqrt(dv);
        for (int i = 0; i < matrix.rowSize(); i++) {
            for (int j = 0; j < matrix.columnSize(); j++) {
                data[i][j] = sigma * data[i][j] / sdv;
            }
        }

        return wordEmbedding;
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
                System.out.println("Load word embedding from file " + file);
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
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
