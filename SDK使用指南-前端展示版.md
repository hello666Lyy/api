## API SDK 使用指南

### 1. 引入 SDK

**步骤 1：拷贝 JAR**

- 将 `api_sdk-1.0-SNAPSHOT.jar` 放入你项目根目录下的 `lib/` 目录。

**步骤 2：在 `pom.xml` 中添加依赖**

```xml
<dependency>
    <groupId>com.org</groupId>
    <artifactId>api_sdk</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/api_sdk-1.0-SNAPSHOT.jar</systemPath>
</dependency>
```

---

### 3. 配置 SDK（`application.yml`）

```yaml
server:
  port: 8082

api:
  baseUrl: http://localhost:8081   # 后端 API 服务地址
  accessKey: UYdonKph9RMptCKJ      # 你的 AK
  secretKey: 4pSuhK9GqyWwb2USBQn0pO25uVDiYFjL  # 你的 SK
```

> 实际使用时，请将 `accessKey` / `secretKey` 换成你自己在平台上申请到的密钥。

---

### 4. 创建 `ApiClient` Bean

在项目中新增一个配置类，例如 `ApiSdkConfig`：

```java
@Configuration
public class ApiSdkConfig {

    @Value("${api.baseUrl}")
    private String baseUrl;

    @Value("${api.accessKey}")
    private String accessKey;

    @Value("${api.secretKey}")
    private String secretKey;

    @Bean
    public ApiClient apiClient() {
        ApiConfig config = ApiConfig.builder()
                .baseUrl(baseUrl)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .connectTimeout(5000)
                .readTimeout(10000)
                .enableLog(true)   // 建议开发环境打开日志，方便排查
                .build();
        return new ApiClient(config);
    }
}
```

---

### 5. 在业务代码中使用 SDK

在你的业务 Service 中注入 `ApiClient`，然后调用对应的服务接口：

```java
@Service
public class DemoService {

    @Autowired
    private ApiClient apiClient;

    // 获取用户信息
    public UserInfoVO getUserInfo() {
        UserService userService = apiClient.getUserService();
        return userService.getUserInfo();
    }

    // 查询天气
    public Map<String, Object> queryWeather(String city) {
        BusinessApiService businessService = apiClient.getBusinessApiService();
        return businessService.queryWeather(city);
    }

    // 获取当前时间
    public Map<String, Object> getCurrentTime(String timezone) {
        BusinessApiService businessService = apiClient.getBusinessApiService();
        return businessService.getCurrentTime(timezone);
    }

    // 生成随机数（需要开通对应接口权限）
    public Map<String, Object> generateRandom(int min, int max, int count) {
        BusinessApiService businessService = apiClient.getBusinessApiService();
        return businessService.generateRandom(min, max, count);
    }
}
```

---

### 6. 快速验证（可选）

如果你使用的是示例工程结构，可以通过以下两种方式快速验证 SDK 是否工作正常。

**方式 1：启动应用自动跑一轮测试（示例项目中的 `SdkTestRunner`）**

```bash
cd sdk_test_test
mvn spring-boot:run
```

**方式 2：运行 JUnit 单元测试**

```bash
cd sdk_test_test
mvn test
```

---

### 7. 常见问题

- **404：AK 不存在或用户已禁用**  
  - 检查 `application.yml` 中配置的 AK 是否正确；
  - 检查后台是否启用了该 AK。

- **403：接口权限不足**  
  - 登录后台，在当前 AK 下为需要调用的接口勾选权限（如时间查询、随机数生成等）。

- **端口 8082 被占用**  
  - 使用 `netstat -ano | findstr :8082` 查 PID；
  - 再用 `taskkill /F /PID <PID>` 结束进程，或者修改 `server.port` 端口。

- **500：Unable to connect to Redis / 数据库异常**  
  - 属于后端服务问题；
  - 请检查 Redis、数据库等基础服务是否正常，与 SDK 配置本身无关。

---


