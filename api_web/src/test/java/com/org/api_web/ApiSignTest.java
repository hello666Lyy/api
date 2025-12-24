package com.org.api_web;
import com.org.api_common.util.SignUtil;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 单元测试类：直接复用项目原生SignUtil，生成100%正确的接口参数
 * 无需重复写加密算法，保证和后端校验逻辑完全一致
 */
public class ApiSignTest {

    // 配置你测试用的AK和SK（从数据库查test_ak_123对应的真实SK）
    private static final String TEST_AK = "7jxtUmuITH8lC68J";
    private static final String TEST_SK = "NniZjv7CxqEmBYa0cIxijsPW2dgLHpDZ"; // 替换成数据库里的真实SK

//    private static final String TEST_AK = "test_ak_789";
//    private static final String TEST_SK = "U8DbBodyLpXW7Rrlm5pvPWZC8z5wNXUxOxLj5FCbhh8"; // 替换成数据库里的真实SK
    @Test
    public void generateCorrectApiParams() {
        // 1. 生成合规参数（秒级timestamp + 全新nonce，符合项目校验规则）
        long timestamp = System.currentTimeMillis() / 1000; // 秒级时间戳（匹配SignUtil.verifyTimestamp）
        String nonce = UUID.randomUUID().toString().replace("-", ""); // 全新nonce（永不重复）

        // 2. 直接调用项目原生的SignUtil生成签名（0重复代码，100%和后端一致）
        Map<String, Object> params = new HashMap<>();
        params.put("accessKey", TEST_AK);
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        String correctSign = SignUtil.generateSign(params, TEST_SK);

        // 3. 输出全套正确参数（直接复制到Postman即可）
        System.out.println("===== 可直接复制到Postman的参数 =====");
        System.out.println("accessKey = " + TEST_AK);
        System.out.println("timestamp = " + timestamp);
        System.out.println("nonce     = " + nonce);
        System.out.println("sign      = " + correctSign);
    }
}