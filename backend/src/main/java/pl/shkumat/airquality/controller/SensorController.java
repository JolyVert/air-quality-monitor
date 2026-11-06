package pl.shkumat.airquality.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.shkumat.airquality.dto.MeasurementDto;
import pl.shkumat.airquality.dto.SensorDto;
import pl.shkumat.airquality.service.SensorService;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private static final int MAX_HOURS = 7 * 24;

    private final SensorService sensorService;

    @GetMapping
    public List<SensorDto> getSensors() {
        return sensorService.getSensors();
    }

    @GetMapping("/{id}/measurements")
    public List<MeasurementDto> getMeasurements(@PathVariable Long id,
                                                @RequestParam(defaultValue = "24") int hours) {
        return sensorService.getMeasurements(id, Math.clamp(hours, 1, MAX_HOURS));
    }
}
