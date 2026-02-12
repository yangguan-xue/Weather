package com.ygx.weatherreport.controller.login;

import com.ygx.weatherreport.model.DTO.LoginResponseDTO;
import com.ygx.weatherreport.model.DTO.UserDTO;
import com.ygx.weatherreport.model.DTO.WechatLoginDTO;
import com.ygx.weatherreport.model.entity.UserEntity;
import com.ygx.weatherreport.service.UserService;
import com.ygx.weatherreport.service.WechatService;
import com.ygx.weatherreport.utils.JWTTokenUtil;
import com.ygx.weatherreport.utils.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private WechatService wechatService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTTokenUtil jwtTokenUtil;

    /**
     * 微信登录接口
     * POST /api/auth/wechat-login
     *
     * 完整流程：
     * 1. 前端传递code和用户信息
     * 2. 调用微信接口获取openid
     * 3. 根据openid创建/更新用户
     * 4. 生成JWT Token返回
     */
    @PostMapping("/wechat-login")
    public ResponseWrapper<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginDTO loginDTO) {
        String requestId = "REQ_" + System.currentTimeMillis();

        try {
            log.info("[{}] === 微信登录请求开始 ===", requestId);
            log.info("[{}] 接收参数 - code: {}, nickname: {}, avatarUrl: {}, city: {}",
                    requestId,
                    loginDTO.getCode(),
                    loginDTO.getNickname(),
                    loginDTO.getAvatarUrl(),
                    loginDTO.getCity());

            // 1. 参数验证
            if (loginDTO.getCode() == null || loginDTO.getCode().trim().isEmpty()) {
                log.error("[{}] 登录失败: code不能为空", requestId);
                return ResponseWrapper.error(400, "登录失败: code不能为空");
            }

            // 2. 调用微信服务获取openid和session_key
            log.info("[{}] 调用微信接口获取openid...", requestId);
            LoginResponseDTO wechatResponse = wechatService.getWxSession(loginDTO.getCode());

            if (wechatResponse == null) {
                log.error("[{}] 微信接口返回空响应", requestId);
                return ResponseWrapper.error(500, "微信服务暂时不可用");
            }

            // 3. 检查微信接口返回的错误
            if (wechatResponse.getErrcode() != null && wechatResponse.getErrcode() != 0) {
                log.error("[{}] 微信接口返回错误: errcode={}, errmsg={}",
                        requestId, wechatResponse.getErrcode(), wechatResponse.getErrmsg());
                return ResponseWrapper.error(400, "微信登录失败: " + wechatResponse.getErrmsg());
            }

            if (wechatResponse.getOpenid() == null || wechatResponse.getOpenid().isEmpty()) {
                log.error("[{}] 微信接口未返回openid", requestId);
                return ResponseWrapper.error(400, "微信登录失败: 未获取到用户标识");
            }

            String openid = wechatResponse.getOpenid();
            log.info("[{}] 成功获取用户openid: {}", requestId, openid);

            // 4. 处理用户信息
            log.info("[{}] 处理用户信息 - nickname: '{}', avatarUrl: '{}', city: '{}'",
                    requestId,
                    loginDTO.getNickname(),
                    loginDTO.getAvatarUrl(),
                    loginDTO.getCity());

            // 确保用户信息不为空
            String nickname = loginDTO.getNickname() != null ? loginDTO.getNickname() : "微信用户";
            String avatarUrl = loginDTO.getAvatarUrl() != null ? loginDTO.getAvatarUrl() : "";
            String city = loginDTO.getCity() != null ? loginDTO.getCity() : "未知";

            // 5. 查找或创建用户
            UserEntity user = userService.findOrCreateByOpenid(
                    openid,
                    nickname,
                    avatarUrl,
                    city
            );

            if (user == null) {
                log.error("[{}] 用户创建失败", requestId);
                return ResponseWrapper.error(500, "用户信息保存失败");
            }

            log.info("[{}] 用户处理完成 - ID: {}, 昵称: {}, 头像: {}, 城市: {}, 创建时间: {}, 更新时间: {}",
                    requestId,
                    user.getId(),
                    user.getNickname(),
                    user.getAvatarUrl(),
                    user.getCity(),
                    user.getCreatedAt(),
                    user.getUpdatedAt());

            // 6. 生成JWT Token
            String token = jwtTokenUtil.generateToken(openid);
            log.info("[{}] 生成JWT Token成功", requestId);

            // 7. 转换为UserDTO
            UserDTO userDTO = convertToUserDTO(user);

            // 8. 判断是否是新用户
            boolean isNewUser = isNewUser(user);
            log.info("[{}] 是否是新用户: {}", requestId, isNewUser);

            // 9. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userDTO);
            data.put("isNewUser", isNewUser);
            data.put("expiresIn", 7200);

            log.info("[{}] === 登录成功，返回数据 ===", requestId);
            log.info("[{}] 返回用户ID: {}, 昵称: {}", requestId, userDTO.getId(), userDTO.getNickname());

            return ResponseWrapper.success("登录成功", data);

        } catch (Exception e) {
            log.error("[{}] 登录过程发生异常", requestId, e);
            return ResponseWrapper.error(500, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseWrapper<UserDTO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String requestId = "ME_" + System.currentTimeMillis();

        try {
            log.info("[{}] === 获取当前用户信息开始 ===", requestId);

            // 1. 验证Authorization头格式
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[{}] Token格式错误或未提供", requestId);
                return ResponseWrapper.error(401, "未提供有效的Token");
            }

            String token = authHeader.substring(7);
            log.info("[{}] 提取Token成功，长度: {}", requestId, token.length());

            // 2. 验证Token有效性
            if (!jwtTokenUtil.validateToken(token)) {
                log.warn("[{}] Token无效或已过期", requestId);
                return ResponseWrapper.error(401, "Token无效或已过期，请重新登录");
            }

            // 3. 从Token中获取openid
            String openid = jwtTokenUtil.getOpenidFromToken(token);
            log.info("[{}] 从Token解析openid: {}", requestId, openid);

            // 4. 查询用户信息
            UserEntity user = userService.findByOpenid(openid);
            if (user == null) {
                log.error("[{}] 用户不存在: openid={}", requestId, openid);
                return ResponseWrapper.error(404, "用户不存在");
            }

            // 5. 转换为DTO
            UserDTO userDTO = convertToUserDTO(user);
            log.info("[{}] 获取用户信息成功: ID={}, nickname={}",
                    requestId, userDTO.getId(), userDTO.getNickname());

            return ResponseWrapper.success(userDTO);

        } catch (Exception e) {
            log.error("[{}] 获取用户信息失败", requestId, e);
            return ResponseWrapper.error(500, "获取用户信息失败");
        }
    }

    /**
     * 验证Token有效性
     * GET /api/auth/verify
     */
    @GetMapping("/verify")
    public ResponseWrapper<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String requestId = "VERIFY_" + System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("[{}] === Token验证开始 ===", requestId);

            // 1. 检查Authorization头
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[{}] Token格式错误或未提供", requestId);
                result.put("valid", false);
                result.put("message", "Token格式错误或未提供");
                result.put("code", 401);
                return ResponseWrapper.success(result);
            }

            String token = authHeader.substring(7);
            log.info("[{}] 验证Token，长度: {}", requestId, token.length());

            // 2. 验证Token
            boolean isValid = jwtTokenUtil.validateToken(token);
            result.put("valid", isValid);

            if (isValid) {
                // 3. 获取Token中的信息
                String openid = jwtTokenUtil.getOpenidFromToken(token);
                result.put("openid", openid);
                result.put("message", "Token有效");
                result.put("code", 200);

                // 4. 获取过期时间
                try {
                    java.util.Date expiration = jwtTokenUtil.getExpirationDateFromToken(token);
                    result.put("expiresAt", expiration);
                    result.put("remainingSeconds",
                            (expiration.getTime() - System.currentTimeMillis()) / 1000);
                } catch (Exception e) {
                    log.warn("[{}] 获取Token过期时间失败", requestId, e);
                }

                log.info("[{}] Token验证成功: openid={}", requestId, openid);

            } else {
                result.put("message", "Token无效或已过期");
                result.put("code", 401);
                log.warn("[{}] Token验证失败", requestId);
            }

            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("[{}] Token验证过程发生异常", requestId, e);
            result.put("valid", false);
            result.put("message", "Token验证异常");
            result.put("code", 500);
            return ResponseWrapper.success(result);
        }
    }

    /**
     * 刷新Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseWrapper<Map<String, Object>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        String requestId = "REFRESH_" + System.currentTimeMillis();

        try {
            log.info("[{}] === Token刷新开始 ===", requestId);

            // 1. 验证Authorization头格式
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[{}] Token格式错误", requestId);
                return ResponseWrapper.error(401, "Token格式错误");
            }

            String oldToken = authHeader.substring(7);
            log.info("[{}] 获取旧Token，长度: {}", requestId, oldToken.length());

            // 2. 验证旧Token
            if (!jwtTokenUtil.validateToken(oldToken)) {
                log.warn("[{}] 旧Token无效或已过期", requestId);
                return ResponseWrapper.error(401, "原Token无效或已过期，请重新登录");
            }

            // 3. 从旧Token中获取openid
            String openid = jwtTokenUtil.getOpenidFromToken(oldToken);
            log.info("[{}] 从旧Token解析openid: {}", requestId, openid);

            // 4. 生成新Token
            String newToken = jwtTokenUtil.generateToken(openid);
            log.info("[{}] 生成新Token成功", requestId);

            // 5. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("expiresIn", 7200);

            log.info("[{}] Token刷新成功: openid={}", requestId, openid);
            return ResponseWrapper.success("Token刷新成功", data);

        } catch (Exception e) {
            log.error("[{}] Token刷新失败", requestId, e);
            return ResponseWrapper.error(500, "Token刷新失败");
        }
    }

    /**
     * 服务健康检查
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseWrapper<Map<String, Object>> healthCheck() {
        String requestId = "HEALTH_" + System.currentTimeMillis();

        try {
            log.info("[{}] 健康检查请求", requestId);

            Map<String, Object> data = new HashMap<>();
            data.put("status", "UP");
            data.put("service", "auth-service");
            data.put("timestamp", LocalDateTime.now());
            data.put("version", "1.0.0");
            data.put("requestId", requestId);

            // 检查依赖服务状态
            Map<String, String> dependencies = new HashMap<>();
            dependencies.put("database", "CONNECTED");
            dependencies.put("jwt", "ENABLED");
            dependencies.put("wechat", "CONFIGURED");
            data.put("dependencies", dependencies);

            log.info("[{}] 健康检查完成: 服务正常", requestId);
            return ResponseWrapper.success(data);

        } catch (Exception e) {
            log.error("[{}] 健康检查异常", requestId, e);

            Map<String, Object> data = new HashMap<>();
            data.put("status", "DOWN");
            data.put("error", e.getMessage());
            data.put("timestamp", LocalDateTime.now());

            return ResponseWrapper.error(503, "服务异常");
        }
    }

    /**
     * 调试接口：返回接收到的原始数据
     * POST /api/auth/debug
     */
    @PostMapping("/debug")
    public ResponseWrapper<Map<String, Object>> debugLogin(@RequestBody WechatLoginDTO loginDTO) {
        String requestId = "DEBUG_" + System.currentTimeMillis();

        log.info("[{}] === 调试接口调用 ===", requestId);
        log.info("[{}] 原始数据: code={}, nickname={}, avatarUrl={}, city={}",
                requestId,
                loginDTO.getCode(),
                loginDTO.getNickname(),
                loginDTO.getAvatarUrl(),
                loginDTO.getCity());

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("receivedData", loginDTO);
        data.put("timestamp", LocalDateTime.now());
        data.put("message", "数据接收成功，请查看日志");

        return ResponseWrapper.success("调试数据接收成功", data);
    }

    /**
     * 转换为UserDTO
     */
    private UserDTO convertToUserDTO(UserEntity user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        try {
            BeanUtils.copyProperties(user, dto);
        } catch (Exception e) {
            log.error("用户对象转换失败", e);
        }
        return dto;
    }

    /**
     * 判断是否为新用户
     * 规则：创建时间与更新时间相差小于2秒认为是新用户
     */
    private boolean isNewUser(UserEntity user) {
        if (user.getCreatedAt() == null || user.getUpdatedAt() == null) {
            return true;
        }

        long diffSeconds = java.time.Duration.between(user.getCreatedAt(), user.getUpdatedAt()).getSeconds();
        return Math.abs(diffSeconds) < 2;
    }
}