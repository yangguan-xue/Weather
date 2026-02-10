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
     */
    @PostMapping("/wechat-login")
    public ResponseWrapper<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginDTO loginDTO) {
        try {
            log.info("微信登录请求: code={}", loginDTO.getCode());

            // 1. 调用微信服务获取openid
            LoginResponseDTO wechatResponse = wechatService.getWxSession(loginDTO.getCode());

            if (wechatResponse == null || wechatResponse.getOpenid() == null) {
                return ResponseWrapper.error("微信登录失败");
            }

            String openid = wechatResponse.getOpenid();

            // 2. 查找或创建用户
            UserEntity user = userService.findOrCreateByOpenid(
                    openid,
                    loginDTO.getNickname(),
                    loginDTO.getAvatarUrl(),
                    loginDTO.getCity()
            );

            // 3. 生成JWT Token
            String token = jwtTokenUtil.generateToken(openid);

            // 4. 转换为UserDTO
            UserDTO userDTO = convertToUserDTO(user);

            // 5. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userDTO);
            data.put("expiresIn", 7200);

            return ResponseWrapper.success("登录成功", data);

        } catch (Exception e) {
            log.error("登录失败", e);
            return ResponseWrapper.error("登录失败");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseWrapper<UserDTO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseWrapper.error(401, "未提供Token");
            }

            String token = authHeader.substring(7);

            if (!jwtTokenUtil.validateToken(token)) {
                return ResponseWrapper.error(401, "Token无效");
            }

            String openid = jwtTokenUtil.getOpenidFromToken(token);
            UserEntity user = userService.findByOpenid(openid);

            if (user == null) {
                return ResponseWrapper.error(404, "用户不存在");
            }

            UserDTO userDTO = convertToUserDTO(user);
            return ResponseWrapper.success(userDTO);

        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseWrapper.error("获取用户信息失败");
        }
    }

    /**
     * 验证Token
     */
    @GetMapping("/verify")
    public ResponseWrapper<Map<String, Object>> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                result.put("valid", false);
                result.put("message", "未提供Token");
                return ResponseWrapper.success(result);
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtTokenUtil.validateToken(token);
            result.put("valid", isValid);

            if (isValid) {
                String openid = jwtTokenUtil.getOpenidFromToken(token);
                result.put("openid", openid);
                result.put("message", "Token有效");
            } else {
                result.put("message", "Token无效");
            }

            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("验证Token失败", e);
            result.put("valid", false);
            result.put("message", "验证失败");
            return ResponseWrapper.success(result);
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ResponseWrapper<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseWrapper.error(401, "未提供Token");
            }

            String oldToken = authHeader.substring(7);

            if (!jwtTokenUtil.validateToken(oldToken)) {
                return ResponseWrapper.error(401, "原Token无效");
            }

            String openid = jwtTokenUtil.getOpenidFromToken(oldToken);
            String newToken = jwtTokenUtil.generateToken(openid);

            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("expiresIn", 7200);

            return ResponseWrapper.success("刷新成功", data);

        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return ResponseWrapper.error("刷新失败");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseWrapper<Map<String, Object>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "auth-service");
        data.put("timestamp", LocalDateTime.now());
        return ResponseWrapper.success(data);
    }

    /**
     * 转换为UserDTO
     */
    private UserDTO convertToUserDTO(UserEntity user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}