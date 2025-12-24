<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Document, Connection } from '@element-plus/icons-vue'
import { getMyApiPermissions, type ApiPermissionVO } from '@/api/apiPermission'

// API权限列表数据（用户已开通的接口）
const apiList = ref<ApiPermissionVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryParams = ref({
  apiName: '',
  apiPath: ''
})

// 详情弹窗
const detailDialogVisible = ref(false)
const currentApi = ref<ApiPermissionVO | null>(null)
const detailLoading = ref(false)

// 获取我的API权限列表
async function fetchApiList() {
  loading.value = true
  try {
    const res = await getMyApiPermissions(
      pageNum.value,
      pageSize.value
    )
    
    let permissions = res.data?.records || []
    
    // 客户端过滤（如果后端不支持，前端过滤）
    if (queryParams.value.apiName) {
      permissions = permissions.filter(p => 
        p.apiName.toLowerCase().includes(queryParams.value.apiName.toLowerCase())
      )
    }
    if (queryParams.value.apiPath) {
      permissions = permissions.filter(p => 
        p.apiPath.toLowerCase().includes(queryParams.value.apiPath.toLowerCase())
      )
    }
    
    apiList.value = permissions
    total.value = res.data?.total || 0
  } catch (error: any) {
    console.error('获取我的API权限列表失败:', error)
    const errorMsg = error.message || ''
    
    // 如果接口未实现，提示用户
    if (errorMsg.includes('404') || errorMsg.includes('不存在')) {
      ElMessage.warning({
        message: '接口权限查询功能暂未实现，请联系管理员开通接口权限',
        duration: 5000
      })
    } else {
      ElMessage.error('获取API列表失败：' + errorMsg)
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchApiList()
}

function handleReset() {
  queryParams.value = {
    apiName: '',
    apiPath: ''
  }
  handleSearch()
}

// 分页变化
function handlePageChange(page: number) {
  pageNum.value = page
  fetchApiList()
}

// 查看详情
function handleViewDetail(api: ApiPermissionVO) {
  currentApi.value = api
  detailDialogVisible.value = true
}

function getMethodTagType(method: string) {
  const typeMap: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return typeMap[method] || 'info'
}

onMounted(() => {
  fetchApiList()
})
</script>

<template>
  <div class="api-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>我的 API 接口</h2>
        <p>查看您已开通权限的API接口，了解接口的使用方法和调用示例</p>
      </div>
      <el-icon :size="48" color="#409eff"><Connection /></el-icon>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="API名称">
          <el-input v-model="queryParams.apiName" placeholder="请输入API名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="API路径">
          <el-input v-model="queryParams.apiPath" placeholder="请输入API路径" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- API列表 -->
    <div v-loading="loading" class="api-cards">
      <el-empty v-if="!loading && apiList.length === 0" description="暂无API数据" />
      <el-card
        v-for="api in apiList"
        :key="api.id"
        shadow="hover"
        class="api-card"
        :class="{ disabled: api.status === 0 }"
      >
        <template #header>
          <div class="api-card-header">
            <div class="api-title">
              <el-tag :type="getMethodTagType(api.method)" size="large">{{ api.method }}</el-tag>
              <span class="api-name">{{ api.apiName }}</span>
            </div>
            <el-tag type="success" size="small">已开通</el-tag>
          </div>
        </template>

        <div class="api-content">
          <div class="api-path">
            <span class="path-label">接口路径：</span>
            <code class="path-value">{{ api.apiPath }}</code>
          </div>
          <div class="api-desc" v-if="api.expireTime">
            <p style="color: #909399; font-size: 12px;">
              过期时间：{{ new Date(api.expireTime).toLocaleString() }}
            </p>
          </div>
          <div class="api-actions">
            <el-button type="primary" :icon="Document" @click="handleViewDetail(api)">查看文档</el-button>
          </div>
        </div>
      </el-card>
    </div>

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

    <!-- API详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="API 文档" width="800px" v-if="currentApi">
      <div v-loading="detailLoading" class="api-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ currentApi.id }}</el-descriptions-item>
          <el-descriptions-item label="API名称">{{ currentApi.apiName }}</el-descriptions-item>
          <el-descriptions-item label="接口路径">
            <code>{{ currentApi.apiPath }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="请求方式">
            <el-tag :type="getMethodTagType(currentApi.method)">{{ currentApi.method }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentApi.status === 1 ? 'success' : 'danger'">
              {{ currentApi.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限状态">
            <el-tag type="success">已开通</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="过期时间">
            {{ currentApi.expireTime ? new Date(currentApi.expireTime).toLocaleString() : '永不过期' }}
          </el-descriptions-item>
          <el-descriptions-item label="开通时间">
            {{ currentApi.createTime ? new Date(currentApi.createTime).toLocaleString() : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>使用说明</el-divider>
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <div>
              <p style="margin: 0 0 8px 0; font-weight: 600;">调用此API需要：</p>
              <ul style="margin: 0; padding-left: 20px;">
                <li>在请求头或参数中携带 AccessKey 和签名信息</li>
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
.api-list-page {
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

.api-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.api-card {
  border-radius: 12px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.api-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.api-card.disabled {
  opacity: 0.6;
}

.api-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.api-title {
  display: flex;
  align-items: center;
  gap: 12px;
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
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.path-label {
  font-size: 13px;
  color: #909399;
  margin-right: 8px;
}

.path-value {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #409eff;
  background: #fff;
  padding: 2px 6px;
  border-radius: 4px;
}

.api-desc {
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.api-desc p {
  margin: 0;
}

.api-actions {
  display: flex;
  justify-content: flex-end;
}

.api-detail {
  padding: 8px 0;
}

.api-detail code {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #409eff;
}

.response-example {
  background: #f5f7fa;
}

.response-example pre {
  margin: 0;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  overflow-x: auto;
}

.response-example code {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

