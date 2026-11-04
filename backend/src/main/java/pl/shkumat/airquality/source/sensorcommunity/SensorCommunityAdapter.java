package pl.shkumat.airquality.source.sensorcommunity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sensor.community returns 5-minute averages from the last 5 minutes for all sensors in the area.
 * API docs: https://github.com/opendata-stuttgart/meta/wiki/APIs
 */
@Component
public class SensorCommunityAdapter implements SourceAdapter {

    private static final String URL = "https://data.sensor.community/airrohr/v1/filter/area={lat},{lon},{radius}";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int AVERAGING_MINUTES = 5;

    private final RestClient restClient = RestClient.create();
    private final double latitude;
    private final double longitude;
    private final double radiusKm;

    public SensorCommunityAdapter(@Value("${app.area.latitude}") double latitude,
                                  @Value("${app.area.longitude}") double longitude,
                                  @Value("${app.area.radius-km}") double radiusKm) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
    }

    @Override
    public SensorSource getSource() {
        return SensorSource.SENSOR_COMMUNITY;
    }

    @Override
    public List<SourceReading> fetchReadings() {
        SensorCommunityRecord[] records = restClient.get()
                .uri(URL, latitude, longitude, radiusKm)
                .retrieve()
                .body(SensorCommunityRecord[].class);
        if (records == null) {
            return List.of();
        }
        return toReadings(Arrays.asList(records));
    }

    List<SourceReading> toReadings(List<SensorCommunityRecord> records) {
        List<SourceReading> readings = new ArrayList<>();
        for (SensorCommunityRecord record : records) {
            Map<String, String> values = new HashMap<>();
            for (SensorCommunityRecord.Value value : record.values()) {
                values.put(value.valueType(), value.value());
            }

            // the area also contains temperature/humidity sensors, skip everything without PM
            // P1 = PM10, P2 = PM2.5
            if (!values.containsKey("P1") || !values.containsKey("P2")) {
                continue;
            }

            try {
                readings.add(new SourceReading(
                        String.valueOf(record.sensor().id()),
                        null,
                        Double.parseDouble(record.location().latitude()),
                        Double.parseDouble(record.location().longitude()),
                        record.sensor().sensorType().name(),
                        record.location().indoor() == 1,
                        parseTimestamp(record.timestamp()),
                        Double.parseDouble(values.get("P2")),
                        Double.parseDouble(values.get("P1")),
                        AVERAGING_MINUTES
                ));
            } catch (NumberFormatException e) {
                // broken sensors sometimes send garbage instead of numbers
            }
        }
        return readings;
    }

    // timestamps look like "2026-10-30 10:46:00" and are in UTC
    private Instant parseTimestamp(String timestamp) {
        return LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC);
    }
}
