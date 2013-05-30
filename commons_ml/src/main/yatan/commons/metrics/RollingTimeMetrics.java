package yatan.commons.metrics;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RollingTimeMetrics {
    private static final Map<String, RollingTimeMetrics> metrics = Maps.newHashMap();

    private final List<Long> TIMESTAMPS = Lists.newArrayList();
    private final List<Double> VALUES = Lists.newArrayList();

    private final long windowInMilisenconds;

    public RollingTimeMetrics(long windowInMilisenconds) {
        this.windowInMilisenconds = windowInMilisenconds;
    }

    public RollingTimeMetrics get(String key) {
        Preconditions.checkArgument(metrics.containsKey(key));

        return metrics.get(key);
    }

    public RollingTimeMetrics get(String key, long windowInMilisenconds) {
        if (metrics.containsKey(key)) {
            return get(key);
        }

        RollingTimeMetrics rollingTimeMetrics = new RollingTimeMetrics(this.windowInMilisenconds);
        metrics.put(key, rollingTimeMetrics);

        return rollingTimeMetrics;
    }

    public void add(double value) {
        removeOutOfFrameTime();

        TIMESTAMPS.add(System.currentTimeMillis());
        VALUES.add(value);
    }

    public double total() {
        removeOutOfFrameTime();

        double total = 0;
        for (Double value : VALUES) {
            total += value;
        }

        return total;
    }

    private void removeOutOfFrameTime() {
        long now = System.currentTimeMillis();
        int maxIndexToRemove = -1;
        for (int i = 0; i < TIMESTAMPS.size(); i++) {
            if (now - TIMESTAMPS.get(i) > this.windowInMilisenconds) {
                maxIndexToRemove++;
            } else {
                break;
            }
        }

        for (int i = 0; i <= maxIndexToRemove; i++) {
            TIMESTAMPS.remove(0);
            VALUES.remove(0);
        }
    }
}
