package pl.shkumat.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.shkumat.airquality.model.Sensor;
import pl.shkumat.airquality.model.SensorSource;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findBySourceAndExternalId(SensorSource source, String externalId);

    List<Sensor> findBySource(SensorSource source);
}
