package yatan.commons.metrics;

import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

public class SimpleMetrics {
    private static final Logger LOGGER = Logger.getLogger(SimpleMetrics.class);
    private static final Map<String, Long> LAST_REPORT_TIME = Maps.newHashMap();

    public static void value(String key, Double value) {
        long now = System.currentTimeMillis();
        Long lastReportTime = LAST_REPORT_TIME.get(key);
        if (lastReportTime == null || (now - lastReportTime) > 15 * 1000) {
            LOGGER.info(key + " = " + value);
            LAST_REPORT_TIME.put(key, now);
        }
    }
}
