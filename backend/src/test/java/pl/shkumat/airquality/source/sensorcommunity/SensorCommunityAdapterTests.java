package pl.shkumat.airquality.source.sensorcommunity;

import org.junit.jupiter.api.Test;
import pl.shkumat.airquality.source.SourceReading;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// uses a saved API response, so it does not need the network or the database
class SensorCommunityAdapterTests {

    private final SensorCommunityAdapter adapter = new SensorCommunityAdapter(51.11, 17.03, 10);

    @Test
    void convertsOnlyPmSensors() {
        List<SourceReading> readings = adapter.toReadings(loadSample());

        // BME280 has no PM values, the last SDS011 sends "unknown" instead of PM10
        assertThat(readings).extracting(SourceReading::externalId).containsExactly("68319", "97388");
    }

    @Test
    void mapsFieldsToCommonFormat() {
        SourceReading reading = adapter.toReadings(loadSample()).get(0);

        assertThat(reading.pm25()).isEqualTo(5.80000019073486);
        assertThat(reading.pm10()).isEqualTo(12.6000003814697);
        assertThat(reading.latitude()).isEqualTo(51.12);
        assertThat(reading.longitude()).isEqualTo(17.024);
        assertThat(reading.sensorType()).isEqualTo("SDS011");
        assertThat(reading.indoor()).isFalse();
        assertThat(reading.measuredAt()).isEqualTo(Instant.parse("2026-10-30T10:46:00Z"));
        assertThat(reading.averagingMinutes()).isEqualTo(5);
    }

    @Test
    void marksIndoorSensors() {
        SourceReading reading = adapter.toReadings(loadSample()).get(1);

        assertThat(reading.indoor()).isTrue();
    }

    private List<SensorCommunityRecord> loadSample() {
        InputStream json = getClass().getResourceAsStream("/sensor-community-sample.json");
        SensorCommunityRecord[] records = JsonMapper.builder().build().readValue(json, SensorCommunityRecord[].class);
        return Arrays.asList(records);
    }
}
