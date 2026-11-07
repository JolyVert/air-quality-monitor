package pl.shkumat.airquality.source.looko2;

import org.junit.jupiter.api.Test;
import pl.shkumat.airquality.source.SourceReading;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// uses a saved API response, so it does not need the network or the database
class LookO2AdapterTests {

    private static final Instant NOW = Instant.parse("2026-11-07T09:40:00Z");

    private final LookO2Adapter adapter = new LookO2Adapter(51.11, 17.03, 10, "XXX");

    @Test
    void keepsOnlyFreshSensorsInArea() {
        List<SourceReading> readings = adapter.toReadings(loadSample(), NOW);

        // WROCLAW_ZAL stopped in 2018, KRAKOW_OLSZA is outside the area, Lawendowa has empty PM2.5
        assertThat(readings).extracting(SourceReading::externalId).containsExactly("6001945521DF", "4C7525922634");
    }

    @Test
    void mapsFieldsToCommonFormat() {
        SourceReading reading = adapter.toReadings(loadSample(), NOW).get(0);

        assertThat(reading.name()).isEqualTo("Wroclaw_Swiniary");
        assertThat(reading.pm25()).isEqualTo(47.0);
        assertThat(reading.pm10()).isEqualTo(58.0);
        assertThat(reading.latitude()).isEqualTo(51.1923);
        assertThat(reading.longitude()).isEqualTo(16.9784);
        assertThat(reading.indoor()).isFalse();
        assertThat(reading.measuredAt()).isEqualTo(Instant.ofEpochSecond(1794044012));
        assertThat(reading.averagingMinutes()).isEqualTo(1);
    }

    @Test
    void marksIndoorSensors() {
        SourceReading reading = adapter.toReadings(loadSample(), NOW).get(1);

        assertThat(reading.indoor()).isTrue();
    }

    private List<LookO2Record> loadSample() {
        InputStream json = getClass().getResourceAsStream("/looko2-sample.json");
        LookO2Record[] records = JsonMapper.builder().build().readValue(json, LookO2Record[].class);
        return Arrays.asList(records);
    }
}
