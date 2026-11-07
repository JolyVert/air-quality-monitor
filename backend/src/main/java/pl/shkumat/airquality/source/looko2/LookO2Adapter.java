package pl.shkumat.airquality.source.looko2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.source.Geo;
import pl.shkumat.airquality.source.RestClients;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LookO2 returns the last reading of every sensor in Poland in one response (about 2000 sensors),
 * so we filter by distance from the area center ourselves.
 * API docs: https://looko2.pl/aktualnosci/api/ (do not call more often than once per 30 minutes).
 */
@Component
public class LookO2Adapter implements SourceAdapter {

    private static final String URL = "https://api.looko2.com/?method=GetAll&token={token}";
    // the API does not say how long the values are averaged, we treat them as a single reading
    private static final int AVERAGING_MINUTES = 1;
    // most sensors in the response stopped working years ago, skip readings older than this
    private static final Duration MAX_AGE = Duration.ofHours(1);

    private final RestClient restClient = RestClients.create();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final double latitude;
    private final double longitude;
    private final double radiusKm;
    private final String token;

    // "XXX" is the placeholder token from the docs, it works until LookO2 sends us a real one
    public LookO2Adapter(@Value("${app.area.latitude}") double latitude,
                         @Value("${app.area.longitude}") double longitude,
                         @Value("${app.area.radius-km}") double radiusKm,
                         @Value("${LOOKO2_TOKEN:}") String token) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        this.token = token.isBlank() ? "XXX" : token;
    }

    @Override
    public SensorSource getSource() {
        return SensorSource.LOOKO2;
    }

    @Override
    public List<SourceReading> fetchReadings() {
        // the API sends JSON with "Content-Type: text/html", so we parse the text ourselves
        String body = restClient.get()
                .uri(URL, token)
                .retrieve()
                .body(String.class);
        if (body == null) {
            return List.of();
        }
        LookO2Record[] records = jsonMapper.readValue(body, LookO2Record[].class);
        return toReadings(Arrays.asList(records), Instant.now());
    }

    List<SourceReading> toReadings(List<LookO2Record> records, Instant now) {
        List<SourceReading> readings = new ArrayList<>();
        for (LookO2Record record : records) {
            try {
                double lat = Double.parseDouble(record.latitude());
                double lon = Double.parseDouble(record.longitude());
                if (Geo.distanceKm(latitude, longitude, lat, lon) > radiusKm) {
                    continue;
                }

                Instant measuredAt = Instant.ofEpochSecond(Long.parseLong(record.epoch()));
                if (measuredAt.isBefore(now.minus(MAX_AGE))) {
                    continue;
                }

                readings.add(new SourceReading(
                        record.device(),
                        record.name(),
                        lat,
                        lon,
                        null,
                        "1".equals(record.indoor()),
                        measuredAt,
                        Double.parseDouble(record.pm25()),
                        Double.parseDouble(record.pm10()),
                        AVERAGING_MINUTES
                ));
            } catch (NumberFormatException | NullPointerException e) {
                // some sensors have empty or broken fields
            }
        }
        return readings;
    }
}
