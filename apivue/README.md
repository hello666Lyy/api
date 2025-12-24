# API 管理平台 - 前端项目

基于 Vue3 + TypeScript + Vite + Pinia + Element Plus 构建的 API 管理平台前端应用。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - 类型安全的 JavaScript 超集
- **Vite** - 下一代前端构建工具
- **Vue Router** - 官方路由管理器
- **Pinia** - Vue 的状态管理库
- **Element Plus** - 基于 Vue 3 的组件库
- **Axios** - HTTP 客户端

## 项目结构

```
apivue/
├── src/
│   ├── api/              # API 接口封装
│   │   ├── auth.ts       # 认证相关接口（登录、退出、用户信息）
│   │   └── request.ts    # Axios 请求封装（拦截器、token处理）
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue  # 主布局（侧边栏+顶部栏）
│   ├── router/           # 路由配置
│   │   └── index.ts      # 路由定义和守卫
│   ├── stores/           # Pinia 状态管理
│   │   └── user.ts       # 用户状态管理（登录、退出、用户信息）
│   ├── utils/            # 工具函数
│   │   └── auth.ts       # 认证工具（token、用户信息存储）
│   ├── views/            # 页面组件
│   │   ├── login/        # 登录页
│   │   ├── dashboard/    # 首页/仪表盘
│   │   ├── api-manage/   # API 管理
│   │   └── user/         # 用户管理
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html            # HTML 模板
├── package.json          # 项目依赖
├── tsconfig.json         # TypeScript 配置
└── vite.config.ts        # Vite 配置
```

## 功能特性

### ✅ 已实现功能

1. **登录认证**
   - 用户名密码登录
   - Token 自动保存和管理
   - 登录状态持久化（localStorage）
   - 路由守卫保护

2. **登录状态管理**
   - Pinia 状态管理
   - Token 自动注入请求头
   - 401 自动跳转登录
   - 刷新页面自动恢复登录状态

3. **退出登录**
   - 调用后端退出接口
   - 清除本地 token 和用户信息
   - 自动跳转登录页
   - 退出确认对话框

4. **主布局**
   - 响应式侧边栏（可折叠）
   - 顶部导航栏
   - 用户信息展示
   - 权限控制（管理员菜单）

## 快速开始

### 安装依赖

```bash
cd apivue
npm install
```

### 开发环境运行

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 配置说明

### API 代理配置

在 `vite.config.ts` 中配置了 API 代理：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081',  // 后端服务地址
      changeOrigin: true
    }
  }
}
```

如果后端服务运行在不同端口，请修改 `target` 地址。

### 环境变量（可选）

可以创建 `.env.development` 和 `.env.production` 文件来配置不同环境的 API 地址：

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8081

# .env.production
VITE_API_BASE_URL=https://api.example.com
```

然后在 `vite.config.ts` 中使用 `process.env.VITE_API_BASE_URL`。

## 核心功能说明

### 1. 登录流程

1. 用户在登录页输入用户名和密码
2. 调用 `/api/auth/login` 接口
3. 后端返回 token 和用户信息
4. 保存 token 到 localStorage 和 Pinia store
5. 跳转到首页或之前访问的页面

### 2. Token 管理

- **存储位置**: localStorage (`api_token`)
- **请求头**: 自动在 `Authorization: Bearer <token>` 中携带
- **过期处理**: 401 响应时自动清除 token 并跳转登录页

### 3. 路由守卫

- 未登录访问受保护路由 → 跳转登录页
- 已登录访问登录页 → 跳转首页
- 非管理员访问管理员路由 → 跳转首页
- 有 token 但无用户信息 → 自动获取用户信息

### 4. 退出登录流程

1. 用户点击退出登录
2. 显示确认对话框
3. 调用 `/api/auth/logout` 接口（可选）
4. 清除本地 token 和用户信息
5. 跳转到登录页

## API 接口说明

### 认证相关接口

- `POST /api/auth/login` - 用户登录
- `GET /api/auth/userInfo` - 获取当前用户信息（需要 token）
- `POST /api/auth/logout` - 退出登录（需要 token）

### 请求/响应格式

**登录请求**:
```json
{
  "username": "admin",
  "password": "123456"
}
```

**登录响应**:
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "admin",
    "accessKey": "ak_xxx",
    "permissionType": 3,
    "expireTime": 1704067200000
  }
}
```

## 开发建议

### 下一步开发顺序

1. ✅ **登录页面和状态管理**（已完成）
2. ✅ **主布局和退出登录**（已完成）
3. **API 管理模块** - 接口列表、新增、编辑、删除
4. **用户管理模块** - 用户列表、AK/SK 管理
5. **调用统计** - 图表展示、数据分析
6. **权限管理** - 角色权限配置

### 代码规范

- 使用 TypeScript 严格模式
- 组件使用 `<script setup>` 语法
- API 接口统一在 `src/api` 目录管理
- 状态管理使用 Pinia
- 样式使用 scoped CSS

## 常见问题

### Q: 登录后刷新页面，用户信息丢失？

A: 已实现自动恢复功能。路由守卫会检查 token，如果有 token 但没有用户信息，会自动调用 `/api/auth/userInfo` 获取用户信息。

### Q: 如何修改后端 API 地址？

A: 修改 `vite.config.ts` 中的 `proxy.target` 配置，或使用环境变量。

### Q: Token 过期如何处理？

A: 当后端返回 401 状态码时，axios 拦截器会自动清除 token 并跳转到登录页。

## 许可证

MIT

