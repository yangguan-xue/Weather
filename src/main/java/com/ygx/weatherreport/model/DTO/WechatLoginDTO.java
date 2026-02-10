package com.ygx.weatherreport.model.DTO;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 *微信登录请求
 */
@Data
public class WechatLoginDTO {
    @NotBlank(message = "code不可以为空哦")
    private String code;    //微信登录code

    private String nickname;    //用户昵称
    private String avatarUrl;   //头像url
    private String city;    //用户城市

    public String getAvatarUrl() {
        return "";
    }
}
