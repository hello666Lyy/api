package com.org.api_common.util;

import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.constant.SignConstants;
import com.org.api_common.exception.BusinessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具（HMAC-SHA256）
 * @author zhangzhenhui
 */
public class SignUtil {

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
            String signStr = sb.substring(0, sb.length() - 1);

            // ========== 【新增调试日志-开始】仅打印拼接串，定位签名问题，不影响业务逻辑 ==========
            System.out.println("【签名调试】服务端拼接的加密字符串：" + signStr);
            // ========== 【新增调试日志-结束】 ==========

            // 3. HMAC-SHA256加密
            Mac mac = Mac.getInstance(SignConstants.SIGN_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(sk.getBytes(StandardCharsets.UTF_8), SignConstants.SIGN_ALGORITHM);
            mac.init(keySpec);
            byte[] signBytes = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));

            // 4. 转16进制字符串
            return bytesToHex(signBytes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR, "签名生成失败：" + e.getMessage());
        }
    }

    /**
     * 验证签名
     * @param params 请求参数（含timestamp、nonce）
     * @param sk 秘钥SK
     * @param sign 待验证签名
     * @return 是否有效
     */
    public static boolean verifySign(Map<String, Object> params, String sk, String sign) {
        String generateSign = generateSign(params, sk);
        System.out.println("后端生成的签名：" + generateSign);
        return generateSign.equalsIgnoreCase(sign);
    }

    /**
     * 验证时间戳是否过期
     * @param timestamp 请求时间戳（秒）
     * @return 是否有效
     */
    public static boolean verifyTimestamp(Long timestamp) {
        if (timestamp == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis() / 1000;
        return Math.abs(currentTime - timestamp) <= SignConstants.TIMESTAMP_EXPIRE_SECOND;
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