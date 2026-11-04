package pl.shkumat.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.shkumat.airquality.model.Measurement;
import pl.shkumat.airquality.model.MeasurementId;
import pl.shkumat.airquality.model.SensorSource;

import java.time.Instant;
import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, MeasurementId> {

    List<Measurement> findBySensorIdAndMeasuredAtBetweenOrderByMeasuredAt(Long sensorId, Instant from, Instant to);

    @Query("select max(m.fetchedAt) from Measurement m, Sensor s where s.id = m.sensorId and s.source = :source")
    Instant findLastFetchedAt(@Param("source") SensorSource source);
}
