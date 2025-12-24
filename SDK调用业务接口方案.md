# SDK调用业务接口完整方案

## 一、整体架构

```
用户（开发者）
  ↓
使用SDK（api_sdk.jar）
  ↓
配置AK/SK
  ↓
调用业务接口（如：天气接口）
  ↓
HTTP请求（带签名）
  ↓
后端接收请求
  ↓
1. 校验AK/SK签名（已有）
  ↓
2. 校验接口权限（新增）⭐
  ↓
3. 执行业务逻辑
  ↓
返回结果
```

---

## 二、需要实现的功能

### 1. 后端部分

#### ① 创建业务接口Controller（示例：天气接口）
- **路径：** `/api/business/weather/query`
- **功能：** 提供业务接口（如天气查询）
- **权限：** 需要用户开通该接口的权限才能调用

#### ② 创建接口权限校验拦截器
- **功能：** 在业务接口调用前，校验用户是否有该接口的权限
- **校验逻辑：**
  1. 从请求中获取 `accessKey`
  2. 根据 `accessKey` 获取用户ID
  3. 根据请求路径和方法，查询对应的接口ID
  4. 查询 `api_permission` 表，检查用户是否有该接口的权限
  5. 检查权限是否过期
  6. 如果没有权限，返回403错误

#### ③ 配置拦截器
- 只拦截业务接口路径（如：`/api/business/**`）
- 不拦截管理接口和认证接口

### 2. SDK部分

#### ① 创建业务接口Service
- **类名：** `BusinessApiService`
- **功能：** 封装业务接口调用方法
- **示例方法：**
  - `queryWeather(String city)` - 查询天气
  - `callBusinessApi(String apiPath, String method, Map<String, Object> params)` - 通用业务接口调用

#### ② 更新ApiClient
- 添加获取业务接口服务的方法

---

## 三、详细实现方案

### 方案A：使用拦截器（推荐）⭐

**优点：**
- 统一处理，代码集中
- 不影响业务代码
- 易于维护

**实现步骤：**

1. **创建业务接口权限拦截器**
   - 文件：`api_web/src/main/java/com/org/api_web/interceptor/ApiPermissionInterceptor.java`
   - 功能：校验用户是否有接口调用权限

2. **创建业务接口Controller（示例）**
   - 文件：`api_web/src/main/java/com/org/api_web/controller/business/WeatherController.java`
   - 路径：`/api/business/weather/query`

3. **配置拦截器**
   - 在 `WebMvcConfig` 中注册拦截器
   - 只拦截 `/api/business/**` 路径

4. **SDK添加业务接口调用方法**
   - 在 `api_sdk` 中添加 `BusinessApiService`

---

### 方案B：使用AOP切面

**优点：**
- 更灵活，可以精确控制
- 可以添加注解标记需要权限校验的接口

**缺点：**
- 需要在每个业务接口上添加注解
- 代码分散

---

## 四、推荐方案：拦截器方案

### 1. 业务接口路径规范

**建议路径格式：**
- `/api/business/{业务模块}/{接口名称}`
- 例如：
  - `/api/business/weather/query` - 天气查询
  - `/api/business/sms/send` - 短信发送
  - `/api/business/payment/create` - 支付创建

### 2. 权限校验流程

```
1. 用户通过SDK调用业务接口
   ↓
2. 请求到达后端，携带accessKey和签名
   ↓
3. 先校验AK/SK签名（已有逻辑）
   ↓
4. 权限拦截器拦截请求
   ↓
5. 根据请求路径和方法，查询api_info表获取接口ID
   ↓
6. 根据accessKey获取用户ID
   ↓
7. 查询api_permission表，检查权限
   ↓
8. 检查权限是否过期
   ↓
9. 有权限 → 放行，执行业务逻辑
   无权限 → 返回403错误
```

### 3. 错误处理

**无权限时返回：**
```json
{
  "code": 403,
  "msg": "接口权限不足，请联系管理员开通接口权限",
  "data": null
}
```

**权限过期时返回：**
```json
{
  "code": 403,
  "msg": "接口权限已过期，请联系管理员续期",
  "data": null
}
```

---

## 五、SDK使用示例

### 示例1：调用天气接口

```java
// 1. 配置AK/SK
ApiConfig config = new ApiConfig();
config.setAccessKey("用户的AK");
config.setSecretKey("用户的SK");
config.setBaseUrl("http://api.example.com");

// 2. 创建客户端
ApiClient client = new ApiClient(config);

// 3. 调用业务接口
BusinessApiService businessService = client.getBusinessApiService();

// 方式1：使用封装好的方法
WeatherResult weather = businessService.queryWeather("北京");

// 方式2：通用调用方法
Map<String, Object> params = new HashMap<>();
params.put("city", "北京");
WeatherResult weather = businessService.callBusinessApi(
    "/api/business/weather/query", 
    "GET", 
    params, 
    WeatherResult.class
);
```

### 示例2：调用其他业务接口

```java
// 调用短信接口
SmsResult result = businessService.sendSms("13800138000", "验证码：1234");

// 调用支付接口
Map<String, Object> paymentParams = new HashMap<>();
paymentParams.put("amount", 100.00);
paymentParams.put("orderId", "ORDER123");
PaymentResult payment = businessService.createPayment(paymentParams);
```

---

## 六、需要新增的文件

### 后端文件

1. **拦截器**
   - `api_web/src/main/java/com/org/api_web/interceptor/ApiPermissionInterceptor.java`

2. **业务接口Controller（示例）**
   - `api_web/src/main/java/com/org/api_web/controller/business/WeatherController.java`
   - 其他业务接口按需添加

3. **配置更新**
   - `api_web/src/main/java/com/org/api_web/config/WebMvcConfig.java` - 注册拦截器

### SDK文件

1. **业务接口Service**
   - `api_sdk/src/main/java/com/org/api_sdk/service/BusinessApiService.java`

2. **业务接口VO（示例）**
   - `api_sdk/src/main/java/com/org/api_sdk/model/WeatherResult.java`
   - 其他业务接口的VO按需添加

3. **ApiClient更新**
   - 添加 `getBusinessApiService()` 方法

---

## 七、实施步骤

### 阶段1：后端权限校验（必须）

1. ✅ 创建 `ApiPermissionInterceptor` - 接口权限拦截器
2. ✅ 创建示例业务接口 `WeatherController` - 天气接口
3. ✅ 配置拦截器，拦截 `/api/business/**` 路径
4. ✅ 测试权限校验功能

### 阶段2：SDK封装（必须）

1. ✅ 创建 `BusinessApiService` - 业务接口服务
2. ✅ 更新 `ApiClient` - 添加业务接口服务
3. ✅ 创建示例VO类（如 `WeatherResult`）
4. ✅ 测试SDK调用功能

### 阶段3：完善功能（可选）

1. ⚠️ 添加更多业务接口示例
2. ⚠️ 优化错误提示
3. ⚠️ 添加调用日志记录

---

## 八、技术细节

### 1. 权限校验逻辑

```java
// 伪代码
1. 从请求参数中获取 accessKey
2. 根据 accessKey 查询用户ID
3. 根据请求路径（如 /api/business/weather/query）和方法（GET）查询 api_info 表
4. 获取接口ID
5. 查询 api_permission 表：
   - WHERE user_id = 用户ID
   - AND api_id = 接口ID
   - AND status = 1（已授权）
   - AND (expire_time IS NULL OR expire_time > NOW())（未过期）
6. 如果查询到记录 → 有权限，放行
   如果查询不到 → 无权限，返回403
```

### 2. 性能优化

- **缓存权限信息：** 使用Redis缓存用户的接口权限列表，减少数据库查询
- **批量查询：** 如果用户调用多个接口，可以批量查询权限

### 3. 安全考虑

- **接口路径校验：** 确保只校验业务接口，不校验管理接口
- **权限缓存失效：** 当权限变更时，清除相关缓存

---

## 九、需要确认的事项

### 1. 业务接口路径规范
- [ ] 业务接口路径是否统一使用 `/api/business/**`？
- [ ] 还是使用其他路径格式？

### 2. 权限校验时机
- [ ] 是否在AK/SK签名校验之后进行权限校验？
- [ ] 还是先校验权限，再校验签名？

### 3. 业务接口类型
- [ ] 需要哪些业务接口？（天气、短信、支付等）
- [ ] 每个接口的参数和返回值格式是什么？

### 4. 错误处理
- [ ] 无权限时的错误码和提示信息
- [ ] 权限过期时的错误码和提示信息

---

## 十、预计工作量

| 模块 | 工作量 | 优先级 |
|------|--------|--------|
| 接口权限拦截器 | 1天 | 高 |
| 业务接口Controller（示例） | 0.5天 | 高 |
| SDK业务接口封装 | 0.5天 | 高 |
| 测试和优化 | 0.5天 | 中 |
| **总计** | **2.5天** | |

---

## 十一、下一步

请确认：
1. ✅ 方案是否符合需求？
2. ✅ 业务接口路径规范是否同意？
3. ✅ 需要哪些业务接口？（可以先实现一个示例）
4. ✅ 权限校验逻辑是否同意？

确认后即可开始实施！

