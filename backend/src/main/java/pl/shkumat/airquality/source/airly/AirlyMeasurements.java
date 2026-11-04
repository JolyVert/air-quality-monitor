package pl.shkumat.airquality.source.airly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response of /v2/measurements/installation. We only use "history":
 * hourly averages for the last 24 full hours. "current" is a moving average
 * that shifts with every request, so it is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AirlyMeasurements(List<Period> history) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Period(String fromDateTime, String tillDateTime, List<Value> values) {
    }

    // name is e.g. "PM25", "PM10", "TEMPERATURE"
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(String name, Double value) {
    }
}
