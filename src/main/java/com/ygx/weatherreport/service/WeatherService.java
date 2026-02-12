// service/WeatherService.java
package com.ygx.weatherreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ygx.weatherreport.model.DTO.CitySearchResponseDTO;
import com.ygx.weatherreport.model.DTO.WeatherResponseDTO;
import com.ygx.weatherreport.model.entity.WeatherCache;
import com.ygx.weatherreport.repository.WeatherCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WeatherService {

    @Value("${qweather.host}")
    private String Host;

    @Value("${qweather.api-key}")
    private String apiKey;

    @Autowired
    private WeatherCacheRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    public WeatherCache getWeather(String city, String locationKey) {
        log.info("获取天气: city={}, locationKey={}", city, locationKey);

        return repository.findFirstByLocationKeyAndWeatherTypeOrderByCreatedAtDesc(
                        locationKey, "now")
                .filter(cache -> cache.getExpireAt().isAfter(LocalDateTime.now()))
                .orElseGet(() -> fetchFromApiAndSave(city, locationKey));
    }

    private WeatherCache fetchFromApiAndSave(String city, String locationKey) {
        try {
            // 构建API请求
            String url = UriComponentsBuilder.fromHttpUrl(Host)
                    .path("/v7/weather/now")
                    .queryParam("location", locationKey)
                    .toUriString();

            log.debug("和风天气API: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-QW-Api-Key", apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 调用API
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API请求失败: " + response.getStatusCode());
            }

            // 解析响应
            String responseBody = response.getBody();
            WeatherResponseDTO weatherResponse = objectMapper.readValue(
                    responseBody, WeatherResponseDTO.class);

            if (!"200".equals(weatherResponse.getCode())) {
                throw new RuntimeException("和风天气错误: code=" + weatherResponse.getCode());
            }

            if (weatherResponse.getNow() == null) {
                throw new RuntimeException("未返回天气数据");
            }

            // 保存到缓存
            WeatherCache cache = new WeatherCache();
            cache.setCity(city);
            cache.setLocationKey(locationKey);
            cache.setWeatherType("now");
            cache.setWeatherData(objectMapper.writeValueAsString(weatherResponse.getNow()));
            cache.setExpireAt(LocalDateTime.now().plusHours(1));

            return repository.save(cache);

        } catch (Exception e) {
            log.error("获取天气失败", e);
            throw new RuntimeException("获取天气失败: " + e.getMessage());
        }
    }

    // 在WeatherService.java中添加
    @Autowired
    private CitySearchService citySearchService;

    /**
     * 使用城市搜索服务获取城市信息
     */
    public List<CitySearchResponseDTO.Location> searchCitiesForWeather(String keyword) {
        return citySearchService.searchCities(keyword);
    }
}