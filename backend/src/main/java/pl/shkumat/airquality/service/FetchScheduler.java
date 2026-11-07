package pl.shkumat.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.shkumat.airquality.source.SourceAdapter;
import pl.shkumat.airquality.source.SourceReading;
import pl.shkumat.airquality.source.airly.AirlyAdapter;
import pl.shkumat.airquality.source.looko2.LookO2Adapter;
import pl.shkumat.airquality.source.sensorcommunity.SensorCommunityAdapter;

import java.time.Duration;
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
    private final AirlyAdapter airlyAdapter;
    private final LookO2Adapter lookO2Adapter;
    private final MeasurementImportService importService;

    @Value("${app.airly.fetch-interval}")
    private Duration airlyInterval;

    @Value("${app.looko2.fetch-interval}")
    private Duration lookO2Interval;

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "${app.sensor-community.fetch-interval}")
    public void fetchSensorCommunity() {
        fetch(sensorCommunityAdapter);
    }

    @Scheduled(initialDelayString = "PT15S", fixedDelayString = "${app.airly.fetch-interval}")
    public void fetchAirly() {
        // one run uses about 17 of 100 daily requests, so restarting the app must not fetch again
        if (importService.fetchedWithin(airlyAdapter.getSource(), airlyInterval.minusMinutes(5))) {
            log.info("AIRLY: fetched less than {} ago, skipping", airlyInterval);
            return;
        }
        fetch(airlyAdapter);
    }

    @Scheduled(initialDelayString = "PT20S", fixedDelayString = "${app.looko2.fetch-interval}")
    public void fetchLookO2() {
        // the token is blocked after calls more often than every 30 minutes, also after a restart
        if (importService.fetchedWithin(lookO2Adapter.getSource(), lookO2Interval)) {
            log.info("LOOKO2: fetched less than {} ago, skipping", lookO2Interval);
            return;
        }
        fetch(lookO2Adapter);
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
