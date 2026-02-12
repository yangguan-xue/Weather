// controller/weather/WeatherController.java
package com.ygx.weatherreport.controller.weather;

import com.ygx.weatherreport.model.entity.WeatherCache;
import com.ygx.weatherreport.service.WeatherService;
import com.ygx.weatherreport.utils.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/weather")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    /**
     * 获取实时天气
     */
    @GetMapping("/now")
    public ResponseWrapper<WeatherCache> getWeather(
            @RequestParam String city,
            @RequestParam String locationKey) {
        try {
            WeatherCache weather = weatherService.getWeather(city, locationKey);
            return ResponseWrapper.success(weather);
        } catch (Exception e) {
            log.error("获取天气异常", e);
            return ResponseWrapper.error(e.getMessage());
        }
    }
}