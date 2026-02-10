package com.ygx.weatherreport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {
    private String appid = "wx7e32dcfe7de20fbd";
    private String secret = "7f1599cc42919be9098c71dc3d2d6f29";
    private Url url = new Url();
    private Token token = new Token();

    @Data
    public static class Url {
        private String code2session;
    }

    @Data
    public static class Token {
        private Integer expireTime;
        private String secretKey;
    }
}
