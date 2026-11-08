package pl.shkumat.airquality.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.shkumat.airquality.model.Anomaly;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.repository.AnomalyRepository;
import pl.shkumat.airquality.repository.MeasurementRepository;
import pl.shkumat.airquality.repository.SensorRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.fetch.enabled=false")
@Transactional
class AnomalyDetectionServiceTests {

    @Autowired
    private AnomalyDetectionService detectionService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @Test
    void flagsOnlyTheSensorThatSpiked() {
        Sensor spiked = sensor("a", 54.35, 18.65);
        Sensor neighbour1 = sensor("b", 54.36, 18.65);
        Sensor neighbour2 = sensor("c", 54.37, 18.65);
        Sensor neighbour3 = sensor("d", 54.35, 18.66);
        Instant end = Instant.now().truncatedTo(ChronoUnit.HOURS);
        history(spiked, end, 20);
        history(neighbour1, end, 20);
        history(neighbour2, end, 21);
        history(neighbour3, end, 19);
        // now: only "a" jumps
        save(spiked, end, 80, 90);
        save(neighbour1, end, 22, 30);
        save(neighbour2, end, 21, 28);
        save(neighbour3, end, 20, 27);
        measurementRepository.flush();

        int found = detectionService.run();

        List<Anomaly> anomalies = anomalyRepository.findAll();
        assertThat(found).isEqualTo(1);
        assertThat(anomalies).extracting(Anomaly::getSensorId).containsExactly(spiked.getId());
        assertThat(anomalies.get(0).getPm25()).isEqualTo(80.0);
        assertThat(anomalies.get(0).getZScore()).isGreaterThan(3);
    }

    @Test
    void cityWideRiseIsNotSaved() {
        Sensor a = sensor("a", 54.35, 18.65);
        Sensor b = sensor("b", 54.36, 18.65);
        Sensor c = sensor("c", 54.37, 18.65);
        Sensor d = sensor("d", 54.35, 18.66);
        Instant end = Instant.now().truncatedTo(ChronoUnit.HOURS);
        history(a, end, 20);
        history(b, end, 20);
        history(c, end, 21);
        history(d, end, 19);
        save(a, end, 120, 150);
        save(b, end, 118, 148);
        save(c, end, 122, 155);
        save(d, end, 119, 149);
        measurementRepository.flush();

        assertThat(detectionService.run()).isZero();
        assertThat(anomalyRepository.findAll()).isEmpty();
    }

    private Sensor sensor(String externalId, double lat, double lon) {
        return sensorRepository.save(new Sensor(SensorSource.SENSOR_COMMUNITY, externalId, lat, lon));
    }

    private void history(Sensor sensor, Instant end, double pm25) {
        for (int hour = 8; hour >= 1; hour--) {
            save(sensor, end.minus(Duration.ofHours(hour)), pm25 + (hour % 2), pm25 + 5);
        }
    }

    private void save(Sensor sensor, Instant at, double pm25, double pm10) {
        measurementRepository.save(new Measurement(sensor.getId(), at.truncatedTo(ChronoUnit.MICROS), pm25, pm10, 60));
    }
}
