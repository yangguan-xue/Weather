package com.ygx.weatherreport.model.entity;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 *用户实体类
 */
@Data
@Entity
@Table(name = " users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "openid",unique = true,nullable = false,length = 100)
    private String openid;  //微信openid

    @Column(name = "nickname",length = 100)
    private String nickname;    //用户昵称

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;   //头像url

    @Column(name = "city", length = 50)
    private String city;    //用户常用城市

    @Column(name = "created_at")
    private LocalDateTime createdAt;    //创建时间

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;    //更新时间

    //在保存前自动设置创建时间
    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    //在更新前自动设置更新时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
