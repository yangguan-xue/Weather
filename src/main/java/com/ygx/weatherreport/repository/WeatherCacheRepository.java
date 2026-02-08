package com.ygx.weatherreport.repository;

import com.ygx.weatherreport.model.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {
    //查找未过期的天气缓存
    Optional<WeatherCache> findByCityAndWeatherTypeAndExpireAtAfter(
            String city, String weatherType, LocalDateTime now);

    //删除过期缓存
    /*
    @Modifying
    @Transactional
    @Query("DELETE FROM WeatherCache w WHERE w.expireAT < :now")
    int deleteExpiredCache(LocalDateTime now);
     */
}
