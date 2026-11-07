package pl.shkumat.airquality.source.gios;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of /data/getData/{sensorId}: hourly values of one position, newest first.
 * "Data" is local Polish time and marks the end of the hour, "Wartość" is null if not measured yet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosData(@JsonProperty("Lista danych pomiarowych") List<Value> values) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            @JsonProperty("Data") String date,
            @JsonProperty("Wartość") Double value
    ) {
    }
}
