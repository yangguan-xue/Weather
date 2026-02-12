// model/DTO/WeatherResponseDTO.java
package com.ygx.weatherreport.model.DTO;

import lombok.Data;

@Data
public class WeatherResponseDTO {
    private String code;
    private String updateTime;
    private WeatherNow now;

    @Data
    public static class WeatherNow {
        private String obsTime;
        private String temp;
        private String feelsLike;
        private String text;
        private String windDir;
        private String windScale;
        private String humidity;
        private String windSpeed;
        private String pressure;
        private String vis;
    }
}