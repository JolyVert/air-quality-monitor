-- why validation rejected the measurement, NULL for valid ones
ALTER TABLE measurement ADD COLUMN invalid_reason VARCHAR(32);
