package pl.shkumat.airquality.source.gios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.shkumat.airquality.model.SensorSource;
import pl.shkumat.airquality.source.Geo;
import pl.shkumat.airquality.source.RestClients;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * GIOS (state stations) needs several requests: the list of stations, the positions of each station
 * and the data of each PM position. PM10 and PM2.5 are separate positions, so their hourly values
 * are joined into one reading per hour.
 * API docs: https://powietrze.gios.gov.pl/pjp/content/api
 */
@Component
public class GiosAdapter implements SourceAdapter {

    private static final String BASE_URL = "https://api.gios.gov.pl/pjp-api/v1/rest";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId POLISH_TIME = ZoneId.of("Europe/Warsaw");
    private static final int AVERAGING_MINUTES = 60;

    private final RestClient restClient = RestClients.builder().baseUrl(BASE_URL).build();
    private final double latitude;
    private final double longitude;
    private final double radiusKm;

    public GiosAdapter(@Value("${app.area.latitude}") double latitude,
                       @Value("${app.area.longitude}") double longitude,
                       @Value("${app.area.radius-km}") double radiusKm) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
    }

    @Override
    public SensorSource getSource() {
        return SensorSource.GIOS;
    }

    @Override
    public List<SourceReading> fetchReadings() {
        // about 300 stations in Poland, they fit in one page
        GiosStations stations = restClient.get()
                .uri("/station/findAll?size=500")
                .retrieve()
                .body(GiosStations.class);
        if (stations == null) {
            return List.of();
        }

        List<SourceReading> readings = new ArrayList<>();
        for (GiosStations.Station station : stations.stations()) {
            double lat = Double.parseDouble(station.latitude());
            double lon = Double.parseDouble(station.longitude());
            if (Geo.distanceKm(latitude, longitude, lat, lon) > radiusKm) {
                continue;
            }
            readings.addAll(toReadings(station, fetchPmData(station.id())));
        }
        return readings;
    }

    // returns data by pollutant code ("PM10", "PM2.5")
    private Map<String, GiosData> fetchPmData(long stationId) {
        GiosSensors sensors = restClient.get()
                .uri("/station/sensors/{id}", stationId)
                .retrieve()
                .body(GiosSensors.class);

        Map<String, GiosData> dataByCode = new HashMap<>();
        if (sensors == null) {
            return dataByCode;
        }
        for (GiosSensors.Sensor sensor : sensors.sensors()) {
            if (!"PM10".equals(sensor.code()) && !"PM2.5".equals(sensor.code())) {
                continue;
            }
            try {
                GiosData data = restClient.get()
                        .uri("/data/getData/{id}", sensor.id())
                        .retrieve()
                        .body(GiosData.class);
                if (data != null) {
                    dataByCode.put(sensor.code(), data);
                }
            } catch (HttpClientErrorException.BadRequest e) {
                // manual positions: results are published 4-8 weeks later, not in this API
            }
        }
        return dataByCode;
    }

    List<SourceReading> toReadings(GiosStations.Station station, Map<String, GiosData> dataByCode) {
        // hour -> {pm25, pm10}
        Map<String, Double[]> valuesByHour = new TreeMap<>();
        addValues(valuesByHour, dataByCode.get("PM2.5"), 0);
        addValues(valuesByHour, dataByCode.get("PM10"), 1);

        List<SourceReading> readings = new ArrayList<>();
        for (Map.Entry<String, Double[]> entry : valuesByHour.entrySet()) {
            readings.add(new SourceReading(
                    String.valueOf(station.id()),
                    station.name(),
                    Double.parseDouble(station.latitude()),
                    Double.parseDouble(station.longitude()),
                    null,
                    false,
                    parseDate(entry.getKey()),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    AVERAGING_MINUTES
            ));
        }
        return readings;
    }

    private void addValues(Map<String, Double[]> valuesByHour, GiosData data, int index) {
        if (data == null || data.values() == null) {
            return;
        }
        for (GiosData.Value value : data.values()) {
            // the newest hours are often not measured yet
            if (value.value() == null) {
                continue;
            }
            valuesByHour.computeIfAbsent(value.date(), date -> new Double[2])[index] = value.value();
        }
    }

    // dates look like "2026-11-07 21:00:00" in Polish local time and mark the end of the hour
    private Instant parseDate(String date) {
        return LocalDateTime.parse(date, DATE_FORMAT).atZone(POLISH_TIME).toInstant();
    }
}
