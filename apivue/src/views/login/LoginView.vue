<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElDialog } from 'element-plus'
import { User, Lock, DocumentCopy } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const showKeysDialog = ref(false)
const registerResult = ref<{ accessKey: string; secretKey: string } | null>(null)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码长度不能少于4位', trigger: 'blur' }
  ]
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名长度不能少于3位', trigger: 'blur' },
    { max: 20, message: '用户名长度不能超过20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码长度不能少于4位', trigger: 'blur' },
    { max: 20, message: '密码长度不能超过20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleLogin() {
  try {
    await loginFormRef.value?.validate()
  } catch (error) {
    // 表单验证失败，不执行后续逻辑
    return
  }

  loading.value = true
  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    ElMessage.success('登录成功')
    
    // 跳转到之前的页面或首页
    const redirect = route.query.redirect as string || '/'
    router.push(redirect)
  } catch (error) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerFormRef.value?.validate()
  } catch (error) {
    // 表单验证失败，不执行后续逻辑
    return
  }

  loading.value = true
  try {
    const result = await userStore.register({
      username: registerForm.username,
      password: registerForm.password
    })
    
    // 保存注册结果（包含AK/SK）
    registerResult.value = {
      accessKey: result.accessKey,
      secretKey: result.secretKey || ''
    }
    
    // 显示密钥对话框
    showKeysDialog.value = true
  } catch (error) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function handleKeysDialogClose() {
  showKeysDialog.value = false
  // 跳转到首页
  const redirect = route.query.redirect as string || '/'
  router.push(redirect)
}

async function copyToClipboard(text: string, type: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${type}已复制到剪贴板`)
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

function switchTab(tab: string) {
  activeTab.value = tab
  // 重置表单
  if (tab === 'login' && loginFormRef.value) {
    loginFormRef.value.resetFields()
  } else if (tab === 'register' && registerFormRef.value) {
    registerFormRef.value.resetFields()
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <img src="/vite.svg" alt="logo" class="login-logo" />
        <h1 class="login-title">API 平台</h1>
        <p class="login-subtitle">企业级 API 开放平台</p>
      </div>

      <el-tabs v-model="activeTab" @tab-change="switchTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="login-form"
            @keyup.enter="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名（3-20位）"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码（4-20位）"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="login-btn"
                @click="handleRegister"
              >
                {{ loading ? '注册中...' : '注 册' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <p v-if="activeTab === 'login'">提示：请使用系统管理员分配的账号登录</p>
        <p v-else>注册成功后，系统将自动为您生成 AccessKey 和 SecretKey</p>
      </div>
    </div>

    <!-- 注册成功显示密钥对话框 -->
    <el-dialog
      v-model="showKeysDialog"
      title="注册成功！请妥善保存您的密钥"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @close="handleKeysDialogClose"
    >
      <div class="keys-dialog-content">
        <el-alert
          type="warning"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #title>
            <strong>重要提示：</strong>SecretKey 仅显示一次，请立即保存！关闭对话框后将无法再次查看。
          </template>
        </el-alert>

        <div class="key-item">
          <div class="key-label">
            <span>AccessKey (AK):</span>
            <el-button
              :icon="DocumentCopy"
              size="small"
              text
              @click="copyToClipboard(registerResult?.accessKey || '', 'AccessKey')"
            >
              复制
            </el-button>
          </div>
          <el-input
            :model-value="registerResult?.accessKey || ''"
            readonly
            class="key-value"
          />
        </div>

        <div class="key-item">
          <div class="key-label">
            <span>SecretKey (SK):</span>
            <el-button
              :icon="DocumentCopy"
              size="small"
              text
              @click="copyToClipboard(registerResult?.secretKey || '', 'SecretKey')"
            >
              复制
            </el-button>
          </div>
          <el-input
            :model-value="registerResult?.secretKey || ''"
            readonly
            type="password"
            class="key-value"
          />
        </div>
      </div>

      <template #footer>
        <el-button type="primary" @click="handleKeysDialogClose">
          我已保存，前往首页
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-logo {
  width: 60px;
  height: 60px;
  margin-bottom: 16px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #999;
}

.login-form {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
}

.login-footer p {
  font-size: 12px;
  color: #999;
}

.login-tabs {
  margin-top: 20px;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

.keys-dialog-content {
  padding: 10px 0;
}

.key-item {
  margin-bottom: 20px;
}

.key-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.key-value {
  width: 100%;
}

.key-value :deep(.el-input__inner) {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}
</style>

