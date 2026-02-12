// model/entity/WeatherCache.java
package com.ygx.weatherreport.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "weather_cache")
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(name = "location_key")
    private String locationKey;

    @Column(name = "weather_data", columnDefinition = "JSON")
    private String weatherData;

    @Column(name = "weather_type")
    private String weatherType = "now";

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}