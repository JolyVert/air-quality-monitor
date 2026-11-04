package pl.shkumat.airquality;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.fetch.enabled=false")
class AirQualityMonitorApplicationTests {

	@Test
	void contextLoads() {
	}

}
