package pl.shkumat.airquality.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.shkumat.airquality.model.InvalidReason;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.repository.MeasurementRepository;
import pl.shkumat.airquality.repository.SensorRepository;
import pl.shkumat.airquality.source.SourceReading;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// runs against the database from docker compose, changes are rolled back after each test
@SpringBootTest(properties = "app.fetch.enabled=false")
@Transactional
class MeasurementImportServiceTests {

    private static final Instant START = Instant.parse("2026-11-01T10:00:00Z");

    @Autowired
    private MeasurementImportService importService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Test
    void sensorSendingZerosIsMarkedAsStuck() {
        List<SourceReading> readings = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            readings.add(reading(START.plus(Duration.ofMinutes(5L * i)), 0.0, 0.0));
        }
        // the batch does not have to be sorted
        readings.add(readings.removeFirst());

        importService.save(SensorSource.SENSOR_COMMUNITY, readings);

        List<Measurement> saved = savedMeasurements();
        assertThat(saved).extracting(Measurement::isValid)
                .containsExactly(true, true, true, true, true, false, false, false);
        assertThat(saved.get(7).getInvalidReason()).isEqualTo(InvalidReason.STUCK_VALUE);
    }

    @Test
    void impossibleValueIsSavedButMarked() {
        importService.save(SensorSource.SENSOR_COMMUNITY, List.of(reading(START, 5.0, 1999.9)));

        Measurement saved = savedMeasurements().getFirst();
        assertThat(saved.isValid()).isFalse();
        assertThat(saved.getInvalidReason()).isEqualTo(InvalidReason.OUT_OF_RANGE);
    }

    private SourceReading reading(Instant measuredAt, Double pm25, Double pm10) {
        return new SourceReading("test-stuck", null, 51.1, 17.0, "SDS011", false, measuredAt, pm25, pm10, 5);
    }

    private List<Measurement> savedMeasurements() {
        Sensor sensor = sensorRepository.findBySourceAndExternalId(SensorSource.SENSOR_COMMUNITY, "test-stuck")
                .orElseThrow();
        return measurementRepository.findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAt(
                sensor.getId(), START, START.plus(Duration.ofHours(1)));
    }
}
