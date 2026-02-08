package com.ygx.weatherreport.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户订阅实体类
 */
@Data
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;  // 关联的用户

    @Column(name = "city", nullable = false, length = 50)
    private String city;  // 订阅的城市

    @Column(name = "daily_time", nullable = false, length = 5)
    private String dailyTime;  // 每日推送时间，格式：HH:mm

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;  // 是否激活

    @Column(name = "created_at")
    private LocalDateTime createdAt;  // 创建时间

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 更新时间

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
