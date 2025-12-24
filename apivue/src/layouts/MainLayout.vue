<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAdminAkSk } from '@/utils/adminSign'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled,
  Connection,
  User,
  SwitchButton,
  Fold,
  Expand,
  Key,
  DataLine,
  Document,
  UserFilled,
  Setting,
  TrendCharts,
  Shop
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

// 是否管理员（permissionType === 3）
const isAdmin = computed(() => userStore.userInfo?.permissionType === 3)

// 菜单列表：普通用户和管理员看到不同的菜单
const menuList = computed(() => {
  // 所有用户都能看到的菜单
  const base = [
    { path: '/dashboard', title: '首页', icon: HomeFilled },
    { path: '/api-market', title: '接口市场', icon: Shop },
    { path: '/api-list', title: '我的接口', icon: Connection },
    { path: '/sdk-guide', title: 'SDK使用指南', icon: Document },
    { path: '/my-statistics', title: '我的调用统计', icon: DataLine },
    { path: '/profile', title: '个人中心', icon: UserFilled }
  ]
  
  // 管理员额外看到的菜单
  if (isAdmin.value) {
    base.splice(5, 0, 
      { path: '/api-manage', title: 'API管理', icon: Setting },
      { path: '/user-manage', title: '用户管理', icon: User },
      { path: '/global-statistics', title: '全局统计', icon: TrendCharts },
      { path: '/operate-log', title: '操作日志', icon: Document }
    )
  }
  
  return base
})

// 管理员 AK/SK 设置弹窗
const akDialogVisible = ref(false)
const akForm = ref({
  accessKey: '',
  secretKey: ''
})

function openAkDialog() {
  const { accessKey, secretKey } = getAdminAkSk()
  akForm.value.accessKey = accessKey
  akForm.value.secretKey = secretKey
  akDialogVisible.value = true
}

function saveAkSk() {
  localStorage.setItem('adminAccessKey', akForm.value.accessKey || '')
  localStorage.setItem('adminSecretKey', akForm.value.secretKey || '')
  ElMessage.success('管理员 AK/SK 已保存')
  akDialogVisible.value = false
}

// 切换侧边栏
function toggleSidebar() {
  isCollapse.value = !isCollapse.value
}

// 退出登录
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    ElMessage.success('已退出登录')
  } catch (error) {
    // 用户取消操作
  }
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <img src="/vite.svg" alt="logo" class="logo-img" />
        <span v-show="!isCollapse" class="logo-text">API平台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#1d1e1f"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item v-for="menu in menuList" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <template #title>{{ menu.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title !== '首页'">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" icon="User" />
              <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><UserFilled /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" @click="openAkDialog">
                  <el-icon><Key /></el-icon>
                  管理员密钥
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <RouterView />
      </el-main>

      <!-- 管理员 AK/SK 设置弹窗 -->
      <el-dialog
        v-model="akDialogVisible"
        title="管理员密钥设置"
        width="480px"
      >
        <el-form label-width="90px" :model="akForm">
          <el-form-item label="AccessKey">
            <el-input
              v-model="akForm.accessKey"
              placeholder="请输入管理员 AccessKey"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="SecretKey">
            <el-input
              v-model="akForm.secretKey"
              placeholder="请输入管理员 SecretKey"
              show-password
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item>
            <span style="font-size: 12px; color: #999;">
              说明：用于调用 /api/admin/** 管理接口的 AK/SK，不会上传到服务端，仅保存在当前浏览器。
            </span>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="akDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAkSk">保存</el-button>
        </template>
      </el-dialog>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #1d1e1f;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid #333;
}

.logo-img {
  width: 32px;
  height: 32px;
}

.logo-text {
  margin-left: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
}

.layout-header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 16px;
  color: #666;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  font-size: 14px;
  color: #333;
}

.layout-main {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>

