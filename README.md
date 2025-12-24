这个写的不对，，，，，，，，有些东西不全。。。。。。。。。

# API项目

基于Spring Boot 3.5.0 + JDK 17的多模块的单体(可改微服务)项目

## 项目结构

```
api/
├── api_common/          # 公共模块：工具类、实体、配置
├── api_service/         # 业务服务模块：Mapper、Service、数据访问
├── api_web/            # Web接口模块：Controller、API接口
├── pom.xml             # 父POM：统一依赖版本管理
└── build.bat           # Windows构建脚本
```

## 技术栈

- **框架**: Spring Boot 3.5.0
- **JDK**: 17
- **数据库**: MySQL 8.x
- **ORM**: MyBatis-Plus 3.5.7
- **缓存**: Redis
- **构建**: Maven 3.x

## 快速开始

### 1. 环境准备

- JDK 17 (必须)
- MySQL 8.x
- Redis
- Maven 3.x

### 2. 数据库配置

创建数据库：
```sql
CREATE DATABASE api CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 项目构建

#### 方法一：使用构建脚本（推荐）

```bash
# Windows环境下双击运行 build.bat
# 或命令行执行：
.\build.bat
```

#### 方法二：手动构建

```bash
# 1. 安装公共模块
mvn install -pl api_common -am -DskipTests

# 2. 编译项目（详细日志）
mvn compile -X

# 3. 打包项目
mvn package -DskipTests
```

### 4. 运行项目

```bash
# 进入web模块目录
cd api_web

# 运行Spring Boot应用
java -jar target/api_web-0.0.1-SNAPSHOT.jar
```

应用将在 `http://localhost:8080` 启动

## 常见问题解决方案

### 1. Maven依赖找不到自定义模块

**问题**: `api_common` 或 `api_service` 模块找不到

**解决**: 先安装公共模块到本地仓库
```bash
mvn install -pl api_common -am -DskipTests
```

### 2. JDK版本不匹配

**问题**: 编译错误，提示JDK版本不匹配

**解决**:
- 确保系统安装了JDK 17
- 检查JAVA_HOME环境变量指向JDK 17
- 项目统一使用JDK 17，不支持其他版本

### 3. MySQL连接失败

**问题**: `Communications link failure`

**解决**:
- 检查MySQL服务是否启动
- 确认数据库存在：`api`
- 检查用户名密码（默认root/hello666）
- 确认端口（默认3306）

### 4. Redis连接失败

**问题**: Redis连接超时

**解决**:
- 检查Redis服务是否启动
- 确认密码设置（默认hello666）
- 检查端口（默认6379）

### 5. 编译失败无详细日志

**解决**: 使用详细日志模式
```bash
mvn compile -X
```

### 6. Bean定义重复

**问题**: Spring启动时出现多个同名Bean

**解决**:
- 检查各模块的`@ComponentScan`和`@MapperScan`配置
- 确保不同模块扫描不同的包路径
- `api_web`模块已排除数据源自动配置

## 开发注意事项

1. **变量命名**: 使用清晰语义化的命名，避免a、b、c等无意义命名
2. **代码注释**: 统一使用中文注释
3. **依赖管理**: 所有依赖版本在父POM中统一管理
4. **模块职责**:
   - `api_common`: 公共工具类、实体类、配置
   - `api_service`: 业务逻辑、数据访问
   - `api_web`: 接口控制器、API定义

## 项目配置

### 数据库配置 (api_service/src/main/resources/application.yml)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/api?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: hello666

  data:
    redis:
      port: 6379
      password: hello666
      host: localhost
      timeout: 10000
      database: 0
```

### 端口配置

默认端口: 8080

修改端口在 `api_web/src/main/resources/application.yml` 中添加：
```yaml
server:
  port: 8081
```
