package com.ygx.weatherreport.model.DTO;


import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息DTO
 */
@Data
public class UserDTO {
    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String city;
    private LocalDateTime createdAt;
}
