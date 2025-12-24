<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, Timer, TrendCharts, Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMyStatistics, type MyStatisticsVO } from '@/api/statistics'
import { getUserCallLogs, type CallLogRecord } from '@/api/callLog'

const userStore = useUserStore()

// 我的统计数据
const statisticsData = ref<MyStatisticsVO | null>(null)
const loadingStats = ref(false)

const myStatistics = ref([
  { title: '今日调用', value: '0', unit: '次', icon: DataLine, color: '#409eff', key: 'todayCalls' },
  { title: '总调用', value: '0', unit: '次', icon: DataLine, color: '#409eff', key: 'totalCalls' },
  { title: '成功调用', value: '0', unit: '次', icon: TrendCharts, color: '#67c23a', key: 'successCalls' },
  { title: '失败调用', value: '0', unit: '次', icon: Document, color: '#f56c6c', key: 'failedCalls' }
])

// 调用趋势数据（占位示例）
const trendData = ref([
  { date: '12-13', value: 120 },
  { date: '12-14', value: 135 },
  { date: '12-15', value: 110 },
  { date: '12-16', value: 145 },
  { date: '12-17', value: 123 },
  { date: '12-18', value: 160 },
  { date: '12-19', value: 156 }
])

// 我的调用日志列表（真实接口返回）
const myLogList = ref<CallLogRecord[]>([])
const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})

const loading = ref(false)
const queryParams = ref({
  startTime: '' as any,
  endTime: '' as any,
  apiPath: '',
  status: undefined as number | undefined
})

// 获取统计数据
async function fetchStatistics() {
  loadingStats.value = true
  try {
    const res = await getMyStatistics(
      queryParams.value.startTime || undefined,
      queryParams.value.endTime || undefined
    )
    
    if (res.data) {
      statisticsData.value = res.data
      // 更新统计卡片数据
      myStatistics.value.forEach(stat => {
        if (stat.key && res.data) {
          stat.value = String(res.data[stat.key as keyof MyStatisticsVO] || 0)
        }
      })
    }
  } catch (error: any) {
    console.error('获取统计数据失败:', error)
    // 如果接口未实现，使用默认值
    if (error.message?.includes('404') || error.message?.includes('不存在')) {
      // 保持默认值，不显示错误
    } else {
      ElMessage.warning('获取统计数据失败：' + (error.message || '未知错误'))
    }
  } finally {
    loadingStats.value = false
  }
}

function formatDateTime(val: any) {
  if (!val) return ''
  if (typeof val === 'string') return val
  const d = val as Date
  const pad = (n: number) => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function handleSearch(page?: number | MouseEvent) {
  loading.value = true
  try {
    if (typeof page === 'number') {
      pagination.value.current = page
    } else {
      // 按钮点击等情况重置到第一页
      pagination.value.current = 1
    }
    const params: any = {
      pageNum: pagination.value.current,
      pageSize: pagination.value.size,
      apiPath: queryParams.value.apiPath || undefined,
      status: queryParams.value.status,
      startTime: formatDateTime(queryParams.value.startTime) || undefined,
      endTime: formatDateTime(queryParams.value.endTime) || undefined
    }
    const res = await getUserCallLogs(params)
    myLogList.value = res.data.records || []
    pagination.value.total = res.data.total || 0
  } catch (error: any) {
    console.error('获取调用日志失败:', error)
    ElMessage.warning('获取调用日志失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.value = {
    startTime: '',
    endTime: '',
    apiPath: '',
    status: undefined
  }
  pagination.value.current = 1
  handleSearch()
}

onMounted(() => {
  fetchStatistics()
  handleSearch()
})
</script>

<template>
  <div class="my-statistics-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>我的调用统计</h2>
        <p>查看我的API调用记录和统计数据</p>
      </div>
      <el-icon :size="48" color="#409eff"><DataLine /></el-icon>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="stat in myStatistics" :key="stat.title">
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
          <span>我的调用趋势（最近7天）</span>
        </div>
      </template>
      <div class="chart-container">
        <div class="chart-placeholder">
          <el-icon :size="48" color="#c0c4cc"><TrendCharts /></el-icon>
          <p>调用趋势图表（待接入图表库）</p>
          <div class="mock-data">
            <div v-for="item in trendData" :key="item.date" class="bar-item">
              <div class="bar" :style="{ height: (item.value / 200 * 200) + 'px' }"></div>
              <span class="bar-label">{{ item.date }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 我的调用日志 -->
    <el-card shadow="never" class="log-card">
      <template #header>
        <div class="card-header">
          <span>我的调用日志</span>
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
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => handleSearch(1)" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table :data="myLogList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="apiPath" label="API路径" min-width="220" />
        <el-table-column prop="method" label="请求方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.method === 'GET' ? 'success' : row.method === 'POST' ? 'primary' : 'warning'">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
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
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.my-statistics-page {
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

