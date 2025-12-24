<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { Key, CopyDocument, Refresh, Download } from '@element-plus/icons-vue'
import { getAkStatusByToken, generateNewAkSkByToken, type AkStatusVO } from '@/api/ak'
import { downloadSdk } from '@/api/sdk'

const userStore = useUserStore()

// 我的AK信息
const myAkInfo = ref<AkStatusVO & { secretKey?: string; createTime?: string }>({
  accessKey: userStore.userInfo?.accessKey || '',
  secretKey: undefined, // SecretKey不返回，需要单独获取
  status: 1,
  permissionType: userStore.userInfo?.permissionType || 1,
  expireTime: null,
  permissionDesc: '',
  createTime: ''
})
const akInfoLoading = ref(false)

const showSecretKey = ref(false)
const generateDialogVisible = ref(false)
const generateLoading = ref(false)
const passwordForm = ref({
  password: ''
})

const newAkSk = ref({
  newAccessKey: '',
  newSecretKey: '',
  newToken: ''
})

const showNewAkDialog = ref(false)

// 权限类型文本
const permissionTypeText = computed(() => {
  const type = myAkInfo.value.permissionType
  return type === 3 ? '管理员' : type === 2 ? '读写' : '只读'
})

const permissionTypeTag = computed(() => {
  const type = myAkInfo.value.permissionType
  return type === 3 ? 'danger' : type === 2 ? 'warning' : 'info'
})

// 查看SecretKey
function handleViewSecretKey() {
  if (showSecretKey.value) {
    showSecretKey.value = false
  } else {
    // 注意：后端不返回SecretKey，只能通过生成新AK/SK时获取
    // 这里提示用户如果需要SecretKey，需要重新生成
    ElMessageBox.alert(
      'SecretKey仅在新生成AK/SK时显示一次。如需查看SecretKey，请重新生成AK/SK。',
      '提示',
      {
        confirmButtonText: '知道了'
      }
    )
  }
}

// 复制到剪贴板
function copyToClipboard(text: string, label: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`${label}已复制到剪贴板`)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 生成新AK/SK
function openGenerateDialog() {
  passwordForm.value.password = ''
  generateDialogVisible.value = true
}

async function handleGenerateNewAkSk() {
  if (!passwordForm.value.password) {
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

    generateLoading.value = true
    const res = await generateNewAkSkByToken(passwordForm.value.password)
    
    if (res.data) {
      newAkSk.value = {
        newAccessKey: res.data.newAccessKey,
        newSecretKey: res.data.newSecretKey,
        newToken: res.data.newToken || ''
      }
      
      generateDialogVisible.value = false
      showNewAkDialog.value = true
      
      // 更新用户信息（如果有新Token）
      if (res.data.newToken && userStore) {
        localStorage.setItem('token', res.data.newToken)
        await userStore.fetchUserInfo()
      }
      
      // 重新获取AK状态
      await fetchAkInfo()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      const errorMsg = error.message || '生成失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    generateLoading.value = false
  }
}

// 获取AK信息
async function fetchAkInfo() {
  try {
    akInfoLoading.value = true
    const res = await getAkStatusByToken()
    if (res.data) {
      myAkInfo.value = {
        ...res.data,
        secretKey: undefined, // SecretKey不返回
        createTime: res.data.expireTime || ''
      }
    }
  } catch (error: any) {
    console.error('获取AK信息失败:', error)
    // 使用用户信息中的默认值
    myAkInfo.value = {
      accessKey: userStore.userInfo?.accessKey || '',
      status: 1,
      permissionType: userStore.userInfo?.permissionType || 1,
      expireTime: null,
      permissionDesc: '',
      createTime: ''
    }
  } finally {
    akInfoLoading.value = false
  }
}

// 查看AK状态
async function handleViewAkStatus() {
  await fetchAkInfo()
  const permissionText = myAkInfo.value.permissionDesc || permissionTypeText.value
  ElMessageBox.alert(
    `AccessKey: ${myAkInfo.value.accessKey}\n状态: ${myAkInfo.value.status === 1 ? '启用' : '禁用'}\n权限类型: ${permissionText}\n过期时间: ${myAkInfo.value.expireTime ? new Date(myAkInfo.value.expireTime).toLocaleString() : '永不过期'}`,
    'AK状态信息',
    {
      confirmButtonText: '确定'
    }
  )
}

// 下载SDK
async function handleDownloadSdk() {
  try {
    const blob = await downloadSdk()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'api_sdk-1.0-SNAPSHOT.jar'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('SDK下载成功')
  } catch (error: any) {
    console.error('SDK下载失败:', error)
    const errorMsg = error.message || 'SDK下载失败，请稍后重试'
    ElMessage.error(errorMsg)
  }
}

onMounted(() => {
  fetchAkInfo()
})
</script>

<template>
  <div class="my-ak-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>我的 AK/SK</h2>
        <p>管理我的AccessKey和SecretKey，用于API调用认证</p>
      </div>
      <el-icon :size="48" color="#409eff"><Key /></el-icon>
    </div>

    <!-- AK信息卡片 -->
    <el-card shadow="never" class="ak-info-card">
      <template #header>
        <div class="card-header">
          <el-icon><Key /></el-icon>
          <span>我的 AK/SK 信息</span>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="AccessKey" :span="2">
          <div class="ak-display">
            <code class="ak-text">{{ myAkInfo.accessKey }}</code>
            <el-button type="primary" link size="small" :icon="CopyDocument" @click="copyToClipboard(myAkInfo.accessKey, 'AccessKey')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="SecretKey" :span="2">
          <div class="ak-display">
            <code class="ak-text" v-if="showSecretKey">{{ myAkInfo.secretKey }}</code>
            <code class="ak-text" v-else>sk_test_**********</code>
            <el-button type="primary" link size="small" @click="handleViewSecretKey">
              {{ showSecretKey ? '隐藏' : '显示' }}
            </el-button>
            <el-button v-if="showSecretKey" type="primary" link size="small" :icon="CopyDocument" @click="copyToClipboard(myAkInfo.secretKey, 'SecretKey')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="myAkInfo.status === 1 ? 'success' : 'danger'">
            {{ myAkInfo.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="权限类型">
          <el-tag :type="permissionTypeTag">
            {{ permissionTypeText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="过期时间">
          {{ myAkInfo.expireTime ? new Date(myAkInfo.expireTime).toLocaleString() : '永不过期' }}
        </el-descriptions-item>
        <el-descriptions-item label="权限描述">
          {{ myAkInfo.permissionDesc || permissionTypeText }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 操作卡片 -->
    <el-card shadow="never" class="action-card">
      <template #header>
        <div class="card-header">
          <span>操作</span>
        </div>
      </template>

      <el-space direction="vertical" size="large" style="width: 100%">
        <div class="action-item">
          <div class="action-info">
            <h4>查看AK状态</h4>
            <p>查看当前AK的详细状态信息</p>
          </div>
          <el-button type="primary" @click="handleViewAkStatus" :loading="akInfoLoading">查看状态</el-button>
        </div>

        <el-divider />

        <div class="action-item">
          <div class="action-info">
            <h4>下载SDK</h4>
            <p>下载Java SDK jar包，快速集成到您的项目中</p>
          </div>
          <el-button type="success" :icon="Download" @click="handleDownloadSdk">
            下载SDK
          </el-button>
        </div>

        <el-divider />

        <div class="action-item">
          <div class="action-info">
            <h4>生成新AK/SK</h4>
            <p>生成新的AccessKey和SecretKey，旧的SecretKey将失效</p>
          </div>
          <el-button type="warning" :icon="Refresh" @click="openGenerateDialog" :loading="generateLoading">
            生成新AK/SK
          </el-button>
        </div>
      </el-space>
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
                <li>签名算法：sign = MD5(accessKey + secretKey + timestamp + nonce)</li>
              </ul>
            </div>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- 生成新AK/SK弹窗 -->
    <el-dialog v-model="generateDialogVisible" title="生成新AK/SK" width="500px">
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
            v-model="passwordForm.password"
            type="password"
            placeholder="请输入登录密码以确认身份"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerateNewAkSk">确定</el-button>
      </template>
    </el-dialog>

    <!-- 显示新AK/SK弹窗 -->
    <el-dialog v-model="showNewAkDialog" title="新AK/SK生成成功" width="600px">
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
            <code class="ak-text">{{ newAkSk.newAccessKey }}</code>
            <el-button type="primary" link size="small" :icon="CopyDocument" @click="copyToClipboard(newAkSk.newAccessKey, 'AccessKey')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="新SecretKey">
          <div class="ak-display">
            <code class="ak-text">{{ newAkSk.newSecretKey }}</code>
            <el-button type="primary" link size="small" :icon="CopyDocument" @click="copyToClipboard(newAkSk.newSecretKey, 'SecretKey')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="新Token">
          <div class="ak-display">
            <code class="ak-text">{{ newAkSk.newToken }}</code>
            <el-button type="primary" link size="small" :icon="CopyDocument" @click="copyToClipboard(newAkSk.newToken, 'Token')">
              复制
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button type="primary" @click="showNewAkDialog = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.my-ak-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.ak-info-card,
.action-card,
.help-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.ak-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ak-text {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
}

.action-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-info h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #303133;
}

.action-info p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.help-content {
  padding: 8px 0;
}
</style>

