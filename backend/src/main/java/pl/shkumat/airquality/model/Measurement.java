package pl.shkumat.airquality.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    @Column(nullable = false)
    private boolean valid = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "invalid_reason")
    private InvalidReason invalidReason;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public Measurement(Long sensorId, Instant measuredAt, Double pm25, Double pm10, int averagingMinutes) {
        this.sensorId = sensorId;
        this.measuredAt = measuredAt;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.averagingMinutes = averagingMinutes;
        // PostgreSQL keeps microseconds, so the object matches what is stored in the database
        this.fetchedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void markInvalid(InvalidReason reason) {
        this.valid = false;
        this.invalidReason = reason;
    }
}
