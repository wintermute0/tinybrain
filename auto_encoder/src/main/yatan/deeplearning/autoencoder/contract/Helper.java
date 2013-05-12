package yatan.deeplearning.autoencoder.contract;

import java.util.Date;
import java.util.Random;

import scala.actors.threadpool.Arrays;
import yatan.ann.AnnData;
import yatan.deeplearning.wordembedding.model.WordEmbedding;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

public class Helper {
    private static final double CORRUPTION_LEVEL = 0.25;
    private static final Random RANDOM = new Random(new Date().getTime());

    private Helper() {
    }

    public static AnnData convertToAnnData(WordEmbedding wordEmbedding, WordEmbeddingTrainingInstance instance) {
        double[] data = wordEmbedding.lookup(instance.getInput());
        double[] corruptedData = Arrays.copyOf(data, data.length);

        return new AnnData(corruptedData, data);
    }

    public static void corruptWithMask(double[] data) {
        for (int i = 0; i < data.length; i++) {
            if (RANDOM.nextDouble() < CORRUPTION_LEVEL) {
                data[i] = 0;
            }
        }
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
}
