CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE sensor (
    id           BIGSERIAL PRIMARY KEY,
    source       VARCHAR(32)      NOT NULL,
    external_id  VARCHAR(64)      NOT NULL,
    name         VARCHAR(255),
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    sensor_type  VARCHAR(64),
    indoor       BOOLEAN          NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMPTZ,
    CONSTRAINT uk_sensor_source_external_id UNIQUE (source, external_id)
);

CREATE TABLE measurement (
    sensor_id         BIGINT           NOT NULL REFERENCES sensor (id),
    measured_at       TIMESTAMPTZ      NOT NULL,
    pm25              DOUBLE PRECISION,
    pm10              DOUBLE PRECISION,
    averaging_minutes INTEGER          NOT NULL,
    valid             BOOLEAN          NOT NULL DEFAULT TRUE,
    fetched_at        TIMESTAMPTZ      NOT NULL,
    PRIMARY KEY (sensor_id, measured_at)
);

SELECT create_hypertable('measurement', by_range('measured_at'));
