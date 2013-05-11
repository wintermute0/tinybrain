package yatan.deeplearning.wordembedding.utility;

import java.text.DecimalFormat;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import yatan.ann.AnnModel;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.wordembedding.model.WordEmbedding;

public class LogUtility {
    private LogUtility() {
    }

    public static void logAnnModel(Logger logger, AnnModel annModel) {
        logger.info("Ann model " + annModel + " statistics:");
        for (int layer = 0; layer < annModel.getLayerCount(); layer++) {
            logger.info("Layer " + layer + "[" + buildLogString(annModel.getLayer(layer)) + "]");
        }
    }

    public static void logWordEmbedding(Logger logger, WordEmbedding wordEmbedding) {
        logger.info("Word Embedding " + "[" + buildLogString(wordEmbedding.getMatrix()) + "]");
    }

    public static void logWordEmbedding(Logger logger, WordEmbedding wordEmbedding, String word) {
        StringBuilder sb = new StringBuilder();
        int index = wordEmbedding.indexOf(word);
        DecimalFormat format = new DecimalFormat("#.##");
        for (int i = 0; i < wordEmbedding.getWordVectorSize(); i++) {
            sb.append(format.format(wordEmbedding.getMatrix().getData()[i][index])).append(" ");
        }
        logger.info(sb.toString());
    }

    public static String buildLogString(double[][] data) {
        double totalW = 0;
        double totalAbsW = 0;
        double maxW = Double.MIN_VALUE;
        double minW = Double.MAX_VALUE;

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (data[i][j] > maxW) {
                    maxW = data[i][j];
                }
                if (data[i][j] < minW) {
                    minW = data[i][j];
                }

                totalAbsW += Math.abs(data[i][j]);
                totalW += data[i][j];
            }
        }

        return MessageFormat
                .format("average abs = {0,number,#.##}, min = {1,number,#.##}, max = {2,number,#.##}, average = {3,number,#.##}",
                        (totalAbsW / (data.length * data[0].length)), minW, maxW,
                        (totalW / (data.length * data[0].length)));
    }

    public static String buildLogString(Matrix matrix) {
        return buildLogString(matrix.getData());
    }
}
