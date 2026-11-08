package pl.shkumat.airquality.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.shkumat.airquality.model.Anomaly;
import pl.shkumat.airquality.model.AnomalyId;

import java.time.Instant;
import java.util.List;

public interface AnomalyRepository extends JpaRepository<Anomaly, AnomalyId> {

    List<Anomaly> findByDetectedAtGreaterThanEqual(Instant since);
}
