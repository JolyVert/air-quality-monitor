package pl.shkumat.airquality.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.shkumat.airquality.model.Anomaly;
import pl.shkumat.airquality.model.AnomalyId;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.repository.AnomalyRepository;
import pl.shkumat.airquality.repository.MeasurementRepository;
import pl.shkumat.airquality.repository.SensorRepository;
import pl.shkumat.airquality.source.Geo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs detection for every outdoor sensor that has a recent valid PM2.5 reading.
 */
@Slf4j
@Service
public class AnomalyDetectionService {

    private final SensorRepository sensorRepository;
    private final MeasurementRepository measurementRepository;
    private final AnomalyRepository anomalyRepository;
    private final AnomalyDetector detector;
    private final Duration window;
    private final double neighborRadiusKm;
    private final double cityRadiusKm;
    private final int minNeighbors;

    public AnomalyDetectionService(SensorRepository sensorRepository,
                                   MeasurementRepository measurementRepository,
                                   AnomalyRepository anomalyRepository,
                                   AnomalyDetector detector,
                                   @Value("${app.detection.window-hours}") int windowHours,
                                   @Value("${app.detection.neighbor-radius-km}") double neighborRadiusKm,
                                   @Value("${app.area.radius-km}") double cityRadiusKm,
                                   @Value("${app.detection.min-neighbors}") int minNeighbors) {
        this.sensorRepository = sensorRepository;
        this.measurementRepository = measurementRepository;
        this.anomalyRepository = anomalyRepository;
        this.detector = detector;
        this.window = Duration.ofHours(windowHours);
        this.neighborRadiusKm = neighborRadiusKm;
        this.cityRadiusKm = cityRadiusKm;
        this.minNeighbors = minNeighbors;
    }

    /**
     * @return how many local anomalies were found in this run
     */
    public int run() {
        Instant now = Instant.now();
        Map<Long, Measurement> latest = latestValidOutdoor(now.minus(Duration.ofHours(24)));
        if (latest.isEmpty()) {
            return 0;
        }

        Map<Long, Sensor> sensors = new HashMap<>();
        for (Sensor sensor : sensorRepository.findAllById(latest.keySet())) {
            sensors.put(sensor.getId(), sensor);
        }

        Instant loadFrom = now.minus(window).minus(Duration.ofHours(24));
        Map<Long, List<Measurement>> history = new HashMap<>();
        for (Measurement measurement : measurementRepository.findByMeasuredAtGreaterThanEqual(loadFrom)) {
            history.computeIfAbsent(measurement.getSensorId(), id -> new ArrayList<>()).add(measurement);
        }

        int found = 0;
        for (Map.Entry<Long, Measurement> entry : latest.entrySet()) {
            Long sensorId = entry.getKey();
            Measurement current = entry.getValue();
            Sensor sensor = sensors.get(sensorId);
            if (sensor == null) {
                continue;
            }

            List<Double> previous = previousPm25(history.getOrDefault(sensorId, List.of()), current);
            Double background = backgroundPm25(sensor, latest, sensors);
            AnomalyDetector.Result result = detector.detect(current.getPm25(), previous, background);

            AnomalyId id = new AnomalyId(sensorId, current.getMeasuredAt());
            if (result != null && result.local()) {
                anomalyRepository.save(new Anomaly(
                        sensorId, current.getMeasuredAt(), current.getPm25(), result.zScore(), background));
                found++;
            } else {
                // a later run with more history may decide it is not an anomaly after all
                anomalyRepository.deleteById(id);
            }
        }
        return found;
    }

    private Map<Long, Measurement> latestValidOutdoor(Instant since) {
        Map<Long, Boolean> indoor = new HashMap<>();
        for (Sensor sensor : sensorRepository.findAll()) {
            indoor.put(sensor.getId(), sensor.isIndoor());
        }
        Map<Long, Measurement> latest = new HashMap<>();
        for (Measurement measurement : measurementRepository.findLatestPerSensorSince(since)) {
            if (!measurement.isValid() || measurement.getPm25() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(indoor.get(measurement.getSensorId()))) {
                continue;
            }
            latest.put(measurement.getSensorId(), measurement);
        }
        return latest;
    }

    private List<Double> previousPm25(List<Measurement> measurements, Measurement current) {
        Instant from = current.getMeasuredAt().minus(window);
        List<Double> values = new ArrayList<>();
        for (Measurement measurement : measurements) {
            Instant at = measurement.getMeasuredAt();
            if (!at.isBefore(current.getMeasuredAt()) || at.isBefore(from)) {
                continue;
            }
            if (measurement.isValid() && measurement.getPm25() != null) {
                values.add(measurement.getPm25());
            }
        }
        return values;
    }

    private Double backgroundPm25(Sensor sensor, Map<Long, Measurement> latest, Map<Long, Sensor> sensors) {
        List<Double> nearby = neighbourValues(sensor, latest, sensors, neighborRadiusKm);
        if (nearby.size() >= minNeighbors) {
            return AnomalyDetector.median(nearby);
        }
        // not enough neighbours nearby, try a wider circle around this sensor
        List<Double> city = neighbourValues(sensor, latest, sensors, cityRadiusKm);
        if (city.size() < minNeighbors) {
            return null;
        }
        return AnomalyDetector.median(city);
    }

    private List<Double> neighbourValues(Sensor sensor, Map<Long, Measurement> latest,
                                         Map<Long, Sensor> sensors, double radiusKm) {
        List<Double> values = new ArrayList<>();
        for (Map.Entry<Long, Measurement> entry : latest.entrySet()) {
            if (entry.getKey().equals(sensor.getId())) {
                continue;
            }
            Sensor other = sensors.get(entry.getKey());
            if (other == null) {
                continue;
            }
            if (Geo.distanceKm(sensor.getLatitude(), sensor.getLongitude(),
                    other.getLatitude(), other.getLongitude()) <= radiusKm) {
                values.add(entry.getValue().getPm25());
            }
        }
        return values;
    }
}
