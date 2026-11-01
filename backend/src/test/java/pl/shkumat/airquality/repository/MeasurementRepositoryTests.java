package pl.shkumat.airquality.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.MeasurementId;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.model.SensorSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// runs against the database from docker compose, changes are rolled back after each test
@SpringBootTest
@Transactional
class MeasurementRepositoryTests {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Test
    void savesSensorWithMeasurements() {
        Sensor sensor = new Sensor(SensorSource.SENSOR_COMMUNITY, "test-68319", 51.12, 17.024);
        sensor.setSensorType("SDS011");
        sensor = sensorRepository.save(sensor);

        Instant first = Instant.parse("2026-10-30T10:45:00Z");
        Instant second = Instant.parse("2026-10-30T10:50:00Z");
        measurementRepository.save(new Measurement(sensor.getId(), first, 5.8, 12.6, 5));
        measurementRepository.save(new Measurement(sensor.getId(), second, 6.1, 13.0, 5));
        measurementRepository.flush();

        assertThat(sensorRepository.findBySourceAndExternalId(SensorSource.SENSOR_COMMUNITY, "test-68319"))
                .isPresent();

        List<Measurement> measurements = measurementRepository.findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAt(
                sensor.getId(), first, second);
        assertThat(measurements).hasSize(2);
        assertThat(measurements.get(0).getPm25()).isEqualTo(5.8);
        assertThat(measurements.get(1).getPm10()).isEqualTo(13.0);
    }

    @Test
    void sameSensorAndTimeIsOneMeasurement() {
        Sensor sensor = sensorRepository.save(new Sensor(SensorSource.GIOS, "test-117", 51.129, 17.029));
        Instant hour = Instant.parse("2026-10-30T10:00:00Z");

        measurementRepository.save(new Measurement(sensor.getId(), hour, 23.5, null, 60));
        measurementRepository.flush();

        // fetching the same hour again (e.g. with PM10 filled in) updates the existing row
        Measurement again = new Measurement(sensor.getId(), hour, 23.5, 30.1, 60);
        measurementRepository.save(again);
        measurementRepository.flush();

        assertThat(measurementRepository.findById(new MeasurementId(sensor.getId(), hour)))
                .get()
                .extracting(Measurement::getPm10)
                .isEqualTo(30.1);
    }
}
