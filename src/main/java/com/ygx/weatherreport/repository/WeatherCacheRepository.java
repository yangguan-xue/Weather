// repository/WeatherCacheRepository.java
package com.ygx.weatherreport.repository;

import com.ygx.weatherreport.model.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {
    Optional<WeatherCache> findFirstByLocationKeyAndWeatherTypeOrderByCreatedAtDesc(
            String locationKey, String weatherType);
}