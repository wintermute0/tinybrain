package yatan.deeplearning.softmax.contract.metric;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

import yatan.ann.AnnGradient;
import yatan.commons.matrix.Matrix;
import yatan.deeplearning.wordembedding.utility.LogUtility;

public class GradientUpdateMetrics {
    private static int REPORT_INTERVAL_SECONDS = 60;
    private static long lastReportTime = new Date().getTime();

    private static int rollingUpdateCount;
    private static AnnGradient rollingAnnGradient;
    private static Map<Integer, Double[]> rollingWordEmbeddingDelta = Maps.newHashMap();

    private static Matrix lastWordEmbeddingDeltaSumSquare;
    private static List<Matrix> lastAnnDeltaSumSquare;

    public static void report(Logger logger, AnnGradient annGradient, Map<Integer, Double[]> wordEmbeddingDelta,
            Matrix wordEmbeddingDeltaSumSquare, List<Matrix> annDeltaSumSquare) {
        rollingUpdateCount++;

        lastWordEmbeddingDeltaSumSquare = wordEmbeddingDeltaSumSquare;
        lastAnnDeltaSumSquare = annDeltaSumSquare;

        // save ann gradient
        if (rollingAnnGradient == null) {
            rollingAnnGradient = annGradient.clone();
        } else {
            rollingAnnGradient.updateByPlus(annGradient);
        }

        // save word embedding delta
        for (Entry<Integer, Double[]> entry : wordEmbeddingDelta.entrySet()) {
            if (rollingWordEmbeddingDelta.containsKey(entry.getKey())) {
                Double[] rollingData = rollingWordEmbeddingDelta.get(entry.getKey());
                Double[] data = entry.getValue();
                for (int i = 0; i < rollingData.length; i++) {
                    rollingData[i] += data[i];
                }
            } else {
                rollingWordEmbeddingDelta.put(entry.getKey(), entry.getValue());
            }
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReportTime > REPORT_INTERVAL_SECONDS * 1000) {
            report(logger);

            // reset rolling status
            rollingUpdateCount = 0;
            rollingAnnGradient = null;
            rollingWordEmbeddingDelta.clear();

            lastReportTime = currentTime;
        }
    }

    private static void report(Logger logger) {
        // report
        logger.info("For the last " + rollingUpdateCount + " update:");
        for (int i = 0; i < rollingAnnGradient.getGradients().size(); i++) {
            logger.info("ANN layer " + i + " gradient stats: ["
                    + LogUtility.buildLogString(rollingAnnGradient.getGradients().get(i)) + "]");
            logger.info("Ann layer " + i + " delta sum square stats: ["
                    + LogUtility.buildLogString(lastAnnDeltaSumSquare.get(i)) + "]");
        }

        double[][] wordingEmbeddingDelta =
                new double[rollingWordEmbeddingDelta.size()][rollingWordEmbeddingDelta.values().iterator().next().length];
        int i = 0;
        for (Entry<Integer, Double[]> entry : rollingWordEmbeddingDelta.entrySet()) {
            Double[] data = entry.getValue();
            for (int j = 0; j < data.length; j++) {
                wordingEmbeddingDelta[i][j] += data[j];
            }
            i++;
        }
        logger.info("Word embedding gradient stats: [" + LogUtility.buildLogString(wordingEmbeddingDelta) + "]");
        logger.info("Word embedding sum square stats: [" + LogUtility.buildLogString(lastWordEmbeddingDeltaSumSquare)
                + "]");
    }
}
