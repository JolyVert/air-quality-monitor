package pl.shkumat.airquality.source;

import pl.shkumat.airquality.model.SensorSource;

import java.util.List;

/**
 * Adapter for one external API. To add a new data source, implement this interface
 * and add a scheduled method for it in FetchScheduler.
 */
public interface SourceAdapter {

    SensorSource getSource();

    List<SourceReading> fetchReadings();
}
