package pl.shkumat.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;
import pl.shkumat.airquality.source.sensorcommunity.SensorCommunityAdapter;

import java.util.List;

/**
 * Fetches data from all sources on a schedule. Each source has its own interval
 * because the APIs update data at different rates and have different limits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.fetch.enabled", havingValue = "true", matchIfMissing = true)
public class FetchScheduler {

    private final SensorCommunityAdapter sensorCommunityAdapter;
    private final MeasurementImportService importService;

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "${app.sensor-community.fetch-interval}")
    public void fetchSensorCommunity() {
        fetch(sensorCommunityAdapter);
    }

    private void fetch(SourceAdapter adapter) {
        try {
            List<SourceReading> readings = adapter.fetchReadings();
            importService.save(adapter.getSource(), readings);
            log.info("{}: saved {} readings", adapter.getSource(), readings.size());
        } catch (Exception e) {
            // the next scheduled run will try again
            log.warn("{}: fetching failed: {}", adapter.getSource(), e.getMessage());
        }
    }
}
