package pl.shkumat.airquality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs detection on a schedule. Starts a bit later than fetching, so the first run
 * already has some fresh measurements.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.fetch.enabled", havingValue = "true", matchIfMissing = true)
public class AnomalyScheduler {

    private final AnomalyDetectionService detectionService;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "${app.detection.interval}")
    public void detect() {
        try {
            int found = detectionService.run();
            log.info("DETECTION: {} local anomalies", found);
        } catch (Exception e) {
            log.warn("DETECTION: failed: {}", e.getMessage());
        }
    }
}
