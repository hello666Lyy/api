package com.org.api_sdk;

import com.org.api_common.vo.UserInfoVO;
import com.org.api_sdk.config.ApiConfig;
import com.org.api_sdk.exception.ApiException;
import com.org.api_sdk.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SDK测试类
 * 放在 api_sdk 模块中，测试SDK本身的功能
 *
 * @author zhangzhenhui
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("API SDK 功能测试")
public class ApiClientTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String ACCESS_KEY = "7jxtUmuITH8lC68J";
    private static final String SECRET_KEY = "NniZjv7CxqEmBYa0cIxijsPW2dgLHpDZ";

    private ApiClient client;
    private UserService userService;

    @BeforeAll
    void initClient() {
        ApiConfig config = ApiConfig.builder()
                .baseUrl(BASE_URL)
                .accessKey(ACCESS_KEY)
                .secretKey(SECRET_KEY)
                .enableLog(true)
                .build();

        client = new ApiClient(config);
        userService = client.getUserService();
    }

    @Test
    @DisplayName("测试：获取用户信息")
    void testGetUserInfo() {
        assertDoesNotThrow(() -> {
            UserInfoVO userInfo = userService.getUserInfo();
            assertNotNull(userInfo);
            assertEquals(ACCESS_KEY, userInfo.getAccessKey());
            System.out.println("✓ 测试通过: " + userInfo.getUsername());
        });
    }

    @Test
    @DisplayName("测试：异常处理")
    void testExceptionHandling() {
        ApiConfig wrongConfig = ApiConfig.builder()
                .baseUrl(BASE_URL)
                .accessKey("wrong_ak")
                .secretKey("wrong_sk")
                .build();

        ApiClient wrongClient = new ApiClient(wrongConfig);
        assertThrows(ApiException.class, () -> {
            wrongClient.getUserService().getUserInfo();
        });
    }
}