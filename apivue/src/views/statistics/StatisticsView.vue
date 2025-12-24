<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, Timer, TrendCharts, Document } from '@element-plus/icons-vue'
import { getGlobalStatistics, type GlobalStatisticsVO } from '@/api/statistics'
import { getAdminCallLogs, type CallLogRecord } from '@/api/callLog'

// 统计数据
const statisticsData = ref<GlobalStatisticsVO | null>(null)
const loadingStats = ref(false)

const statistics = ref([
  { title: '今日调用', value: '0', unit: '次', icon: DataLine, color: '#409eff', key: 'todayCalls' },
  { title: '总调用', value: '0', unit: '次', icon: DataLine, color: '#409eff', key: 'totalCalls' },
  { title: '总用户', value: '0', unit: '个', icon: TrendCharts, color: '#67c23a', key: 'totalUsers' },
  { title: '总接口', value: '0', unit: '个', icon: Document, color: '#f56c6c', key: 'totalApis' },
  { title: '平均耗时', value: '0', unit: 'ms', icon: Timer, color: '#e6a23c', key: 'avgResponseTime' }
])

// 调用趋势数据（仍保留示例柱状数据）
const trendData = ref([
  { date: '12-13', value: 1200 },
  { date: '12-14', value: 1350 },
  { date: '12-15', value: 1100 },
  { date: '12-16', value: 1450 },
  { date: '12-17', value: 1234 },
  { date: '12-18', value: 1600 },
  { date: '12-19', value: 1800 }
])

// 调用日志列表（改为真实接口）
const logList = ref<CallLogRecord[]>([])
const loading = ref(false)
const pagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const queryParams = ref({
  startTime: '',
  endTime: '',
  apiPath: '',
  status: undefined as number | undefined
})

// 获取统计数据
async function fetchStatistics() {
  loadingStats.value = true
  try {
    const res = await getGlobalStatistics(
      queryParams.value.startTime || undefined,
      queryParams.value.endTime || undefined
    )
    
    if (res.data) {
      statisticsData.value = res.data
      // 更新统计卡片数据
      statistics.value.forEach(stat => {
        if (stat.key && res.data) {
          const value = res.data[stat.key as keyof GlobalStatisticsVO]
          if (stat.key === 'avgResponseTime') {
            const num = typeof value === 'number' ? value : Number(value || 0)
            stat.value = num.toFixed(0)
          } else {
            stat.value = typeof value === 'number' ? value.toLocaleString() : String(value || 0)
          }
        }
      })
    }
  } catch (error: any) {
    console.error('获取统计数据失败:', error)
    if (!error.message?.includes('404')) {
      ElMessage.warning('获取统计数据失败：' + (error.message || '未知错误'))
    }
  } finally {
    loadingStats.value = false
  }
}

// 获取调用日志
async function fetchCallLogs(resetPage = false) {
  if (resetPage) {
    pagination.value.pageNum = 1
  }
  loading.value = true
  try {
    const res = await getAdminCallLogs({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      apiPath: queryParams.value.apiPath || undefined,
      status: queryParams.value.status
    })
    if (res.data) {
      logList.value = res.data.records || []
      pagination.value.total = res.data.total || 0
      pagination.value.pageSize = res.data.size || pagination.value.pageSize
      pagination.value.pageNum = res.data.current || pagination.value.pageNum
    }
  } catch (error: any) {
    console.error('获取调用日志失败:', error)
    ElMessage.warning('获取调用日志失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchStatistics()
  fetchCallLogs(true)
}

function handleReset() {
  queryParams.value = {
    startTime: '',
    endTime: '',
    apiPath: '',
    status: undefined
  }
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.value.pageNum = page
  fetchCallLogs()
}

function handleSizeChange(size: number) {
  pagination.value.pageSize = size
  pagination.value.pageNum = 1
  fetchCallLogs()
}

onMounted(() => {
  fetchStatistics()
  fetchCallLogs(true)
})
</script>

<template>
  <div class="statistics-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="stat in statistics" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-title">{{ stat.title }}</p>
              <p class="stat-value">
                {{ stat.value }}<span class="stat-unit">{{ stat.unit }}</span>
              </p>
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

    <!-- 调用趋势图表 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>全局调用趋势（最近7天）</span>
        </div>
      </template>
      <div class="chart-container">
        <div class="chart-placeholder">
          <el-icon :size="48" color="#c0c4cc"><TrendCharts /></el-icon>
          <p>调用趋势图表（待接入图表库）</p>
          <div class="mock-data">
            <div v-for="item in trendData" :key="item.date" class="bar-item">
              <div class="bar" :style="{ height: (item.value / 2000 * 200) + 'px' }"></div>
              <span class="bar-label">{{ item.date }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 调用日志 -->
    <el-card shadow="never" class="log-card">
      <template #header>
        <div class="card-header">
          <span>全局调用日志</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.startTime"
            type="datetime"
            placeholder="开始时间"
            style="width: 180px"
          />
          <span style="margin: 0 8px">-</span>
          <el-date-picker
            v-model="queryParams.endTime"
            type="datetime"
            placeholder="结束时间"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="API路径">
          <el-input v-model="queryParams.apiPath" placeholder="请输入" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="AccessKey">
          <el-input v-model="queryParams.accessKey" placeholder="请输入" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="200" />
            <el-option label="失败" :value="500" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table :data="logList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="apiPath" label="API路径" min-width="220" />
        <el-table-column prop="method" label="请求方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.method === 'GET' ? 'success' : row.method === 'POST' ? 'primary' : 'warning'">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessKey" label="AccessKey" min-width="160" />
        <el-table-column prop="status" label="状态码" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 200 ? 'success' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时" width="100">
          <template #default="{ row }">
            {{ row.costTime }}ms
          </template>
        </el-table-column>
        <el-table-column prop="callTime" label="调用时间" width="180" />
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.statistics-page {
  padding: 0;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-card,
.log-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-placeholder {
  text-align: center;
  color: #909399;
}

.chart-placeholder p {
  margin: 16px 0;
  font-size: 14px;
}

.mock-data {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  height: 200px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar {
  width: 40px;
  background: linear-gradient(180deg, #409eff 0%, #66b1ff 100%);
  border-radius: 4px 4px 0 0;
  margin-bottom: 8px;
  min-height: 20px;
}

.bar-label {
  font-size: 12px;
  color: #909399;
}

.search-form {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

