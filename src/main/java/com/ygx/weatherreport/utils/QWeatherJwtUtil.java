// utils/QWeatherJwtUtil.java
package com.ygx.weatherreport.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class QWeatherJwtUtil {

    static {
        // 注册BouncyCastle提供者
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${qweather.project-id}")
    private String projectId;

    @Value("${qweather.credential-id}")
    private String credentialId;

    @Value("${qweather.private-key}")
    private String privateKeyBase64; // PKCS#8格式的Base64私钥

    @Value("${qweather.public-key}")
    private String publicKeyBase64; // X.509格式的Base64公钥

    /**
     * 生成用于和风天气API调用的JWT Token
     */
    public String generateQWeatherToken() {
        try {
            log.info("开始生成JWT Token...");

            // 1. 提取原始32字节Ed25519密钥
            byte[] rawPrivateKey = extractRawEd25519PrivateKey();
            byte[] rawPublicKey = extractRawEd25519PublicKey();

            log.info("密钥提取成功: 私钥{}字节, 公钥{}字节",
                    rawPrivateKey.length, rawPublicKey.length);

            // 2. 构建Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "EdDSA");
            header.put("kid", credentialId);
            header.put("typ", "JWT");

            // 3. 构建Payload
            long nowSeconds = Instant.now().getEpochSecond();
            long iat = nowSeconds - 30; // 签发时间设置为30秒前
            long exp = nowSeconds + 300; // 5分钟有效期

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(projectId)
                    .issueTime(Date.from(Instant.ofEpochSecond(iat)))
                    .expirationTime(Date.from(Instant.ofEpochSecond(exp)))
                    .build();

            // 4. 将原始密钥转换为Base64URL字符串
            String publicKeyBase64Url = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawPublicKey);

            String privateKeyBase64Url = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawPrivateKey);

            log.debug("公钥Base64URL: {}", publicKeyBase64Url);
            log.debug("私钥Base64URL: {}", privateKeyBase64Url.substring(0, Math.min(20, privateKeyBase64Url.length())) + "...");

            // 5. 创建OctetKeyPair
            OctetKeyPair jwk = new OctetKeyPair.Builder(
                    Curve.Ed25519,
                    Base64URL.from(publicKeyBase64Url)  // 使用Base64URL对象
            )
                    .keyID(credentialId)
                    .d(Base64URL.from(privateKeyBase64Url))  // 使用Base64URL对象
                    .build();

            // 6. 创建签名器并签名
            Ed25519Signer signer = new Ed25519Signer(jwk);

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                            .keyID(credentialId)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims
            );

            signedJWT.sign(signer);

            String token = signedJWT.serialize();
            log.info("✅ JWT Token生成成功，长度: {}字符", token.length());
            log.info("JWT Token前50字符: {}", token);

            return token;

        } catch (Exception e) {
            log.error("❌ 生成JWT Token失败", e);
            throw new RuntimeException("生成API认证令牌失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从PKCS#8格式提取原始32字节Ed25519私钥
     */
    private byte[] extractRawEd25519PrivateKey() throws Exception {
        try {
            // 清理密钥字符串
            String cleanKey = privateKeyBase64.trim()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            // 解码Base64
            byte[] pkcs8Bytes = Base64.getDecoder().decode(cleanKey);
            log.debug("PKCS#8私钥解码长度: {}字节", pkcs8Bytes.length);

            // 创建PKCS8EncodedKeySpec
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);

            // 获取KeyFactory实例
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // 获取原始密钥字节
            byte[] encoded = privateKey.getEncoded();

            // 从PKCS#8格式中提取原始32字节私钥
            // Ed25519的PKCS#8格式：通常是16字节头部 + 32字节私钥
            if (encoded.length >= 48) {
                byte[] rawKey = new byte[32];
                System.arraycopy(encoded, encoded.length - 32, rawKey, 0, 32);
                return rawKey;
            }
            // 如果已经是32字节，直接返回
            else if (encoded.length == 32) {
                return encoded;
            }
            else {
                throw new IllegalArgumentException(
                        String.format("无法识别的私钥格式: 解码后长度=%d字节", encoded.length)
                );
            }

        } catch (Exception e) {
            log.error("私钥提取失败", e);
            throw new RuntimeException("私钥提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从X.509格式提取原始32字节Ed25519公钥
     */
    private byte[] extractRawEd25519PublicKey() throws Exception {
        try {
            // 清理密钥字符串
            String cleanKey = publicKeyBase64.trim()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            // 解码Base64
            byte[] x509Bytes = Base64.getDecoder().decode(cleanKey);
            log.debug("X.509公钥解码长度: {}字节", x509Bytes.length);

            // 创建X509EncodedKeySpec
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509Bytes);

            // 获取KeyFactory实例
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // 获取原始密钥字节
            byte[] encoded = publicKey.getEncoded();

            // 从X.509格式中提取原始32字节公钥
            // Ed25519的X.509格式：通常是12字节头部 + 32字节公钥
            if (encoded.length >= 44) {
                byte[] rawKey = new byte[32];
                System.arraycopy(encoded, encoded.length - 32, rawKey, 0, 32);
                return rawKey;
            }
            // 如果已经是32字节，直接返回
            else if (encoded.length == 32) {
                return encoded;
            }
            else {
                throw new IllegalArgumentException(
                        String.format("无法识别的公钥格式: 解码后长度=%d字节", encoded.length)
                );
            }

        } catch (Exception e) {
            log.error("公钥提取失败", e);
            throw new RuntimeException("公钥提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 诊断工具：打印密钥信息
     */
    public void diagnoseKey() {
        try {
            log.info("=== 密钥诊断开始 ===");
            log.info("项目ID: {}", projectId);
            log.info("凭据ID: {}", credentialId);

            // 诊断私钥
            log.info("私钥原始字符串长度: {} 字符", privateKeyBase64.length());
            log.info("私钥前100字符: {}",
                    privateKeyBase64.substring(0, Math.min(100, privateKeyBase64.length())));

            // 清理并解码私钥
            String cleanPrivate = privateKeyBase64.trim()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] privateBytes = Base64.getDecoder().decode(cleanPrivate);
            log.info("私钥解码后长度: {} 字节", privateBytes.length);

            // 诊断公钥
            log.info("公钥原始字符串长度: {} 字符", publicKeyBase64.length());
            log.info("公钥前100字符: {}",
                    publicKeyBase64.substring(0, Math.min(100, publicKeyBase64.length())));

            // 清理并解码公钥
            String cleanPublic = publicKeyBase64.trim()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] publicBytes = Base64.getDecoder().decode(cleanPublic);
            log.info("公钥解码后长度: {} 字节", publicBytes.length);

            // 尝试提取原始密钥
            try {
                byte[] rawPrivate = extractRawEd25519PrivateKey();
                log.info("✅ 原始私钥提取成功: {} 字节", rawPrivate.length);

                byte[] rawPublic = extractRawEd25519PublicKey();
                log.info("✅ 原始公钥提取成功: {} 字节", rawPublic.length);

                // 转换为Base64URL格式
                String privateBase64Url = Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(rawPrivate);
                String publicBase64Url = Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(rawPublic);

                log.info("私钥Base64URL (前30字符): {}...",
                        privateBase64Url.substring(0, Math.min(30, privateBase64Url.length())));
                log.info("公钥Base64URL (前30字符): {}...",
                        publicBase64Url.substring(0, Math.min(30, publicBase64Url.length())));

            } catch (Exception e) {
                log.error("❌ 密钥提取失败", e);
            }

            log.info("=== 密钥诊断结束 ===");
        } catch (Exception e) {
            log.error("密钥诊断失败", e);
        }
    }
}