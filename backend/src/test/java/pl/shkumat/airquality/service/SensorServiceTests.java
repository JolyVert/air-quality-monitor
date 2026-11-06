package pl.shkumat.airquality.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.shkumat.airquality.dto.MeasurementDto;
import pl.shkumat.airquality.dto.SensorDto;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.repository.MeasurementRepository;
import pl.shkumat.airquality.repository.SensorRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// runs against the database from docker compose, changes are rolled back after each test
@SpringBootTest(properties = "app.fetch.enabled=false")
@Transactional
class SensorServiceTests {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Test
    void sensorHasItsNewestMeasurement() {
        Sensor sensor = sensorRepository.save(new Sensor(SensorSource.SENSOR_COMMUNITY, "test-1", 51.1, 17.0));
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        measurementRepository.save(new Measurement(sensor.getId(), now.minus(Duration.ofMinutes(10)), 5.0, 8.0, 5));
        measurementRepository.save(new Measurement(sensor.getId(), now.minus(Duration.ofMinutes(5)), 7.0, 9.0, 5));
        measurementRepository.flush();

        SensorDto dto = findDto(sensor.getId());

        assertThat(dto.measuredAt()).isEqualTo(now.minus(Duration.ofMinutes(5)));
        assertThat(dto.pm25()).isEqualTo(7.0);
        assertThat(dto.pm10()).isEqualTo(9.0);
    }

    @Test
    void sensorWithOldDataHasNoCurrentValues() {
        Sensor sensor = sensorRepository.save(new Sensor(SensorSource.AIRLY, "test-2", 51.1, 17.0));
        measurementRepository.save(new Measurement(sensor.getId(), Instant.now().minus(Duration.ofDays(3)), 20.0, 30.0, 60));
        measurementRepository.flush();

        SensorDto dto = findDto(sensor.getId());

        assertThat(dto.measuredAt()).isNull();
        assertThat(dto.pm25()).isNull();
    }

    @Test
    void returnsHistoryInTimeOrder() {
        Sensor sensor = sensorRepository.save(new Sensor(SensorSource.AIRLY, "test-3", 51.1, 17.0));
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        measurementRepository.save(new Measurement(sensor.getId(), now.minus(Duration.ofHours(1)), 11.0, 15.0, 60));
        measurementRepository.save(new Measurement(sensor.getId(), now.minus(Duration.ofHours(3)), 10.0, 14.0, 60));
        measurementRepository.save(new Measurement(sensor.getId(), now.minus(Duration.ofHours(30)), 9.0, 13.0, 60));
        measurementRepository.flush();

        List<MeasurementDto> history = sensorService.getMeasurements(sensor.getId(), 24);

        // the measurement from 30 hours ago is outside of the 24 hour window
        assertThat(history).extracting(MeasurementDto::pm25).containsExactly(10.0, 11.0);
    }

    private SensorDto findDto(Long sensorId) {
        return sensorService.getSensors().stream()
                .filter(s -> s.id().equals(sensorId))
                .findFirst()
                .orElseThrow();
    }
}
