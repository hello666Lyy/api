<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { User, Lock, Key, Document } from '@element-plus/icons-vue'
import { changePassword } from '@/api/auth'
import { getAkStatusByToken, generateNewAkSkByToken, type AkStatusVO } from '@/api/ak'

const userStore = useUserStore()

// 个人信息
const userInfo = computed(() => userStore.userInfo)

// 密码修改表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRef = ref()
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)

// AK/SK 相关
const akStatus = ref<AkStatusVO>({
  accessKey: userInfo.value?.accessKey || '',
  status: 1,
  permissionType: userInfo.value?.permissionType || 1,
  expireTime: null,
  permissionDesc: ''
})
const akStatusLoading = ref(false)

const generatePasswordDialogVisible = ref(false)
const generateAkDialogVisible = ref(false)
const generateAkLoading = ref(false)
const passwordFormForAk = ref({
  password: ''
})
const newAkSk = ref({
  newAccessKey: '',
  newSecretKey: ''
})

// 修改密码
async function handleChangePassword() {
  if (!passwordFormRef.value) return
  
  try {
    // 先验证表单
    const valid = await passwordFormRef.value.validate()
    if (!valid) return
    
    // 额外验证：两次密码是否一致
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    
    // 额外验证：密码长度
    if (passwordForm.value.newPassword.length < 6) {
      ElMessage.error('密码长度不能少于6位')
      return
    }
    
    passwordLoading.value = true
    // 调用修改密码接口
    await changePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error: any) {
    console.error('修改密码失败:', error)
    // 错误已在拦截器中处理
  } finally {
    passwordLoading.value = false
  }
}

// 打开生成新AK/SK密码输入弹窗
function openGenerateAkDialog() {
  passwordFormForAk.value.password = ''
  generatePasswordDialogVisible.value = true
}

// 生成新AK/SK
async function handleGenerateNewAkSk() {
  if (!passwordFormForAk.value.password) {
    ElMessage.warning('请输入密码')
    return
  }

  try {
    await ElMessageBox.confirm(
      '生成新AK/SK后，旧的SecretKey将失效，请确保已保存新的密钥。是否继续？',
      '提示',
      {
        type: 'warning'
      }
    )
    
    generateAkLoading.value = true
    const res = await generateNewAkSkByToken(passwordFormForAk.value.password)
    
    if (res.data) {
      newAkSk.value = {
        newAccessKey: res.data.newAccessKey,
        newSecretKey: res.data.newSecretKey,
        newToken: res.data.newToken
      }
      
      generatePasswordDialogVisible.value = false
      generateAkDialogVisible.value = true
      
      // 更新用户信息（如果有新Token）
      if (res.data.newToken && userStore) {
        // 更新Token
        localStorage.setItem('token', res.data.newToken)
        // 重新获取用户信息
        await userStore.fetchUserInfo()
      }
      
      // 重新获取AK状态
      await fetchAkStatus()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      const errorMsg = error.message || '生成失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    generateAkLoading.value = false
  }
}

// 获取AK状态
async function fetchAkStatus() {
  try {
    akStatusLoading.value = true
    const res = await getAkStatusByToken()
    if (res.data) {
      akStatus.value = res.data
    }
  } catch (error: any) {
    console.error('获取AK状态失败:', error)
    // 如果获取失败，使用用户信息中的默认值
    akStatus.value = {
      accessKey: userInfo.value?.accessKey || '',
      status: 1,
      permissionType: userInfo.value?.permissionType || 1,
      expireTime: null,
      permissionDesc: ''
    }
  } finally {
    akStatusLoading.value = false
  }
}

// 查看AK状态详情
async function handleViewAkStatus() {
  await fetchAkStatus()
  const permissionTypeText = akStatus.value.permissionDesc || 
    (akStatus.value.permissionType === 3 ? '管理员' : 
     akStatus.value.permissionType === 2 ? '读写' : '只读')
  ElMessageBox.alert(
    `AccessKey: ${akStatus.value.accessKey}\n状态: ${akStatus.value.status === 1 ? '启用' : '禁用'}\n权限类型: ${permissionTypeText}\n过期时间: ${akStatus.value.expireTime || '永不过期'}`,
    'AK状态信息',
    {
      confirmButtonText: '确定'
    }
  )
}

// 复制到剪贴板
function copyToClipboard(text: string, label: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`${label}已复制到剪贴板`)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 密码验证规则
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' }
  ]
}

// 初始化时获取AK状态
onMounted(() => {
  fetchAkStatus()
})
</script>

<template>
  <div class="profile-page">
    <!-- 个人信息卡片 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-header">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </div>
      </template>
      
      <div class="info-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ userInfo?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录过期时间">
            {{ userInfo?.expireTime ? new Date(userInfo.expireTime).toLocaleString() : '永不过期' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 安全设置 -->
    <el-card shadow="never" class="security-card">
      <template #header>
        <div class="card-header">
          <el-icon><Lock /></el-icon>
          <span>安全设置</span>
        </div>
      </template>
      
      <div class="security-content">
        <el-space direction="vertical" size="large" style="width: 100%">
          <div class="security-item">
            <div class="security-info">
              <h4>修改密码</h4>
              <p>定期修改密码可以提高账户安全性</p>
            </div>
            <el-button type="primary" @click="passwordDialogVisible = true">修改密码</el-button>
          </div>
          
          <el-divider />
          
          <div class="security-item">
            <div class="security-info">
              <h4>生成新AK/SK</h4>
              <p>生成新的AccessKey和SecretKey，旧的SecretKey将失效</p>
            </div>
            <el-button type="warning" @click="openGenerateAkDialog" :loading="generateAkLoading">
              生成新AK/SK
            </el-button>
          </div>
        </el-space>
      </div>
    </el-card>

    <!-- AK状态信息 -->
    <el-card shadow="never" class="ak-status-card">
      <template #header>
        <div class="card-header">
          <el-icon><Key /></el-icon>
          <span>AK状态信息</span>
        </div>
      </template>
      
      <div class="ak-status-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="AccessKey" :span="2">
            <div class="ak-display">
              <span class="ak-text">{{ akStatus.accessKey || userInfo?.accessKey || '-' }}</span>
              <el-button type="primary" link size="small" @click="copyToClipboard(akStatus.accessKey || userInfo?.accessKey || '', 'AccessKey')">
                复制
              </el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="akStatus.status === 1 ? 'success' : 'danger'">
              {{ akStatus.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限类型">
            <el-tag :type="akStatus.permissionType === 3 ? 'danger' : akStatus.permissionType === 2 ? 'warning' : 'info'">
              {{ akStatus.permissionType === 3 ? '管理员' : akStatus.permissionType === 2 ? '读写' : '只读' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="过期时间">
            {{ akStatus.expireTime ? new Date(akStatus.expireTime).toLocaleString() : '永不过期' }}
          </el-descriptions-item>
          <el-descriptions-item :span="2">
            <el-button type="primary" @click="handleViewAkStatus" :loading="akStatusLoading">查看详细状态</el-button>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 使用说明 -->
    <el-card shadow="never" class="help-card">
      <template #header>
        <div class="card-header">
          <span>使用说明</span>
        </div>
      </template>
      <div class="help-content">
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <div>
              <p style="margin: 0 0 8px 0; font-weight: 600;">重要提示：</p>
              <ul style="margin: 0; padding-left: 20px;">
                <li>SecretKey 仅显示一次，请妥善保管，不要泄露给他人</li>
                <li>如果 SecretKey 丢失，需要重新生成，旧的 SecretKey 将失效</li>
                <li>使用 AK/SK 进行 API 调用时，需要在请求头中携带签名信息</li>
                <li>签名算法：sign = HMAC-SHA256(accessKey + secretKey + timestamp + nonce)</li>
              </ul>
            </div>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px">
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 生成新AK/SK密码输入弹窗 -->
    <el-dialog v-model="generatePasswordDialogVisible" title="生成新AK/SK" width="500px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #default>
          <p style="margin: 0;">生成新AK/SK后，旧的SecretKey将失效，请确保已保存新的密钥。</p>
        </template>
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="登录密码" required>
          <el-input
            v-model="passwordFormForAk.password"
            type="password"
            placeholder="请输入登录密码以确认身份"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generatePasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateAkLoading" @click="handleGenerateNewAkSk">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新AK/SK显示弹窗 -->
    <el-dialog v-model="generateAkDialogVisible" title="新AK/SK生成成功" width="600px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #default>
          <div>
            <p style="margin: 0 0 8px 0; font-weight: 600;">重要提示：</p>
            <p style="margin: 0;">SecretKey 仅显示一次，请立即保存。如果丢失，需要重新生成。</p>
          </div>
        </template>
      </el-alert>
      
      <el-descriptions :column="1" border>
        <el-descriptions-item label="新AccessKey">
          <div class="ak-display">
            <span class="ak-text">{{ newAkSk.newAccessKey }}</span>
            <el-button type="primary" link size="small" @click="copyToClipboard(newAkSk.newAccessKey, 'AccessKey')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="新SecretKey">
          <div class="ak-display">
            <span class="ak-text">{{ newAkSk.newSecretKey }}</span>
            <el-button type="primary" link size="small" @click="copyToClipboard(newAkSk.newSecretKey, 'SecretKey')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item v-if="newAkSk.newToken" label="新Token">
          <div class="ak-display">
            <span class="ak-text">{{ newAkSk.newToken }}</span>
            <el-button type="primary" link size="small" @click="copyToClipboard(newAkSk.newToken, 'Token')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      
      <template #footer>
        <el-button type="primary" @click="generateAkDialogVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 0;
}

.info-card,
.security-card,
.ak-status-card,
.help-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.help-content {
  padding: 8px 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.info-content,
.security-content,
.ak-status-content {
  padding: 8px 0;
}

.ak-text {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
  word-break: break-all;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.security-info h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #303133;
}

.security-info p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.ak-display {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

