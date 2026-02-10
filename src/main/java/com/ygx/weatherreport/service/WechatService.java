package com.ygx.weatherreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ygx.weatherreport.config.WechatConfig;
import com.ygx.weatherreport.model.DTO.LoginResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class WechatService {

    @Autowired
    private WechatConfig wechatConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 正式环境：调用微信官方接口获取openid和session_key
     */
    public LoginResponseDTO getWxSession(String code) {
        // 1. 验证关键配置参数，防止NPE
        if (wechatConfig == null) {
            log.error("致命错误：WechatConfig 配置对象未成功注入Spring容器，为 null。");
            throw new RuntimeException("系统配置错误：微信服务配置缺失");
        }

        String appid = wechatConfig.getAppid();
        String secret = wechatConfig.getSecret();
        // 注意：`wechatConfig.getUrl()` 返回的是一个 `Url` 对象，不是字符串。
        // 需要从该对象中获取 `code2session` 的具体值。
        String code2sessionUrl = (wechatConfig.getUrl() != null) ? wechatConfig.getUrl().getCode2session() : null;

        if (appid == null || appid.isEmpty()) {
            log.error("微信小程序 AppID 未在配置文件中设置 (wechat.appid)。");
            throw new RuntimeException("系统配置错误：微信 AppID 缺失");
        }
        if (secret == null || secret.isEmpty()) {
            log.error("微信小程序 AppSecret 未在配置文件中设置 (wechat.secret)。");
            throw new RuntimeException("系统配置错误：微信 AppSecret 缺失");
        }
        if (code2sessionUrl == null || code2sessionUrl.isEmpty()) {
            // 可以提供一个默认值，但强烈建议在配置中指定
            code2sessionUrl = "https://api.weixin.qq.com/sns/jscode2session";
            log.warn("微信 code2session 接口URL未配置，使用默认值: {}", code2sessionUrl);
        }
        if (code == null || code.isEmpty()) {
            log.error("调用微信登录接口时，前端传来的登录凭证 `code` 为空。");
            throw new RuntimeException("登录失败：无效的登录请求");
        }

        // 2. 使用 UriComponentsBuilder 安全地构建URL，避免字符串拼接和编码问题
        String requestUrl = UriComponentsBuilder.fromHttpUrl(code2sessionUrl)
                .queryParam("appid", appid)
                .queryParam("secret", secret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUriString();

        // 3. 安全地记录日志（避免在日志中暴露 AppSecret）
        String logSafeUrl = requestUrl.replace(secret, "******");
        log.info("准备调用微信服务器授权接口，URL: {}", logSafeUrl);

        try {
            // 4. 执行网络请求
            ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
            String responseBody = response.getBody();
            log.debug("微信服务器返回原始响应: {}", responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                log.error("微信接口返回的响应体为空。");
                throw new RuntimeException("微信服务暂时不可用，请稍后重试");
            }

            // 5. 解析响应
            LoginResponseDTO loginResponse = objectMapper.readValue(responseBody, LoginResponseDTO.class);

            // 6. 处理微信接口返回的业务错误
            if (loginResponse.getErrcode() != null && loginResponse.getErrcode() != 0) {
                // 根据微信错误码给出更友好的提示（示例）
                String errMsg = switch (loginResponse.getErrcode()) {
                    case 40029 -> "登录码（code）无效或已过期";
                    case 45011 -> "API 调用太频繁，请稍后重试";
                    case 40125 -> "AppSecret 配置错误";
                    case -1 -> "微信系统繁忙";
                    default -> loginResponse.getErrmsg();
                };
                log.error("微信接口返回业务错误。错误码: {}, 错误信息: {}", loginResponse.getErrcode(), errMsg);
                throw new RuntimeException("微信登录失败: " + errMsg);
            }

            if (loginResponse.getOpenid() == null || loginResponse.getOpenid().isEmpty()) {
                log.error("微信接口调用成功，但未返回有效的 openid。响应: {}", responseBody);
                throw new RuntimeException("微信登录失败：未能获取用户身份标识");
            }

            log.info("微信登录验证成功，获取到用户 OpenID: {}", loginResponse.getOpenid().substring(0, Math.min(8, loginResponse.getOpenid().length())) + "...");
            return loginResponse;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("网络连接异常，无法访问微信服务器。可能原因：网络不通、超时或DNS问题。", e);
            throw new RuntimeException("网络异常，无法连接微信服务");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("解析微信返回的JSON数据时发生异常。原始响应可能格式不正确。", e);
            throw new RuntimeException("微信服务响应异常");
        } catch (Exception e) {
            // 捕获其他未预见的异常
            log.error("调用微信登录接口时发生未知异常。", e);
            throw new RuntimeException("登录服务发生未知错误");
        }
    }
}