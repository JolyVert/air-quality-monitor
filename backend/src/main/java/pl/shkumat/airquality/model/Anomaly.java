package pl.shkumat.airquality.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "anomaly")
@IdClass(AnomalyId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Anomaly {

    @Id
    @Column(name = "sensor_id")
    private Long sensorId;

    @Id
    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(nullable = false)
    private double pm25;

    @Column(name = "z_score", nullable = false)
    private double zScore;

    // median PM2.5 of neighbouring outdoor sensors at the same time
    @Column(name = "background_pm25", nullable = false)
    private double backgroundPm25;

    public Anomaly(Long sensorId, Instant detectedAt, double pm25, double zScore, double backgroundPm25) {
        this.sensorId = sensorId;
        this.detectedAt = detectedAt;
        this.pm25 = pm25;
        this.zScore = zScore;
        this.backgroundPm25 = backgroundPm25;
    }
}
