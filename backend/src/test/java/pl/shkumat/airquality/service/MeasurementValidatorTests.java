package pl.shkumat.airquality.service;

import org.junit.jupiter.api.Test;
import pl.shkumat.airquality.model.InvalidReason;
import pl.shkumat.airquality.model.Measurement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementValidatorTests {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private final MeasurementValidator validator = new MeasurementValidator(6);

    @Test
    void normalMeasurementIsValid() {
        assertThat(validator.validate(measurement(12.3, 18.1), List.of())).isNull();
    }

    @Test
    void onlyOneValueIsEnough() {
        assertThat(validator.validate(measurement(null, 18.1), List.of())).isNull();
    }

    @Test
    void negativeValueIsOutOfRange() {
        assertThat(validator.validate(measurement(-1.0, 18.1), List.of())).isEqualTo(InvalidReason.OUT_OF_RANGE);
    }

    @Test
    void tooHighValueIsOutOfRange() {
        assertThat(validator.validate(measurement(12.0, 1500.0), List.of())).isEqualTo(InvalidReason.OUT_OF_RANGE);
    }

    @Test
    void sixSameValuesInARowAreStuck() {
        List<Measurement> previous = repeat(0.0, 0.0, 5);

        assertThat(validator.validate(measurement(0.0, 0.0), previous)).isEqualTo(InvalidReason.STUCK_VALUE);
    }

    @Test
    void fiveSameValuesAreNotEnough() {
        List<Measurement> previous = repeat(7.5, 9.0, 4);

        assertThat(validator.validate(measurement(7.5, 9.0), previous)).isNull();
    }

    @Test
    void oneDifferentValueBreaksTheSeries() {
        List<Measurement> previous = repeat(7.5, 9.0, 5);
        previous.set(2, measurement(7.6, 9.0));

        assertThat(validator.validate(measurement(7.5, 9.0), previous)).isNull();
    }

    private Measurement measurement(Double pm25, Double pm10) {
        return new Measurement(1L, NOW, pm25, pm10, 5);
    }

    private List<Measurement> repeat(Double pm25, Double pm10, int count) {
        List<Measurement> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(measurement(pm25, pm10));
        }
        return list;
    }
}
