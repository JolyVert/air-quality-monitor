package pl.shkumat.airquality.dto;

import pl.shkumat.airquality.model.SensorSource;

import java.time.Instant;

/**
 * Sensor for the map, with its latest measurement (null values if there is no recent one).
 */
public record SensorDto(
        Long id,
        SensorSource source,
        String name,
        double latitude,
        double longitude,
        String sensorType,
        boolean indoor,
        Instant measuredAt,
        Double pm25,
        Double pm10
) {
}
