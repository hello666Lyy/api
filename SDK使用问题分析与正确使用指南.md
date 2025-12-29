# SDK使用问题分析与正确使用指南

## 📋 目录

1. [问题分析](#问题分析)
2. [正确使用方法](#正确使用方法)
3. [完整示例代码](#完整示例代码)
4. [常见错误](#常见错误)
5. [最佳实践](#最佳实践)
6. [故障排查](#故障排查)

---

## 🔍 问题分析

### 问题现象

客户测试代码中所有SDK功能测试均失败，返回404错误：
```
{"code":404,"msg":"资源不存在：AK不存在或用户已禁用","data":null}
```

实际HTTP请求使用的AccessKey为 `test_ak_123`，而不是配置文件中设置的 `UYdonKph9RMptCKJ`。

### 问题根源

**问题出在客户测试代码的配置传递上，SDK代码本身没有问题。**

#### ✅ SDK代码验证

1. **ApiConfig配置类** - 没有默认值，必须显式设置
   ```java
   @Data
   @Builder
   public class ApiConfig {
       private String baseUrl;      // 无默认值
       private String accessKey;    // 无默认值 ✅
       private String secretKey;    // 无默认值 ✅
   }
   ```

2. **ApiClient使用配置** - 正确使用传入的配置
   ```java
   // SDK内部正确使用配置
   signParams.put("accessKey", config.getAccessKey());
   String sign = SignUtil.generateSign(signParams, config.getSecretKey());
   allParams.put("accessKey", config.getAccessKey());
   ```

3. **无硬编码值** - 代码中没有任何硬编码的AccessKey或默认值

#### ❌ 客户代码问题

客户在创建 `ApiConfig` 时没有正确传递配置文件中的值，可能的原因：

1. **配置类实现错误** - 没有使用 `@Value` 读取配置文件
2. **创建了多个ApiClient实例** - 其中一个使用了错误的配置
3. **配置注入失败** - Spring没有正确注入配置值

---

## ✅ 正确使用方法

### 方法1：使用Spring Boot配置（推荐）

#### 步骤1：添加配置文件

在 `src/main/resources/application.yml` 中添加配置：

```yaml
# API SDK配置
api:
  baseUrl: http://localhost:8081  # 后端服务地址
  accessKey: UYdonKph9RMptCKJ      # 你的AccessKey
  secretKey: 4pSuhK9GqyWwb2USBQn0pO25uVDiYFjL  # 你的SecretKey
```

#### 步骤2：创建配置类

创建 `ApiSdkConfig.java`：

```java
package com.org.sdk_test_test.config;

import com.org.api_sdk.ApiClient;
import com.org.api_sdk.config.ApiConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        // ✅ 正确：使用注入的配置值创建ApiConfig
        ApiConfig config = ApiConfig.builder()
            .baseUrl(baseUrl)
            .accessKey(accessKey)    // ✅ 从配置文件读取
            .secretKey(secretKey)    // ✅ 从配置文件读取
            .connectTimeout(5000)    // 可选：连接超时（毫秒）
            .readTimeout(10000)      // 可选：读取超时（毫秒）
            .enableLog(true)         // 可选：是否打印请求日志
            .build();
            
        return new ApiClient(config);
    }
}
```

#### 步骤3：使用SDK

在需要使用SDK的地方注入 `ApiClient`：

```java
package com.org.sdk_test_test;

import com.org.api_sdk.ApiClient;
import com.org.api_sdk.service.BusinessApiService;
import com.org.api_sdk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SdkTestRunner implements CommandLineRunner {
    
    @Autowired
    private ApiClient apiClient;  // ✅ 注入配置好的ApiClient
    
    @Override
    public void run(String... args) {
        // 使用SDK
        UserService userService = apiClient.getUserService();
        BusinessApiService businessService = apiClient.getBusinessApiService();
        
        // 测试：获取用户信息
        try {
            var userInfo = userService.getUserInfo();
            System.out.println("✅ 成功 - 获取用户信息: " + userInfo);
        } catch (Exception e) {
            System.err.println("❌ 失败: " + e.getMessage());
        }
        
        // 测试：查询天气
        try {
            Map<String, Object> weather = businessService.queryWeather("北京");
            System.out.println("✅ 成功 - 查询天气: " + weather);
        } catch (Exception e) {
            System.err.println("❌ 失败: " + e.getMessage());
        }
    }
}
```

### 方法2：直接创建（不使用Spring）

如果不使用Spring Boot，可以直接创建：

```java
import com.org.api_sdk.ApiClient;
import com.org.api_sdk.config.ApiConfig;

public class SdkExample {
    public static void main(String[] args) {
        // ✅ 正确：直接创建ApiConfig并设置所有必需参数
        ApiConfig config = ApiConfig.builder()
            .baseUrl("http://localhost:8081")
            .accessKey("UYdonKph9RMptCKJ")                    // ✅ 必须设置
            .secretKey("4pSuhK9GqyWwb2USBQn0pO25uVDiYFjL")    // ✅ 必须设置
            .enableLog(true)  // 可选：启用日志
            .build();
        
        ApiClient client = new ApiClient(config);
        
        // 使用SDK
        var userInfo = client.getUserService().getUserInfo();
        System.out.println("用户信息: " + userInfo);
    }
}
```

---

## 📝 完整示例代码

### 完整的Spring Boot配置示例

#### 1. application.yml

```yaml
server:
  port: 8082

# API SDK配置
api:
  baseUrl: http://localhost:8081
  accessKey: UYdonKph9RMptCKJ
  secretKey: 4pSuhK9GqyWwb2USBQn0pO25uVDiYFjL
```

#### 2. ApiSdkConfig.java

```java
package com.org.sdk_test_test.config;

import com.org.api_sdk.ApiClient;
import com.org.api_sdk.config.ApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiSdkConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ApiSdkConfig.class);
    
    @Value("${api.baseUrl}")
    private String baseUrl;
    
    @Value("${api.accessKey}")
    private String accessKey;
    
    @Value("${api.secretKey}")
    private String secretKey;
    
    @Bean
    public ApiClient apiClient() {
        log.info("========== SDK配置信息 ==========");
        log.info("baseUrl: {}", baseUrl);
        log.info("accessKey: {}", accessKey);
        log.info("secretKey: {}...", secretKey != null ? secretKey.substring(0, 10) : "null");
        log.info("==================================");
        
        ApiConfig config = ApiConfig.builder()
            .baseUrl(baseUrl)
            .accessKey(accessKey)
            .secretKey(secretKey)
            .connectTimeout(5000)
            .readTimeout(10000)
            .enableLog(true)  // 启用日志便于调试
            .build();
        
        ApiClient client = new ApiClient(config);
        
        // 验证配置是否正确传递
        log.info("验证 - ApiClient中的accessKey: {}", client.getConfig().getAccessKey());
        
        return client;
    }
}
```

#### 3. SdkTestRunner.java

```java
package com.org.sdk_test_test;

import com.org.api_sdk.ApiClient;
import com.org.api_sdk.exception.ApiException;
import com.org.api_sdk.service.BusinessApiService;
import com.org.api_sdk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SdkTestRunner implements CommandLineRunner {
    
    @Autowired
    private ApiClient apiClient;
    
    private void sleepForNextRequest() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void run(String... args) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("========== SDK功能测试开始 ==========");
        System.out.println("=".repeat(50) + "\n");
        
        // 验证配置
        System.out.println("【配置验证】");
        System.out.println("  baseUrl: " + apiClient.getConfig().getBaseUrl());
        System.out.println("  accessKey: " + apiClient.getConfig().getAccessKey());
        System.out.println("  secretKey: " + 
            (apiClient.getConfig().getSecretKey() != null ? 
             apiClient.getConfig().getSecretKey().substring(0, 10) + "..." : "null"));
        System.out.println();
        
        int successCount = 0;
        int failCount = 0;
        
        // 测试1: 获取用户信息
        System.out.println("[测试1] 获取用户信息");
        try {
            UserService userService = apiClient.getUserService();
            var userInfo = userService.getUserInfo();
            System.out.println("✅ 成功 - 获取用户信息: " + userInfo);
            successCount++;
        } catch (ApiException e) {
            System.err.println("❌ 失败 - 获取用户信息: " + e.getMsg() + " (code=" + e.getCode() + ")");
            failCount++;
        } catch (Exception e) {
            System.err.println("❌ 失败 - 获取用户信息: " + e.getMessage());
            failCount++;
        }
        System.out.println();
        
        // 测试2: 查询天气
        sleepForNextRequest();
        System.out.println("[测试2] 查询天气接口");
        try {
            BusinessApiService businessService = apiClient.getBusinessApiService();
            Map<String, Object> weather = businessService.queryWeather("北京");
            System.out.println("✅ 成功 - 查询天气: " + weather);
            successCount++;
        } catch (ApiException e) {
            System.err.println("❌ 失败 - 查询天气: " + e.getMsg() + " (code=" + e.getCode() + ")");
            failCount++;
        } catch (Exception e) {
            System.err.println("❌ 失败 - 查询天气: " + e.getMessage());
            failCount++;
        }
        System.out.println();
        
        // 测试3: 获取当前时间
        sleepForNextRequest();
        System.out.println("[测试3] 获取当前时间");
        try {
            BusinessApiService businessService = apiClient.getBusinessApiService();
            Map<String, Object> time = businessService.getCurrentTime("Asia/Shanghai");
            System.out.println("✅ 成功 - 获取当前时间: " + time);
            successCount++;
        } catch (ApiException e) {
            System.err.println("❌ 失败 - 获取当前时间: " + e.getMsg() + " (code=" + e.getCode() + ")");
            failCount++;
        } catch (Exception e) {
            System.err.println("❌ 失败 - 获取当前时间: " + e.getMessage());
            failCount++;
        }
        System.out.println();
        
        // 测试4: 生成随机数
        sleepForNextRequest();
        System.out.println("[测试4] 生成随机数");
        try {
            BusinessApiService businessService = apiClient.getBusinessApiService();
            Map<String, Object> random = businessService.generateRandom(1, 100, 5);
            System.out.println("✅ 成功 - 生成随机数: " + random);
            successCount++;
        } catch (ApiException e) {
            System.err.println("❌ 失败 - 生成随机数: " + e.getMsg() + " (code=" + e.getCode() + ")");
            failCount++;
        } catch (Exception e) {
            System.err.println("❌ 失败 - 生成随机数: " + e.getMessage());
            failCount++;
        }
        System.out.println();
        
        // 测试结果汇总
        System.out.println("=".repeat(50));
        System.out.println("========== SDK功能测试完成 ==========");
        System.out.println("=".repeat(50));
        System.out.println("成功: " + successCount + " 个");
        System.out.println("失败: " + failCount + " 个");
        System.out.println("总计: " + (successCount + failCount) + " 个");
        System.out.println("=".repeat(50) + "\n");
    }
}
```

---

## ❌ 常见错误

### 错误1：硬编码配置值

```java
// ❌ 错误：硬编码了配置值
@Bean
public ApiClient apiClient() {
    ApiConfig config = ApiConfig.builder()
        .baseUrl("http://localhost:8081")
        .accessKey("test_ak_123")  // ❌ 硬编码，应该从配置文件读取
        .secretKey("wrong_sk")     // ❌ 硬编码，应该从配置文件读取
        .build();
    return new ApiClient(config);
}

// ✅ 正确：从配置文件读取
@Value("${api.accessKey}")
private String accessKey;

@Bean
public ApiClient apiClient() {
    ApiConfig config = ApiConfig.builder()
        .baseUrl(baseUrl)
        .accessKey(accessKey)  // ✅ 使用注入的值
        .secretKey(secretKey) // ✅ 使用注入的值
        .build();
    return new ApiClient(config);
}
```

### 错误2：忘记使用@Value注解

```java
// ❌ 错误：没有使用@Value，字段值为null
@Configuration
public class ApiSdkConfig {
    private String accessKey;  // ❌ 没有@Value，值为null
    
    @Bean
    public ApiClient apiClient() {
        ApiConfig config = ApiConfig.builder()
            .accessKey(accessKey)  // ❌ accessKey为null
            .build();
        return new ApiClient(config);
    }
}

// ✅ 正确：使用@Value注解
@Configuration
public class ApiSdkConfig {
    @Value("${api.accessKey}")
    private String accessKey;  // ✅ 正确注入配置值
    
    @Bean
    public ApiClient apiClient() {
        ApiConfig config = ApiConfig.builder()
            .accessKey(accessKey)  // ✅ 使用正确的值
            .build();
        return new ApiClient(config);
    }
}
```

### 错误3：创建了多个ApiClient实例

```java
// ❌ 错误：创建了多个ApiClient，使用了不同的配置
@Configuration
public class ApiSdkConfig {
    @Bean
    public ApiClient apiClient1() {
        // 正确的配置
        return new ApiClient(ApiConfig.builder()
            .accessKey("UYdonKph9RMptCKJ")
            .build());
    }
    
    @Bean
    public ApiClient apiClient2() {
        // ❌ 错误的配置，但也被注入了
        return new ApiClient(ApiConfig.builder()
            .accessKey("test_ak_123")  // 错误的配置
            .build());
    }
}

// ✅ 正确：只创建一个ApiClient Bean
@Configuration
public class ApiSdkConfig {
    @Bean
    public ApiClient apiClient() {
        // 只创建一个，使用正确的配置
        return new ApiClient(ApiConfig.builder()
            .accessKey(accessKey)
            .secretKey(secretKey)
            .build());
    }
}
```

### 错误4：配置文件路径或格式错误

```yaml
# ❌ 错误：配置属性名称不匹配
api:
  access_key: UYdonKph9RMptCKJ  # 使用了下划线
  secret_key: xxx

# Java代码中使用的是驼峰命名
@Value("${api.accessKey}")  // ❌ 找不到配置，值为null

# ✅ 正确：使用驼峰命名
api:
  accessKey: UYdonKph9RMptCKJ
  secretKey: xxx
```

---

## 💡 最佳实践

### 1. 配置验证

在创建 `ApiClient` 后，添加配置验证：

```java
@Bean
public ApiClient apiClient() {
    ApiConfig config = ApiConfig.builder()
        .baseUrl(baseUrl)
        .accessKey(accessKey)
        .secretKey(secretKey)
        .build();
    
    ApiClient client = new ApiClient(config);
    
    // ✅ 验证配置是否正确
    if (client.getConfig().getAccessKey() == null || 
        client.getConfig().getAccessKey().isEmpty()) {
        throw new IllegalStateException("AccessKey不能为空！");
    }
    
    if (client.getConfig().getSecretKey() == null || 
        client.getConfig().getSecretKey().isEmpty()) {
        throw new IllegalStateException("SecretKey不能为空！");
    }
    
    return client;
}
```

### 2. 使用环境变量（生产环境）

生产环境建议使用环境变量：

```yaml
# application.yml
api:
  baseUrl: ${API_BASE_URL:http://localhost:8081}
  accessKey: ${API_ACCESS_KEY}
  secretKey: ${API_SECRET_KEY}
```

### 3. 启用日志便于调试

```java
ApiConfig config = ApiConfig.builder()
    .baseUrl(baseUrl)
    .accessKey(accessKey)
    .secretKey(secretKey)
    .enableLog(true)  // ✅ 启用日志，便于调试
    .build();
```

### 4. 异常处理

```java
try {
    var userInfo = userService.getUserInfo();
    System.out.println("成功: " + userInfo);
} catch (ApiException e) {
    // ✅ 处理API异常
    if (e.getCode() == 404) {
        System.err.println("AK不存在或已禁用: " + e.getMsg());
    } else if (e.getCode() == 401) {
        System.err.println("签名验证失败: " + e.getMsg());
    } else {
        System.err.println("API错误: " + e.getMsg());
    }
} catch (Exception e) {
    // ✅ 处理其他异常
    System.err.println("未知错误: " + e.getMessage());
    e.printStackTrace();
}
```

---

## 🔧 故障排查

### 步骤1：验证配置文件

```bash
# 检查配置文件是否存在
cat src/main/resources/application.yml | grep -A 3 "api:"
```

### 步骤2：添加调试日志

在 `ApiSdkConfig` 中添加日志：

```java
@Bean
public ApiClient apiClient() {
    System.out.println("【调试】读取的配置值:");
    System.out.println("  baseUrl: " + baseUrl);
    System.out.println("  accessKey: " + accessKey);
    System.out.println("  secretKey: " + (secretKey != null ? secretKey.substring(0, 10) + "..." : "null"));
    
    ApiConfig config = ApiConfig.builder()
        .baseUrl(baseUrl)
        .accessKey(accessKey)
        .secretKey(secretKey)
        .build();
    
    ApiClient client = new ApiClient(config);
    
    System.out.println("【调试】ApiClient中的配置值:");
    System.out.println("  accessKey: " + client.getConfig().getAccessKey());
    
    return client;
}
```

### 步骤3：检查是否有多个ApiClient Bean

```bash
# 搜索所有创建ApiClient的地方
grep -r "new ApiClient" src/
grep -r "@Bean.*ApiClient" src/
```

### 步骤4：验证配置注入

在测试代码中验证：

```java
@Autowired
private ApiClient apiClient;

@Override
public void run(String... args) {
    // 验证配置
    System.out.println("实际使用的accessKey: " + apiClient.getConfig().getAccessKey());
    System.out.println("实际使用的baseUrl: " + apiClient.getConfig().getBaseUrl());
    
    // 如果accessKey不是期望的值，说明配置传递有问题
    if (!"UYdonKph9RMptCKJ".equals(apiClient.getConfig().getAccessKey())) {
        System.err.println("❌ 配置错误！期望: UYdonKph9RMptCKJ, 实际: " + 
            apiClient.getConfig().getAccessKey());
    }
}
```

### 步骤5：检查Spring配置扫描

确保配置类被Spring扫描到：

```java
@SpringBootApplication
// ✅ 确保包路径正确
@ComponentScan(basePackages = {"com.org.sdk_test_test"})
public class SdkTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SdkTestApplication.class, args);
    }
}
```

---

## 📌 总结

### 关键要点

1. ✅ **SDK代码没有问题** - 所有配置必须显式传入，没有默认值
2. ✅ **必须使用@Value注解** - 从配置文件读取配置值
3. ✅ **只创建一个ApiClient Bean** - 避免多个实例使用不同配置
4. ✅ **添加配置验证** - 确保配置正确传递
5. ✅ **启用日志** - 便于调试和排查问题

### 快速检查清单

- [ ] 配置文件 `application.yml` 存在且格式正确
- [ ] 配置类使用了 `@Configuration` 注解
- [ ] 配置字段使用了 `@Value("${api.accessKey}")` 注解
- [ ] `@Bean` 方法中正确传递了配置值
- [ ] 只创建了一个 `ApiClient` Bean
- [ ] 添加了配置验证日志
- [ ] 测试代码中验证了实际使用的配置值

---

## 📅 文档版本

- **创建时间**: 2025-12-22
- **SDK版本**: 1.0-SNAPSHOT
- **适用场景**: Spring Boot项目集成SDK

---

**如有问题，请检查配置传递链路，确保配置文件中的值正确传递到 `ApiConfig` 中。**






























































