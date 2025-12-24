package com.org.api_sdk.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具（HMAC-SHA256）
 * SDK独立实现，不依赖Spring
 *
 * 【重要】签名逻辑需与服务端 api_common/util/SignUtil 保持一致
 * 如果服务端签名算法变更，此处也需要同步修改
 *
 * @author zhangzhenhui
 */
public class SignUtil {

    /** 签名算法：HMAC-SHA256 */
    private static final String SIGN_ALGORITHM = "HmacSHA256";

    /**
     * 生成签名
     * @param params 请求参数（含timestamp、nonce）
     * @param sk 秘钥SK
     * @return 签名串
     */
    public static String generateSign(Map<String, Object> params, String sk) {
        try {
            // 1. 参数按ASCII升序排序
            Map<String, Object> sortedParams = new TreeMap<>(params);

            // 2. 拼接参数为key=value&key=value格式
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            // 删除最后一个&
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            String signStr = sb.toString();

            // 3. HMAC-SHA256加密
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(sk.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM);
            mac.init(keySpec);
            byte[] signBytes = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));

            // 4. 转16进制字符串（小写）
            return bytesToHex(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * byte数组转16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexStr.append('0');
            }
            hexStr.append(hex);
        }
        return hexStr.toString().toLowerCase();
    }
}