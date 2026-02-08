package com.ygx.weatherreport.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 天气缓存实体类
 */
@Data
@Entity
@Table(name = "weather_cache")
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", nullable = false, length = 50)
    private String city;  // 城市

    @Column(name = "location_key", length = 50)
    private String locationKey;  // 和风天气的位置key

    @Column(name = "weather_data", columnDefinition = "JSON")
    private String weatherData;  // 天气数据（JSON格式）

    @Column(name = "weather_type", length = 20)
    private String weatherType;  // 天气类型：now/forecast

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;  // 过期时间

    @Column(name = "created_at")
    private LocalDateTime createdAt;  // 创建时间

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}