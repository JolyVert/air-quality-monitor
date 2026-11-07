package pl.shkumat.airquality.source.looko2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One sensor from the LookO2 GetAll response, only the fields we need.
 * All values come as strings, e.g. "PM25": "47", "Epoch": "1794044012".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LookO2Record(
        @JsonProperty("Device") String device,
        @JsonProperty("Name") String name,
        @JsonProperty("Lat") String latitude,
        @JsonProperty("Lon") String longitude,
        @JsonProperty("Epoch") String epoch,
        @JsonProperty("PM25") String pm25,
        @JsonProperty("PM10") String pm10,
        @JsonProperty("Indoor") String indoor
) {
}
