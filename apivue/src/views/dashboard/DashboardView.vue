<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { Connection, User, DataLine, Timer, Key, Setting } from '@element-plus/icons-vue'
import { getGlobalStatistics, type GlobalStatisticsVO } from '@/api/statistics'

const userStore = useUserStore()

// 是否管理员
const isAdmin = computed(() => userStore.userInfo?.permissionType === 3)

// 首页统计数据（接入全局统计接口）
const statistics = ref([
  { title: 'API 总数', value: 0, icon: Connection, color: '#409eff', key: 'totalApis' },
  { title: '用户总数', value: 0, icon: User, color: '#67c23a', key: 'totalUsers' },
  { title: '今日调用', value: 0, icon: DataLine, color: '#e6a23c', key: 'todayCalls' },
  { title: '平均响应', value: '0ms', icon: Timer, color: '#f56c6c', key: 'avgResponseTime' }
])

const loadingStats = ref(false)

async function fetchGlobalStats() {
  loadingStats.value = true
  try {
    const res = await getGlobalStatistics()
    if (res.data) {
      updateStatistics(res.data)
    }
  } catch (e) {
    // 保持默认值即可
    console.warn('获取全局统计失败', e)
  } finally {
    loadingStats.value = false
  }
}

function updateStatistics(data: GlobalStatisticsVO) {
  statistics.value = statistics.value.map(item => {
    const v = data[item.key as keyof GlobalStatisticsVO]
    if (item.key === 'avgResponseTime') {
      const num = typeof v === 'number' ? v : Number(v || 0)
      return { ...item, value: `${num.toFixed(0)}ms` }
    }
    if (typeof v === 'number') {
      return { ...item, value: v.toLocaleString() }
    }
    return item
  })
}

onMounted(() => {
  fetchGlobalStats()
})
</script>

<template>
  <div class="dashboard">
    <!-- 顶部欢迎区 -->
    <section class="hero">
      <div class="hero-text">
        <h2>欢迎回来，{{ userStore.userInfo?.username || '用户' }}</h2>
        <p>这里是 API 开放平台，你可以查看可用API、管理你的AK/SK并查看调用统计。</p>
      </div>
      <div class="hero-badge">
        <span class="hero-label">API平台</span>
      </div>
    </section>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="stat in statistics" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">{{ stat.title }}</p>
              <p class="stat-value">{{ stat.value }}</p>
            </div>
            <div class="stat-icon" :style="{ backgroundColor: stat.color }">
              <el-icon :size="26" color="#fff">
                <component :is="stat.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作（所有用户可见） -->
    <el-row :gutter="20" class="quick-row">
      <el-col :span="8">
        <el-card shadow="never" class="quick-card api-card" @click="$router.push('/api-list')">
          <div class="quick-main">
            <div>
              <h3>API 列表</h3>
              <p>查看所有可用的API接口，了解接口的使用方法和参数说明。</p>
            </div>
            <el-icon class="quick-icon primary">
              <Connection />
            </el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="quick-card user-card" @click="$router.push('/my-statistics')">
          <div class="quick-main">
            <div>
              <h3>我的调用统计</h3>
              <p>查看我的API调用记录和统计数据，了解使用情况。</p>
            </div>
            <el-icon class="quick-icon info">
              <DataLine />
            </el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="quick-card ak-card" @click="$router.push('/my-ak')">
          <div class="quick-main">
            <div>
              <h3>我的 AK/SK</h3>
              <p>管理我的AccessKey和SecretKey，用于API调用认证。</p>
            </div>
            <el-icon class="quick-icon warning">
              <Key />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 管理员快捷操作 -->
    <el-row v-if="isAdmin" :gutter="20" class="quick-row admin-row">
      <el-col :span="12">
        <el-card shadow="never" class="quick-card admin-card" @click="$router.push('/api-manage')">
          <div class="quick-main">
            <div>
              <h3>API 管理</h3>
              <p>创建和管理平台API接口，配置接口信息和状态。</p>
            </div>
            <el-icon class="quick-icon danger">
              <Setting />
            </el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="quick-card admin-card" @click="$router.push('/user-manage')">
          <div class="quick-main">
            <div>
              <h3>用户管理</h3>
              <p>管理所有用户和AK/SK，查看全局统计和操作日志。</p>
            </div>
            <el-icon class="quick-icon danger">
              <User />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 12px 8px 24px;
  background: radial-gradient(circle at top left, #eef2ff, #f9fafb 40%, #f1f5f9 80%);
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 45%, #0ea5e9 100%);
  color: #fff;
  margin-bottom: 22px;
  box-shadow: 0 18px 30px rgba(99, 102, 241, 0.35);
}

.hero-text h2 {
  margin: 0 0 6px;
  font-size: 22px;
}

.hero-text p {
  margin: 0;
  font-size: 13px;
  opacity: 0.9;
}

.hero-badge {
  display: flex;
  align-items: center;
}

.hero-label {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-title {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: #111827;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-row {
  margin-top: 4px;
}

.quick-card {
  border-radius: 14px;
  border: none;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.18);
}

.quick-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quick-main h3 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #111827;
}

.quick-main p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.quick-icon.primary {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.quick-icon.info {
  background: linear-gradient(135deg, #0ea5e9, #22c55e);
}

.quick-icon.warning {
  background: linear-gradient(135deg, #e6a23c, #f56c6c);
}

.quick-icon.danger {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
}

.admin-row {
  margin-top: 20px;
}

.admin-card {
  border: 2px dashed #e4e7ed;
}
</style>

