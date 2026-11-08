package pl.shkumat.airquality.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local anomaly: the sensor is unusual versus its own recent values (high Z-score)
 * and versus its neighbours (value much higher than their median).
 * Window length and thresholds are settings, so they can be tuned later.
 */
@Component
public class AnomalyDetector {

    private final int minSamples;
    private final double zThreshold;
    private final double backgroundFactor;

    public AnomalyDetector(@Value("${app.detection.min-samples}") int minSamples,
                           @Value("${app.detection.z-threshold}") double zThreshold,
                           @Value("${app.detection.background-factor}") double backgroundFactor) {
        this.minSamples = minSamples;
        this.zThreshold = zThreshold;
        this.backgroundFactor = backgroundFactor;
    }

    /**
     * @param previous valid PM2.5 values of this sensor in the window, not including current
     * @param background median PM2.5 of neighbouring sensors, or null if there are too few
     * @return the scores, or null if there is not enough data to decide
     */
    public Result detect(double current, List<Double> previous, Double background) {
        if (previous.size() < minSamples) {
            return null;
        }
        double mean = mean(previous);
        double stdDev = stdDev(previous, mean);
        // all values in the window were the same, Z-score is not defined
        if (stdDev == 0) {
            return null;
        }
        double zScore = (current - mean) / stdDev;
        boolean local = zScore >= zThreshold
                && background != null
                && current > background
                && current >= background * backgroundFactor;
        return new Result(local, zScore, mean, stdDev, background);
    }

    public static Double median(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    private double mean(List<Double> values) {
        double sum = 0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    // sample standard deviation (n - 1)
    private double stdDev(List<Double> values, double mean) {
        double sum = 0;
        for (Double value : values) {
            double d = value - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / (values.size() - 1));
    }

    public record Result(boolean local, double zScore, double mean, double stdDev, Double background) {
    }
}
