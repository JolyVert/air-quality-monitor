package pl.shkumat.airquality.dto;

import java.time.Instant;

public record MeasurementDto(Instant measuredAt, Double pm25, Double pm10, boolean valid) {
}
