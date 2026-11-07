package pl.shkumat.airquality.source.airly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.source.RestClients;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Airly needs one request for the list of installations and one request per installation.
 * The free API key allows 100 requests per day, so this adapter runs rarely and reads
 * the last 24 hours from "history" every time.
 * API docs: https://developer.airly.org
 */
@Slf4j
@Component
public class AirlyAdapter implements SourceAdapter {

    private static final String BASE_URL = "https://airapi.airly.eu/v2";
    private static final int AVERAGING_MINUTES = 60;

    private final RestClient restClient;
    private final String apiKey;
    private final double latitude;
    private final double longitude;
    private final double radiusKm;
    private final int maxInstallations;

    public AirlyAdapter(@Value("${AIRLY_API_KEY:}") String apiKey,
                        @Value("${app.area.latitude}") double latitude,
                        @Value("${app.area.longitude}") double longitude,
                        @Value("${app.area.radius-km}") double radiusKm,
                        @Value("${app.airly.max-installations}") int maxInstallations) {
        this.apiKey = apiKey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        this.maxInstallations = maxInstallations;
        this.restClient = RestClients.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("apikey", apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public SensorSource getSource() {
        return SensorSource.AIRLY;
    }

    @Override
    public List<SourceReading> fetchReadings() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("AIRLY_API_KEY is not set in .env");
        }

        AirlyInstallation[] installations = restClient.get()
                .uri("/installations/nearest?lat={lat}&lng={lng}&maxDistanceKM={radius}&maxResults=-1",
                        latitude, longitude, radiusKm)
                .retrieve()
                .body(AirlyInstallation[].class);
        if (installations == null) {
            return List.of();
        }

        List<SourceReading> readings = new ArrayList<>();
        for (int i = 0; i < installations.length && i < maxInstallations; i++) {
            AirlyInstallation installation = installations[i];
            try {
                AirlyMeasurements measurements = restClient.get()
                        .uri("/measurements/installation?installationId={id}", installation.id())
                        .retrieve()
                        .body(AirlyMeasurements.class);
                if (measurements != null) {
                    readings.addAll(toReadings(installation, measurements));
                }
            } catch (HttpClientErrorException.TooManyRequests e) {
                // daily limit reached, keep what we already have
                log.warn("AIRLY: daily request limit reached after {} installations", i);
                break;
            }
        }
        return readings;
    }

    List<SourceReading> toReadings(AirlyInstallation installation, AirlyMeasurements measurements) {
        List<SourceReading> readings = new ArrayList<>();
        if (measurements.history() == null) {
            return readings;
        }

        for (AirlyMeasurements.Period period : measurements.history()) {
            Map<String, Double> values = new HashMap<>();
            for (AirlyMeasurements.Value value : period.values()) {
                values.put(value.name(), value.value());
            }

            Double pm25 = values.get("PM25");
            Double pm10 = values.get("PM10");
            // the sensor was offline in this hour
            if (pm25 == null && pm10 == null) {
                continue;
            }

            String street = installation.address() != null ? installation.address().street() : null;
            readings.add(new SourceReading(
                    String.valueOf(installation.id()),
                    street,
                    installation.location().latitude(),
                    installation.location().longitude(),
                    "Airly",
                    false,
                    // measured_at is the end of the averaging period, like in the other sources
                    Instant.parse(period.tillDateTime()),
                    pm25,
                    pm10,
                    AVERAGING_MINUTES
            ));
        }
        return readings;
    }
}
