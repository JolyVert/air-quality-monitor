package pl.shkumat.airquality.source.gios;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of /station/sensors/{stationId}: measuring positions of one station,
 * one position per pollutant (PM10, PM2.5, NO2, ...).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosSensors(@JsonProperty("Lista stanowisk pomiarowych dla podanej stacji") List<Sensor> sensors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sensor(
            @JsonProperty("Identyfikator stanowiska") long id,
            @JsonProperty("Wskaźnik - kod") String code
    ) {
    }
}
