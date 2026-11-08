package pl.shkumat.airquality.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.shkumat.airquality.dto.MeasurementDto;
import pl.shkumat.airquality.dto.SensorDto;
import pl.shkumat.airquality.model.Anomaly;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.repository.AnomalyRepository;
import pl.shkumat.airquality.repository.MeasurementRepository;
import pl.shkumat.airquality.repository.SensorRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SensorService {

    // older measurements are not shown on the map as "current"
    private static final Duration LATEST_MAX_AGE = Duration.ofHours(24);

    private final SensorRepository sensorRepository;
    private final MeasurementRepository measurementRepository;
    private final AnomalyRepository anomalyRepository;

    public List<SensorDto> getSensors() {
        Instant since = Instant.now().minus(LATEST_MAX_AGE);
        Map<Long, Measurement> latest = new HashMap<>();
        for (Measurement m : measurementRepository.findLatestPerSensorSince(since)) {
            latest.put(m.getSensorId(), m);
        }
        Map<Long, Anomaly> anomalies = new HashMap<>();
        for (Anomaly anomaly : anomalyRepository.findByDetectedAtGreaterThanEqual(since)) {
            Measurement current = latest.get(anomaly.getSensorId());
            if (current != null && current.getMeasuredAt().equals(anomaly.getDetectedAt())) {
                anomalies.put(anomaly.getSensorId(), anomaly);
            }
        }

        return sensorRepository.findAll().stream()
                .map(sensor -> toDto(sensor, latest.get(sensor.getId()), anomalies.get(sensor.getId())))
                .toList();
    }

    public List<MeasurementDto> getMeasurements(Long sensorId, int hours) {
        if (!sensorRepository.existsById(sensorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sensor " + sensorId + " not found");
        }
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofHours(hours));
        return measurementRepository.findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAt(sensorId, from, to).stream()
                .map(m -> new MeasurementDto(m.getMeasuredAt(), m.getPm25(), m.getPm10(), m.isValid()))
                .toList();
    }

    private SensorDto toDto(Sensor sensor, Measurement latest, Anomaly anomaly) {
        boolean currentAnomaly = anomaly != null && latest != null
                && anomaly.getDetectedAt().equals(latest.getMeasuredAt());
        return new SensorDto(
                sensor.getId(),
                sensor.getSource(),
                sensor.getName(),
                sensor.getLatitude(),
                sensor.getLongitude(),
                sensor.getSensorType(),
                sensor.isIndoor(),
                latest != null ? latest.getMeasuredAt() : null,
                latest != null ? latest.getPm25() : null,
                latest != null ? latest.getPm10() : null,
                latest == null || latest.isValid(),
                currentAnomaly,
                currentAnomaly ? anomaly.getZScore() : null
        );
    }
}
