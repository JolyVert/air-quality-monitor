-- one row = a local anomaly at that sensor and time (kept even after it ends)
CREATE TABLE anomaly (
    sensor_id        BIGINT           NOT NULL REFERENCES sensor (id),
    detected_at      TIMESTAMPTZ      NOT NULL,
    pm25             DOUBLE PRECISION NOT NULL,
    z_score          DOUBLE PRECISION NOT NULL,
    background_pm25  DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (sensor_id, detected_at)
);
