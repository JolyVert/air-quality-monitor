package pl.shkumat.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.MeasurementId;

import java.time.Instant;
import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, MeasurementId> {

    List<Measurement> findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAt(Long sensorId, Instant from, Instant to);
}
