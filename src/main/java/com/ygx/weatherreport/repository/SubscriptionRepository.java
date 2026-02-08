package com.ygx.weatherreport.repository;

import com.ygx.weatherreport.model.entity.Subscription;
import com.ygx.weatherreport.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // 查找用户的所有订阅
    List<Subscription> findByUser(UserEntity user);

    // 查找用户的所有活跃订阅
    List<Subscription> findByUserAndIsActiveTrue(UserEntity user);

    // 查找特定城市的活跃订阅
    List<Subscription> findByCityAndIsActiveTrue(String city);

    // 查找用户的特定城市订阅
    Optional<Subscription> findByUserAndCity(UserEntity user, String city);

    // 检查用户是否已订阅某个城市
    boolean existsByUserAndCity(UserEntity user, String city);

    // 统计用户订阅数量
    long countByUser(UserEntity user);
}
