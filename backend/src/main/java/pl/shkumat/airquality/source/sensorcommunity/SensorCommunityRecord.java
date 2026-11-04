package pl.shkumat.airquality.source.sensorcommunity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One record from the Sensor.community API, only the fields we need.
 * Numbers come as strings in this API, e.g. "latitude": "51.12".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SensorCommunityRecord(
        String timestamp,
        Location location,
        Sensor sensor,
        @JsonProperty("sensordatavalues") List<Value> values
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(String latitude, String longitude, int indoor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sensor(long id, @JsonProperty("sensor_type") SensorType sensorType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SensorType(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(@JsonProperty("value_type") String valueType, String value) {
    }
}
