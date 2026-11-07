package pl.shkumat.airquality.source;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient with timeouts for all adapters. All sources are fetched one after another
 * in one scheduler thread, so a request that hangs forever would stop fetching from every source.
 */
public final class RestClients {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    // Sensor.community sometimes needs a few dozen seconds for the area query
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private RestClients() {
    }

    public static RestClient.Builder builder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    public static RestClient create() {
        return builder().build();
    }
}
