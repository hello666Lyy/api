<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Connection, Check, Plus } from '@element-plus/icons-vue'
import { getAvailableApis, applyApiPermission, type AvailableApiVO, type PageResult } from '@/api/apiMarket'

// 接口列表数据
const apiList = ref<AvailableApiVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryParams = ref({
  apiName: ''
})

// 详情弹窗
const detailDialogVisible = ref(false)
const currentApi = ref<AvailableApiVO | null>(null)

// 获取可用接口列表
async function fetchApiList() {
  loading.value = true
  try {
    const res = await getAvailableApis({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      apiName: queryParams.value.apiName || undefined
    })
    
    if (res.data) {
      apiList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('获取接口列表失败:', error)
    const errorMsg = error.message || '获取接口列表失败'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

// 申请开通接口权限
async function handleApplyPermission(api: AvailableApiVO) {
  try {
    await ElMessageBox.confirm(
      `确定要开通接口 "${api.apiName}" 的调用权限吗？`,
      '确认开通',
      {
        type: 'info'
      }
    )
    
    loading.value = true
    await applyApiPermission(api.id)
    
    ElMessage.success('接口权限开通成功！')
    // 重新加载列表
    await fetchApiList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('开通权限失败:', error)
      const errorMsg = error.message || '开通权限失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

// 查看详情
function handleViewDetail(api: AvailableApiVO) {
  currentApi.value = api
  detailDialogVisible.value = true
}

// 搜索
function handleSearch() {
  pageNum.value = 1
  fetchApiList()
}

// 重置
function handleReset() {
  queryParams.value.apiName = ''
  handleSearch()
}

// 分页变化
function handlePageChange(page: number) {
  pageNum.value = page
  fetchApiList()
}

// 获取请求方式标签类型
function getMethodTagType(method: string) {
  const typeMap: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return typeMap[method] || 'info'
}

// 初始化
onMounted(() => {
  fetchApiList()
})
</script>

<template>
  <div class="api-market-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>接口市场</h2>
        <p>浏览所有可用的业务接口，申请开通后即可使用SDK调用</p>
      </div>
      <el-icon :size="48" color="#409eff"><Connection /></el-icon>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="接口名称">
          <el-input 
            v-model="queryParams.apiName" 
            placeholder="请输入接口名称" 
            clearable 
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 接口列表 -->
    <div class="api-list">
      <el-card 
        v-for="api in apiList" 
        :key="api.id" 
        shadow="hover" 
        class="api-card"
      >
        <template #header>
          <div class="api-card-header">
            <div class="api-title">
              <el-tag :type="getMethodTagType(api.method)" size="large">{{ api.method }}</el-tag>
              <span class="api-name">{{ api.apiName }}</span>
              <el-tag v-if="api.hasPermission" type="success" size="small" style="margin-left: 8px">
                <el-icon><Check /></el-icon>
                已开通
              </el-tag>
            </div>
          </div>
        </template>

        <div class="api-content">
          <div class="api-path">
            <span class="path-label">接口路径：</span>
            <code class="path-value">{{ api.apiPath }}</code>
          </div>
          <div class="api-desc">
            <p>{{ api.apiDesc || '暂无描述' }}</p>
          </div>
          <div class="api-actions">
            <el-button 
              v-if="!api.hasPermission" 
              type="primary" 
              :icon="Plus" 
              @click="handleApplyPermission(api)"
            >
              申请开通
            </el-button>
            <el-button 
              type="info" 
              @click="handleViewDetail(api)"
            >
              查看文档
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && apiList.length === 0" description="暂无可用接口" />

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 接口详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="接口文档" width="800px" v-if="currentApi">
      <div class="api-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="接口名称">{{ currentApi.apiName }}</el-descriptions-item>
          <el-descriptions-item label="接口路径">
            <code>{{ currentApi.apiPath }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="请求方式">
            <el-tag :type="getMethodTagType(currentApi.method)">{{ currentApi.method }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接口描述">{{ currentApi.apiDesc || '暂无描述' }}</el-descriptions-item>
          <el-descriptions-item label="权限状态">
            <el-tag v-if="currentApi.hasPermission" type="success">已开通</el-tag>
            <el-tag v-else type="info">未开通</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentApi.hasPermission && currentApi.expireTime" label="过期时间">
            {{ new Date(currentApi.expireTime).toLocaleString() }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>使用说明</el-divider>
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <div>
              <p style="margin: 0 0 8px 0; font-weight: 600;">调用此API需要：</p>
              <ul style="margin: 0; padding-left: 20px;">
                <li>先开通该接口的调用权限</li>
                <li>使用SDK配置您的AK/SK</li>
                <li>在请求中携带签名信息</li>
                <li>签名算法：sign = HMAC-SHA256(accessKey + secretKey + timestamp + nonce)</li>
                <li>请求方式：{{ currentApi.method }}</li>
                <li>接口路径：{{ currentApi.apiPath }}</li>
              </ul>
            </div>
          </template>
        </el-alert>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.api-market-page {
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

.search-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.api-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.api-card {
  border-radius: 12px;
  transition: transform 0.2s;
}

.api-card:hover {
  transform: translateY(-2px);
}

.api-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.api-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.api-content {
  padding: 8px 0;
}

.api-path {
  margin-bottom: 12px;
}

.path-label {
  font-size: 13px;
  color: #909399;
  margin-right: 8px;
}

.path-value {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
}

.api-desc {
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.api-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.api-detail {
  padding: 8px 0;
}
</style>

