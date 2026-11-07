package pl.shkumat.airquality.source.gios;

import org.junit.jupiter.api.Test;
import pl.shkumat.airquality.source.SourceReading;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// uses saved API responses, so it does not need the network or the database
class GiosAdapterTests {

    private final GiosAdapter adapter = new GiosAdapter(51.11, 17.03, 10);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void joinsPm10AndPm25ByHour() {
        List<SourceReading> readings = adapter.toReadings(loadStation(), Map.of(
                "PM2.5", load("/gios-pm25-sample.json", GiosData.class),
                "PM10", load("/gios-pm10-sample.json", GiosData.class)));

        // 21:00 has no value yet; 18:00 has only PM10, 19:00 only PM2.5
        assertThat(readings).extracting(SourceReading::pm25).containsExactly(null, 40.7, 36.5);
        assertThat(readings).extracting(SourceReading::pm10).containsExactly(55.0, null, 50.1);
    }

    @Test
    void mapsFieldsToCommonFormat() {
        SourceReading reading = adapter.toReadings(loadStation(), Map.of(
                "PM2.5", load("/gios-pm25-sample.json", GiosData.class))).get(1);

        assertThat(reading.externalId()).isEqualTo("117");
        assertThat(reading.name()).isEqualTo("Wrocław, wyb. Conrada-Korzeniowskiego");
        assertThat(reading.latitude()).isEqualTo(51.129378);
        assertThat(reading.longitude()).isEqualTo(17.02925);
        // 20:00 Polish winter time (UTC+1)
        assertThat(reading.measuredAt()).isEqualTo(Instant.parse("2026-11-07T19:00:00Z"));
        assertThat(reading.averagingMinutes()).isEqualTo(60);
    }

    @Test
    void skipsStationWithoutData() {
        assertThat(adapter.toReadings(loadStation(), Map.of())).isEmpty();
    }

    private GiosStations.Station loadStation() {
        return load("/gios-stations-sample.json", GiosStations.class).stations().get(0);
    }

    private <T> T load(String file, Class<T> type) {
        return jsonMapper.readValue(getClass().getResourceAsStream(file), type);
    }
}
