# 项目介绍

面向业务接口开放与接入的全栈项目，后端基于 Spring Boot 多模块架构，前端基于 Vue3 + Vite。项目提供用户认证、API 市场与权限申请、调用日志与统计、SDK 签名调用等完整能力。

## 架构与模块

```
api/
├─ api_common/         公共模块：实体、DTO/VO、工具、配置
├─ api_service/        业务服务：Mapper、Service、数据访问与业务实现
├─ api_web/            Web 接口：Controller、拦截器、异常处理、切面
├─ api_admin_service/  管理端服务：管理员管理 API 与权限
├─ api_sdk/            SDK 模块：签名、Nonce、客户端封装
├─ apivue/             前端应用：Vue3 + Vite + Element Plus
├─ pom.xml             父 POM
├─ build.bat           Windows 构建脚本
└─ init_business_apis.sql  业务接口初始化 SQL
```

- 后端入口：[ApiWebApplication.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/ApiWebApplication.java)
- 典型控制器：
  - 认证：[AuthController.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/controller/auth/AuthController.java)
  - 用户：[SysUserController.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/controller/SysUserController.java)
  - API 市场：[ApiMarketController.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/controller/ApiMarketController.java)
  - 业务示例：时间/天气/随机数（[TimeController.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/controller/business/TimeController.java)、[WeatherController.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/controller/business/WeatherController.java)、[RandomController.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/controller/business/RandomController.java)）
- 权限与认证：
  - 拦截器：[JwtInterceptor.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/interceptor/JwtInterceptor.java)、[ApiPermissionInterceptor.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/interceptor/ApiPermissionInterceptor.java)
  - 统一异常：[GlobalExceptionHandler.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/exception/GlobalExceptionHandler.java)
  - 指标切面：[ApiMetricsAspect.java](file:///e:/study/apiCode/api/api_web/src/main/java/com/org/api_web/aspect/ApiMetricsAspect.java)
- 公共能力：
  - JWT 工具：[JwtUtil.java](file:///e:/study/apiCode/api/api_common/src/main/java/com/org/api_common/util/JwtUtil.java)
  - 签名工具：[SignUtil.java](file:///e:/study/apiCode/api/api_common/src/main/java/com/org/api_common/util/SignUtil.java)
  - Redis 工具：[RedisUtil.java](file:///e:/study/apiCode/api/api_common/src/main/java/com/org/api_common/util/RedisUtil.java)
- SDK：
  - 客户端入口：[ApiClient.java](file:///e:/study/apiCode/api/api_sdk/src/main/java/com/org/api_sdk/ApiClient.java)

## 技术栈

- 后端：Spring Boot 3.5、JDK 17、MyBatis-Plus、MySQL、Redis、Maven
- 前端：Vue 3、Vite 5、TypeScript、Pinia、Vue Router、Element Plus、Axios

## 核心功能

- 用户认证：登录/校验、JWT 鉴权
- API 市场：浏览可用接口、查看使用说明
- 权限申请：用户申请/管理员审批、AK 状态管理
- 调用日志：记录调用明细与统计分析
- 业务接口：时间/天气/随机数等示例业务
- SDK 调用：提供签名算法与客户端封装，简化调用

## 快速开始

### 环境准备

- JDK 17、Maven 3.x、MySQL 8.x、Redis
- Node.js 18+（前端开发）

### 初始化数据库

- 创建数据库并导入业务接口初始化脚本：[init_business_apis.sql](file:///e:/study/apiCode/api/init_business_apis.sql)

### 构建与运行（后端）

- 一键构建（Windows）：

```bash
.\build.bat
```

- 手动构建：

```bash
mvn install -pl api_common -am -DskipTests
mvn package -DskipTests
```

- 运行 Web 接口模块：

```bash
cd api_web
java -jar target/api_web-0.0.1-SNAPSHOT.jar
```

- 默认端口：8080（本地联调建议在 api_web 的 application.yml 中设置为 8081 以配合前端代理）

### 运行（前端）

```bash
cd apivue
npm install
npm run dev
```

- 开发服务器：默认 http://localhost:5173
- 代理配置：见 [vite.config.ts](file:///e:/study/apiCode/api/apivue/vite.config.ts)，将 /api 转发到后端（默认 http://localhost:8081）

## 配置参考

- 数据源与 Redis（示例，位于 api_service）：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/api?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: hello666

  data:
    redis:
      host: localhost
      port: 6379
      password: hello666
      timeout: 10000
      database: 0
```

## 相关文档

- SDK 使用指南（前端展示版）：[SDK使用指南-前端展示版.md](file:///e:/study/apiCode/api/SDK使用指南-前端展示版.md)
- SDK 使用问题分析与正确使用指南：[SDK使用问题分析与正确使用指南.md](file:///e:/study/apiCode/api/SDK使用问题分析与正确使用指南.md)
- SDK 调用业务接口方案：[SDK调用业务接口方案.md](file:///e:/study/apiCode/api/SDK调用业务接口方案.md)
- 前端接入后端方案：[前端接入后端方案.md](file:///e:/study/apiCode/api/前端接入后端方案.md)
- 后端接口实现完成总结：[后端接口实现完成总结.md](file:///e:/study/apiCode/api/后端接口实现完成总结.md)

## 目录与代码索引

- 公共配置与自动装配：[CommonAutoConfiguration.java](file:///e:/study/apiCode/api/api_common/src/main/java/com/org/api_common/config/CommonAutoConfiguration.java)
- MyBatis-Plus 配置：[MybatisPlusConfig.java](file:///e:/study/apiCode/api/api_common/src/main/java/com/org/api_common/config/MybatisPlusConfig.java)
- 权限与统计服务（管理端）：[ApiPermissionServiceImpl.java](file:///e:/study/apiCode/api/api_admin_service/src/main/java/com/org/api_admin_service/service/serviceImpl/ApiPermissionServiceImpl.java)、[StatisticsServiceImpl.java](file:///e:/study/apiCode/api/api_admin_service/src/main/java/com/org/api_admin_service/service/serviceImpl/StatisticsServiceImpl.java)
- 业务服务实现（通用）：[AuthServiceImpl.java](file:///e:/study/apiCode/api/api_service/src/main/java/com/org/api_service/service/serviceImpl/AuthServiceImpl.java)、[SysUserServiceImpl.java](file:///e:/study/apiCode/api/api_service/src/main/java/com/org/api_service/service/serviceImpl/SysUserServiceImpl.java)

## 约定与注意事项

- 统一使用 JDK 17；依赖版本由父 POM 管理
- 模块间职责清晰，避免重复扫描与 Bean 冲突
- 前端通过 Vite 代理访问后端 /api 前缀，联调时保持端口一致

