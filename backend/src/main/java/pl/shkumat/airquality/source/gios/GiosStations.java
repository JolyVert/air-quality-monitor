package pl.shkumat.airquality.source.gios;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of /station/findAll. Field names in this API are in Polish.
 * Coordinates come as strings, e.g. "WGS84 φ N": "51.129378".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GiosStations(@JsonProperty("Lista stacji pomiarowych") List<Station> stations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Station(
            @JsonProperty("Identyfikator stacji") long id,
            @JsonProperty("Nazwa stacji") String name,
            @JsonProperty("WGS84 φ N") String latitude,
            @JsonProperty("WGS84 λ E") String longitude
    ) {
    }
}
