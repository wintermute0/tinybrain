package yatan.deeplearning.wordembedding.model;

import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import yatan.common.CheckUtility;
import yatan.commons.matrix.Matrix;

public class WordEmbedding implements Serializable {
    private static final long serialVersionUID = -4191565458443082675L;
    // private final Logger logger = Logger.getLogger(WordEmbedding.class);
    private final List<String> dictionary;
    private final Matrix matrix;
    private final int wordVectorSize;

    public WordEmbedding(List<String> dictionary, int wordVectorSize) {
        CheckUtility.notNull(dictionary, "dictionary");
        CheckUtility.isPositive(wordVectorSize, "wordVectorSize");

        // this.logger.info("Create word embedding with dictionary size: " + dictionary.size()
        // + ", and word vector size: " + wordVectorSize);

        this.dictionary = dictionary;
        this.matrix = new Matrix(wordVectorSize, dictionary.size());
        this.matrix.randomInitialize();
        this.wordVectorSize = wordVectorSize;
    }

    public double[] lookup(List<Integer> wordIndecis) {
        double[] annInput = new double[wordIndecis.size() * this.wordVectorSize];
        for (int i = 0; i < wordIndecis.size(); i++) {
            int index = wordIndecis.get(i);
            for (int j = 0; j < this.wordVectorSize; j++) {
                annInput[i * this.wordVectorSize + j] = this.matrix.getData()[j][index];
            }
        }

        return annInput;
    }

    public int indexOf(String word) {
        CheckUtility.notNull(word, "word");

        return this.dictionary.indexOf(word);
    }

    public void update(int wordIndex, Double[] delta, double learningRate) {
        if (wordIndex < 0 || wordIndex > this.dictionary.size()) {
            throw new IllegalArgumentException("WordIndex " + wordIndex + " out of bounds.");
        }
        if (delta.length != this.getWordVectorSize()) {
            throw new IllegalArgumentException("Delta size does not match.");
        }

        for (int i = 0; i < this.getWordVectorSize(); i++) {
            this.matrix.getData()[i][wordIndex] += delta[i] * learningRate;
        }
    }

    public void update(int wordIndex, Double[] delta, double lampda, Matrix wordEmbeddingDeltaSqureSum) {
        if (wordIndex < 0 || wordIndex > this.dictionary.size()) {
            throw new IllegalArgumentException("WordIndex " + wordIndex + " out of bounds.");
        }
        if (delta.length != this.getWordVectorSize()) {
            throw new IllegalArgumentException("Delta size does not match.");
        }

        for (int i = 0; i < this.getWordVectorSize(); i++) {
            if (delta[i] == 0) {
                continue;
            }

            wordEmbeddingDeltaSqureSum.getData()[i][wordIndex] += Math.pow(delta[i], 2);
            double learningRate = lampda / Math.sqrt(wordEmbeddingDeltaSqureSum.getData()[i][wordIndex]);
            this.matrix.getData()[i][wordIndex] += delta[i] * learningRate;
        }
    }

    public void update(Map<Integer, Double[]> wordEmbeddingDelta, double learningRate) {
        for (Entry<Integer, Double[]> entry : wordEmbeddingDelta.entrySet()) {
            update(entry.getKey(), entry.getValue(), learningRate);
        }
    }

    public void update(Map<Integer, Double[]> wordEmbeddingDelta, double lampda, Matrix wordEmbeddingDeltaSqureSum) {
        for (Entry<Integer, Double[]> entry : wordEmbeddingDelta.entrySet()) {
            update(entry.getKey(), entry.getValue(), lampda, wordEmbeddingDeltaSqureSum);
        }
    }

    /**
     * This is a WordVectorSize * DictionarySize matrix.
     * @return
     */
    public Matrix getMatrix() {
        return matrix;
    }

    public List<String> getDictionary() {
        return dictionary;
    }

    public int getWordVectorSize() {
        return wordVectorSize;
    }

    public double distanceBetween(int index1, int index2) {
        double distance = 0;
        for (int i = 0; i < this.wordVectorSize; i++) {
            distance += Math.pow(this.matrix.getData()[i][index1] - this.matrix.getData()[i][index2], 2);
        }

        return distance;
    }

    public double distanceBetween(String word1, String word2) {
        int index1 = this.dictionary.indexOf(word1);
        int index2 = this.dictionary.indexOf(word2);

        return distanceBetween(index1, index2);
    }

    @Override
    public String toString() {
        return "WordEmbedding [dictionary.size=" + dictionary.size() + ", wordVectorSize=" + wordVectorSize + "]";
    }
}