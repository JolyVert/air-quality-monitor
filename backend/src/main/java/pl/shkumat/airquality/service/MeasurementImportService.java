package pl.shkumat.airquality.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.repository.MeasurementRepository;
import pl.shkumat.airquality.repository.SensorRepository;
import pl.shkumat.airquality.source.SourceReading;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementImportService {

    private final SensorRepository sensorRepository;
    private final MeasurementRepository measurementRepository;

    /**
     * Saves readings from one source. Sensors seen for the first time are created,
     * known sensors get updated location and last_seen_at.
     */
    @Transactional
    public void save(SensorSource source, List<SourceReading> readings) {
        for (SourceReading reading : readings) {
            Sensor sensor = sensorRepository.findBySourceAndExternalId(source, reading.externalId())
                    .orElseGet(() -> new Sensor(source, reading.externalId(), reading.latitude(), reading.longitude()));

            sensor.setName(reading.name());
            sensor.setLatitude(reading.latitude());
            sensor.setLongitude(reading.longitude());
            sensor.setSensorType(reading.sensorType());
            sensor.setIndoor(reading.indoor());
            if (sensor.getLastSeenAt() == null || sensor.getLastSeenAt().isBefore(reading.measuredAt())) {
                sensor.setLastSeenAt(reading.measuredAt());
            }
            sensor = sensorRepository.save(sensor);

            measurementRepository.save(new Measurement(sensor.getId(), reading.measuredAt(),
                    reading.pm25(), reading.pm10(), reading.averagingMinutes()));
        }
    }

    public boolean fetchedWithin(SensorSource source, Duration duration) {
        Instant lastFetchedAt = measurementRepository.findLastFetchedAt(source);
        return lastFetchedAt != null && lastFetchedAt.isAfter(Instant.now().minus(duration));
    }
}
