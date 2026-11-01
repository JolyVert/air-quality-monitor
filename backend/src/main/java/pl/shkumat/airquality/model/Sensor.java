package pl.shkumat.airquality.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sensor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SensorSource source;

    // id of the sensor in the source API (GIOS station id, Airly installation id, ...)
    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Setter
    private String name;

    @Setter
    @Column(nullable = false)
    private double latitude;

    @Setter
    @Column(nullable = false)
    private double longitude;

    @Setter
    @Column(name = "sensor_type")
    private String sensorType;

    @Setter
    @Column(nullable = false)
    private boolean indoor;

    @Setter
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public Sensor(SensorSource source, String externalId, double latitude, double longitude) {
        this.source = source;
        this.externalId = externalId;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
