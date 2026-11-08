package pl.shkumat.airquality.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AnomalyDetectorTests {

    // starting values from application.properties, to be tuned later
    private final AnomalyDetector detector = new AnomalyDetector(6, 3, 1.5);

    @Test
    void spikeAgainstCalmNeighboursIsLocal() {
        // μ ≈ 20, σ ≈ 1.6, x = 50 → z ≈ 19; neighbours still at 21
        AnomalyDetector.Result result = detector.detect(50, around(20, 6), 21.0);

        assertThat(result).isNotNull();
        assertThat(result.local()).isTrue();
        assertThat(result.zScore()).isGreaterThan(3);
    }

    @Test
    void cityWideRiseIsNotLocal() {
        // the sensor jumped, but so did the neighbours
        AnomalyDetector.Result result = detector.detect(120, around(20, 6), 118.0);

        assertThat(result).isNotNull();
        assertThat(result.local()).isFalse();
        assertThat(result.zScore()).isGreaterThan(3);
    }

    @Test
    void smallChangeIsNotAnAnomaly() {
        AnomalyDetector.Result result = detector.detect(22, around(20, 6), 19.0);

        assertThat(result).isNotNull();
        assertThat(result.local()).isFalse();
        assertThat(result.zScore()).isLessThan(3);
    }

    @Test
    void notEnoughHistoryReturnsNull() {
        assertThat(detector.detect(50, around(20, 5), 21.0)).isNull();
    }

    @Test
    void noNeighboursIsNotLocal() {
        AnomalyDetector.Result result = detector.detect(50, around(20, 6), null);

        assertThat(result).isNotNull();
        assertThat(result.local()).isFalse();
    }

    @Test
    void constantHistoryHasNoZScore() {
        assertThat(detector.detect(50, List.of(20.0, 20.0, 20.0, 20.0, 20.0, 20.0), 21.0)).isNull();
    }

    @Test
    void medianOfOddAndEvenLists() {
        assertThat(AnomalyDetector.median(List.of(3.0, 1.0, 2.0))).isEqualTo(2.0);
        assertThat(AnomalyDetector.median(List.of(4.0, 1.0, 2.0, 3.0))).isCloseTo(2.5, within(0.001));
        assertThat(AnomalyDetector.median(List.of())).isNull();
    }

    private List<Double> around(double mean, int count) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(mean + (i % 2 == 0 ? -1 : 1));
        }
        return values;
    }
}
