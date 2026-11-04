package pl.shkumat.airquality.source.airly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Airly installation (one sensor device) from /v2/installations/nearest.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AirlyInstallation(long id, Location location, Address address) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(String street) {
    }
}
