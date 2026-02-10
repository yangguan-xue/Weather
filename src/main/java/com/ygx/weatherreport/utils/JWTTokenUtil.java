package com.ygx.weatherreport.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JWTTokenUtil {

    @Value("${jwt.secret:your-default-secret-key-for-development-only}")
    private String secretKey;

    @Value("${jwt.expiration:7200}")
    private Long expiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        // 确保密钥长度足够（至少32个字符）
        if (secretKey.length() < 32) {
            log.warn("JWT密钥长度不足32位，建议在生产环境使用更长的密钥");
            // 如果密钥太短，使用默认的安全密钥
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * 生成JWT Token
     * @param openid 用户openid
     * @return JWT Token字符串
     */
    public String generateToken(String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("openid", openid);
        claims.put("type", "access_token");

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(openid)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取openid
     * @param token JWT Token
     * @return 用户openid
     */
    public String getOpenidFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从Token中解析openid失败", e);
            throw new RuntimeException("Token解析失败");
        }
    }

    /**
     * 获取Token的过期时间
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("获取Token过期时间失败", e);
            throw new RuntimeException("Token解析失败");
        }
    }

    /**
     * 检查Token是否即将过期（剩余时间小于指定阈值）
     * @param token JWT Token
     * @param thresholdSeconds 阈值（秒）
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token, long thresholdSeconds) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            Date now = new Date();
            long remainingMillis = expirationDate.getTime() - now.getTime();
            return remainingMillis < (thresholdSeconds * 1000);
        } catch (Exception e) {
            return true; // 如果解析失败，认为需要刷新
        }
    }

    /**
     * 生成刷新Token（可选功能）
     * @param openid 用户openid
     * @return 刷新Token
     */
    public String generateRefreshToken(String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("openid", openid);
        claims.put("type", "refresh_token");

        Date now = new Date();
        // 刷新Token有效期更长，例如7天
        Date expirationDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(openid)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证刷新Token
     * @param token 刷新Token
     * @return 是否有效
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 检查Token类型
            String type = (String) claims.get("type");
            return "refresh_token".equals(type);
        } catch (Exception e) {
            log.debug("刷新Token验证失败: {}", e.getMessage());
            return false;
        }
    }
}