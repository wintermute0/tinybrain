package yatan.commons.metrics;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

public class SingleValueMetrics {
    private static final Logger LOGGER = Logger.getLogger(SingleValueMetrics.class);

    private static final Map<String, SingleValueMetrics> SINGLE_VALUE_METRICS = Maps.newHashMap();

    private final String name;
    private long lastReportTime = System.currentTimeMillis();

    private double min;
    private double max;
    private double total;
    private double absTotal;
    private int count;

    public static SingleValueMetrics metrics(String name) {
        if (SINGLE_VALUE_METRICS.containsKey(name)) {
            return SINGLE_VALUE_METRICS.get(name);
        }

        SingleValueMetrics metrics = new SingleValueMetrics(name);
        SINGLE_VALUE_METRICS.put(name, metrics);

        return metrics;
    }

    public SingleValueMetrics(String name) {
        this.name = name;
    }

    public void reset() {
        this.min = Double.MAX_VALUE;
        this.max = Double.MIN_VALUE;
        this.total = 0;
        this.absTotal = 0;
        this.count = 0;
    }

    public void value(double value) {
        if (value < this.min) {
            this.min = value;
        }
        if (value > this.max) {
            this.max = value;
        }
        this.total += value;
        this.absTotal += Math.abs(value);
        this.count++;

        long now = System.currentTimeMillis();
        if (now - this.lastReportTime >= 60 * 1000) {
            this.lastReportTime = now;

            report();
            reset();
        }
    }

    public void report() {
        String message =
                MessageFormat
                        .format("average abs = {0,number,#.##}, min = {1,number,#.##}, max = {2,number,#.##}, average = {3,number,#.##}",
                                this.absTotal / this.count, this.min, this.max, this.total / this.count);
        LOGGER.info(this.name + " (rolling statistics): " + message);
    }
}
