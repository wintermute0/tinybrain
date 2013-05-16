package yatan.deeplearning.autoencoder.contract;

import java.util.Date;
import java.util.Random;

import scala.actors.threadpool.Arrays;
import yatan.ann.AnnData;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

public class Helper {
    private static final double CORRUPTION_LEVEL = 0.2;
    private static final Random RANDOM = new Random(new Date().getTime());

    private Helper() {
    }

    public static AnnData convertToAnnData(WordEmbedding wordEmbedding, WordEmbeddingTrainingInstance instance) {
        double[] data = wordEmbedding.lookup(instance.getInput());
        double[] corruptedData = Arrays.copyOf(data, data.length);

        return new AnnData(corruptedData, data);
    }

    public static boolean[] corruptWithMask(double[] data) {
        boolean[] mask = new boolean[data.length];
        for (int i = 0; i < data.length; i++) {
            if (RANDOM.nextDouble() < CORRUPTION_LEVEL) {
                data[i] = 0;
                mask[i] = true;
            }
        }

        return mask;
    }

    public static void corruptWithSaltAndPepper(double[] data) {
        for (int i = 0; i < data.length; i++) {
            if (RANDOM.nextDouble() < CORRUPTION_LEVEL) {
                data[i] = RANDOM.nextBoolean() ? 1 : -1;
            }
        }
    }

    public static void corruptCenterWord(double[] data) {
        for (int i = 2 * 50; i < 3 * 50; i++) {
            data[i] = 0;
        }
    }

    public static boolean[] corruptRandomWord(double[] data) {
        boolean[] mask = new boolean[data.length];
        int word = RANDOM.nextInt(5);
        for (int i = word * 50; i < (word + 1) * 50; i++) {
            data[i] = 0;
            mask[i] = true;
        }

        return mask;
    }
}
