package pl.shkumat.airquality.source;

import java.time.Instant;

/**
 * One reading from any source, already converted to the common format.
 * Adapters return these, MeasurementImportService saves them to the database.
 */
public record SourceReading(
        String externalId,
        String name,
        double latitude,
        double longitude,
        String sensorType,
        boolean indoor,
        Instant measuredAt,
        Double pm25,
        Double pm10,
        int averagingMinutes
) {
}
