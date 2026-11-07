package pl.shkumat.airquality.source.looko2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;

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
    private static final double EARTH_RADIUS_KM = 6371;

    private final RestClient restClient = RestClient.create();
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
        LookO2Record[] records = restClient.get()
                .uri(URL, token)
                .retrieve()
                .body(LookO2Record[].class);
        if (records == null) {
            return List.of();
        }
        return toReadings(Arrays.asList(records), Instant.now());
    }

    List<SourceReading> toReadings(List<LookO2Record> records, Instant now) {
        List<SourceReading> readings = new ArrayList<>();
        for (LookO2Record record : records) {
            try {
                double lat = Double.parseDouble(record.latitude());
                double lon = Double.parseDouble(record.longitude());
                if (distanceKm(latitude, longitude, lat, lon) > radiusKm) {
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

    // haversine formula, distance between two points on Earth
    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
