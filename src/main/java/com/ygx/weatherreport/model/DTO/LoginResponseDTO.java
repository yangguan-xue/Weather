package com.ygx.weatherreport.model.DTO;

import lombok.Data;

/**
 *登录响应
 */
@Data
public class LoginResponseDTO {
    private String openid;
    private String sessionKey;
    private String unionid;
    private Integer errcode;
    private String errmsg;
}
