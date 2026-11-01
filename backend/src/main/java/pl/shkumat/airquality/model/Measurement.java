package pl.shkumat.airquality.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "measurement")
@IdClass(MeasurementId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Measurement {

    @Id
    @Column(name = "sensor_id")
    private Long sensorId;

    // always stored in UTC
    @Id
    @Column(name = "measured_at")
    private Instant measuredAt;

    @Setter
    private Double pm25;

    @Setter
    private Double pm10;

    // how long the source averaged the value: GIOS 60, Sensor.community 5, ...
    @Column(name = "averaging_minutes", nullable = false)
    private int averagingMinutes;

    @Setter
    @Column(nullable = false)
    private boolean valid = true;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public Measurement(Long sensorId, Instant measuredAt, Double pm25, Double pm10, int averagingMinutes) {
        this.sensorId = sensorId;
        this.measuredAt = measuredAt;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.averagingMinutes = averagingMinutes;
        this.fetchedAt = Instant.now();
    }
}
