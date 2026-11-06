package pl.shkumat.airquality.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.shkumat.airquality.model.InvalidReason;
import pl.shkumat.airquality.model.Measurement;

import java.util.List;
import java.util.Objects;

/**
 * Finds measurements that are certainly wrong. It does not compare sensors with each other,
 * that is the job of the anomaly detection.
 */
@Component
public class MeasurementValidator {

    // SDS011 and Plantower sensors measure up to about 1000 µg/m³
    static final double MAX_PM = 1000;

    private final int stuckCount;

    public MeasurementValidator(@Value("${app.validation.stuck-count}") int stuckCount) {
        this.stuckCount = stuckCount;
    }

    /**
     * How many previous measurements of the sensor validate() needs.
     */
    public int previousNeeded() {
        return stuckCount - 1;
    }

    /**
     * @param previous earlier measurements of the same sensor, newest first
     * @return why the measurement is wrong, or null if it is fine
     */
    public InvalidReason validate(Measurement measurement, List<Measurement> previous) {
        if (isOutOfRange(measurement.getPm25()) || isOutOfRange(measurement.getPm10())) {
            return InvalidReason.OUT_OF_RANGE;
        }
        if (isStuck(measurement, previous)) {
            return InvalidReason.STUCK_VALUE;
        }
        return null;
    }

    private boolean isOutOfRange(Double value) {
        return value != null && (value < 0 || value > MAX_PM);
    }

    // a working sensor never sends exactly the same PM2.5 and PM10 many times in a row
    private boolean isStuck(Measurement measurement, List<Measurement> previous) {
        if (previous.size() < stuckCount - 1) {
            return false;
        }
        for (int i = 0; i < stuckCount - 1; i++) {
            Measurement earlier = previous.get(i);
            if (!Objects.equals(earlier.getPm25(), measurement.getPm25())
                    || !Objects.equals(earlier.getPm10(), measurement.getPm10())) {
                return false;
            }
        }
        return true;
    }
}
