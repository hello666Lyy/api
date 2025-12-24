package com.org.api_web;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * IP限流测试类
 * 快速发送多个请求，验证限流功能是否生效
 */
public class RateLimitTest {

    // 测试接口地址
    private static final String TEST_URL = "http://localhost:8081/api/test/metrics/success";

    // 限流阈值（应该和 ApiMetricsAspect 里的 IP_RATE_LIMIT_PER_MINUTE 一致）
    private static final int RATE_LIMIT = 60;

    // 测试请求总数（建议设置为限流阈值的1.5倍，比如90次）
    private static final int TOTAL_REQUESTS = 90;

    @Test
    public void testIpRateLimit() throws Exception {
        System.out.println("===== 开始IP限流测试 =====");
        System.out.println("测试URL: " + TEST_URL);
        System.out.println("限流阈值: " + RATE_LIMIT + " 次/分钟");
        System.out.println("发送请求数: " + TOTAL_REQUESTS);
        System.out.println("请求间隔: 10ms");
        System.out.println("-----------------------------------");

        int successCount = 0;      // 成功请求数（200）
        int limitCount = 0;        // 被限流请求数（429）
        int errorCount = 0;        // 其他错误

        long startTime = System.currentTimeMillis();

        // 快速发送请求
        for (int i = 1; i <= TOTAL_REQUESTS; i++) {
            try {
                URL url = new URL(TEST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();

                // 读取响应体
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();

                String responseBody = response.toString();

                // 判断响应类型
                if (responseBody.contains("\"code\":429") ||
                        responseBody.contains("访问过于频繁")) {
                    limitCount++;
                    if (limitCount == 1) {
                        System.out.println(">>> 第 " + i + " 次请求开始被限流");
                    }
                } else if (responseCode == 200 &&
                        (responseBody.contains("\"code\":200") ||
                                responseBody.contains("操作成功"))) {
                    successCount++;
                } else {
                    errorCount++;
                    System.out.println("第 " + i + " 次请求异常: code=" + responseCode);
                }

                // 每10次输出一次进度
                if (i % 10 == 0) {
                    System.out.println("进度: " + i + "/" + TOTAL_REQUESTS +
                            " | 成功: " + successCount +
                            " | 限流: " + limitCount);
                }

                // 短暂延迟，避免过快
                Thread.sleep(10);

            } catch (Exception e) {
                errorCount++;
                System.err.println("第 " + i + " 次请求失败: " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 输出测试结果
        System.out.println("-----------------------------------");
        System.out.println("===== 测试结果统计 =====");
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功请求: " + successCount + " 次");
        System.out.println("限流请求: " + limitCount + " 次");
        System.out.println("异常请求: " + errorCount + " 次");
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均耗时: " + (duration / TOTAL_REQUESTS) + " ms/次");
        System.out.println("-----------------------------------");

        // 验证限流是否生效
        if (successCount <= RATE_LIMIT && limitCount > 0) {
            System.out.println("✅ 限流功能正常：前 " + successCount + " 次成功，后续 " + limitCount + " 次被限流");
        } else if (successCount > RATE_LIMIT) {
            System.out.println("⚠️  警告：成功请求数超过限流阈值，可能限流未生效");
        } else {
            System.out.println("❌ 异常：未检测到限流，请检查限流配置");
        }
    }

    /**
     * 快速测试：只发送70次请求，验证前60次成功，后10次被限流
     */
    @Test
    public void quickTest() throws Exception {
        System.out.println("===== 快速限流测试（70次请求）=====");

        int successCount = 0;
        int limitCount = 0;

        for (int i = 1; i <= 70; i++) {
            try {
                URL url = new URL(TEST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();
                conn.disconnect();

                if (response != null && (response.contains("\"code\":429") ||
                        response.contains("访问过于频繁"))) {
                    limitCount++;
                    if (limitCount == 1) {
                        System.out.println("第 " + i + " 次开始被限流");
                    }
                } else {
                    successCount++;
                }

                Thread.sleep(10);
            } catch (Exception e) {
                System.err.println("请求失败: " + e.getMessage());
            }
        }

        System.out.println("结果: 成功 " + successCount + " 次，限流 " + limitCount + " 次");

        if (successCount == 60 && limitCount == 10) {
            System.out.println("✅ 测试通过：限流功能正常");
        } else {
            System.out.println("⚠️  结果异常，请检查限流配置");
        }
    }
}