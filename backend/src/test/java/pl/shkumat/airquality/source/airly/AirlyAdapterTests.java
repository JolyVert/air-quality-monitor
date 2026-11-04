package pl.shkumat.airquality.source.airly;

import org.junit.jupiter.api.Test;
import pl.shkumat.airquality.source.SourceReading;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// uses a saved API response, so it does not need the network or the database
class AirlyAdapterTests {

    private final AirlyAdapter adapter = new AirlyAdapter("test-key", 51.11, 17.03, 10, 16);

    private final AirlyInstallation installation = new AirlyInstallation(35894,
            new AirlyInstallation.Location(51.106439, 17.033665),
            new AirlyInstallation.Address("Widok"));

    @Test
    void usesHistoryAndSkipsHoursWithoutPm() {
        List<SourceReading> readings = adapter.toReadings(installation, loadSample());

        // 4 hours in history, one of them has no values at all; "current" is not used
        assertThat(readings).extracting(SourceReading::measuredAt).containsExactly(
                Instant.parse("2026-11-03T11:00:00Z"),
                Instant.parse("2026-11-03T13:00:00Z"),
                Instant.parse("2026-11-04T10:00:00Z"));
    }

    @Test
    void mapsFieldsToCommonFormat() {
        SourceReading reading = adapter.toReadings(installation, loadSample()).get(0);

        assertThat(reading.externalId()).isEqualTo("35894");
        assertThat(reading.name()).isEqualTo("Widok");
        assertThat(reading.latitude()).isEqualTo(51.106439);
        assertThat(reading.longitude()).isEqualTo(17.033665);
        assertThat(reading.pm25()).isEqualTo(6.73);
        assertThat(reading.pm10()).isEqualTo(7.78);
        assertThat(reading.averagingMinutes()).isEqualTo(60);
        assertThat(reading.indoor()).isFalse();
    }

    @Test
    void keepsHourWithOnlyPm10() {
        SourceReading reading = adapter.toReadings(installation, loadSample()).get(1);

        assertThat(reading.pm25()).isNull();
        assertThat(reading.pm10()).isEqualTo(8.95);
    }

    private AirlyMeasurements loadSample() {
        InputStream json = getClass().getResourceAsStream("/airly-measurements-sample.json");
        return JsonMapper.builder().build().readValue(json, AirlyMeasurements.class);
    }
}
