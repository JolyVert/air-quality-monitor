package pl.shkumat.airquality.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeasurementId implements Serializable {

    private Long sensorId;
    private Instant measuredAt;
}
