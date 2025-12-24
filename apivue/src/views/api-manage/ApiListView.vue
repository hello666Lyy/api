<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, View, Operation, Warning } from '@element-plus/icons-vue'
import { getApiList, addApi, updateApi, deleteApi, updateApiStatus, getApiDetail, type ApiInfoVO } from '@/api/apiInfo'

// 表格数据
const tableData = ref<ApiInfoVO[]>([])
const loading = ref(false)
const total = ref(0)

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  apiName: '',
  apiPath: '',
  status: undefined as number | undefined
})

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加 API')
const formLoading = ref(false)

// 表单数据
const form = reactive({
  id: undefined as number | undefined,
  apiName: '',
  apiPath: '',
  method: 'GET',
  apiDesc: '',
  status: 1
})

// 获取列表数据
async function fetchData() {
  loading.value = true
  try {
    // 默认只查询启用状态的数据（status=1），过滤掉逻辑删除的数据
    // 如果用户选择了状态筛选，则使用用户选择的值
    const statusFilter = queryParams.status !== undefined ? queryParams.status : 1
    
    const res = await getApiList({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      apiName: queryParams.apiName || undefined,
      apiPath: queryParams.apiPath || undefined,
      status: statusFilter
    })
    
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('获取API列表失败:', error)
    ElMessage.error(error.message || '获取API列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1
  fetchData()
}

// 重置
function handleReset() {
  queryParams.apiName = ''
  queryParams.apiPath = ''
  queryParams.status = undefined
  handleSearch()
}

// 分页变化
function handlePageChange(page: number) {
  queryParams.pageNum = page
  fetchData()
}

// 打开新增弹窗
function handleAdd() {
  dialogTitle.value = '添加 API'
  form.id = undefined
  form.apiName = ''
  form.apiPath = ''
  form.method = 'GET'
  form.apiDesc = ''
  form.status = 1
  dialogVisible.value = true
}

// 打开编辑弹窗
function handleEdit(row: ApiInfoVO) {
  dialogTitle.value = '编辑 API'
  form.id = row.id
  form.apiName = row.apiName
  form.apiPath = row.apiPath
  form.method = row.method
  form.apiDesc = row.apiDesc
  form.status = row.status
  dialogVisible.value = true
}

// 表单验证
function validateForm(): boolean {
  if (!form.apiName || form.apiName.trim() === '') {
    ElMessage.warning('请输入API名称')
    return false
  }
  if (!form.apiPath || form.apiPath.trim() === '') {
    ElMessage.warning('请输入API路径')
    return false
  }
  if (!form.apiPath.startsWith('/')) {
    ElMessage.warning('API路径必须以 / 开头')
    return false
  }
  if (!form.method) {
    ElMessage.warning('请选择请求方式')
    return false
  }
  return true
}

// 提交表单
async function handleSubmit() {
  if (!validateForm()) {
    return
  }
  
  formLoading.value = true
  try {
    if (form.id) {
      // 编辑
      await updateApi(form.id, {
        apiName: form.apiName.trim(),
        apiPath: form.apiPath.trim(),
        method: form.method,
        apiDesc: form.apiDesc?.trim() || '',
        status: form.status
      })
      ElMessage.success('修改成功')
    } else {
      // 新增
      await addApi({
        apiName: form.apiName.trim(),
        apiPath: form.apiPath.trim(),
        method: form.method,
        apiDesc: form.apiDesc?.trim() || '',
        status: form.status
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error: any) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

// 删除确认对话框
const deleteDialogVisible = ref(false)
const deleteConfirmName = ref('')
const deleteLoading = ref(false)
const deleteTarget = ref<ApiInfoVO | null>(null)
const deleteDetail = ref<ApiInfoVO | null>(null)

// 打开删除确认对话框
async function handleDelete(row: ApiInfoVO) {
  deleteTarget.value = row
  deleteConfirmName.value = ''
  deleteDialogVisible.value = true
  
  // 获取最新详情信息
  try {
    const res = await getApiDetail(row.id)
    if (res.data) {
      deleteDetail.value = res.data
    } else {
      deleteDetail.value = { ...row }
    }
  } catch (error: any) {
    console.error('获取详情失败:', error)
    // 如果获取详情失败，使用当前行数据
    deleteDetail.value = { ...row }
  }
}

// 确认删除
async function confirmDelete() {
  if (!deleteTarget.value || !deleteDetail.value) {
    return
  }
  
  // 验证输入的API名称
  if (deleteConfirmName.value.trim() !== deleteDetail.value.apiName.trim()) {
    ElMessage.warning('输入的API名称不匹配，请重新输入')
    return
  }
  
  deleteLoading.value = true
  try {
    await deleteApi(deleteTarget.value.id)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    deleteTarget.value = null
    deleteDetail.value = null
    deleteConfirmName.value = ''
    
    // 删除成功后，从列表中移除该项（逻辑删除后状态变为0，会被过滤掉）
    // 如果当前没有状态筛选，刷新列表会自动过滤掉已删除的数据
    // 如果当前有状态筛选，也需要刷新列表
    const deletedId = deleteTarget.value.id
    const index = tableData.value.findIndex(item => item.id === deletedId)
    if (index !== -1) {
      tableData.value.splice(index, 1)
      total.value = Math.max(0, total.value - 1)
    }
    
    // 如果当前页没有数据了，且不是第一页，则跳转到上一页
    if (tableData.value.length === 0 && queryParams.pageNum > 1) {
      queryParams.pageNum--
      fetchData()
    } else {
      // 刷新列表以确保数据同步
      fetchData()
    }
  } catch (error: any) {
    console.error('删除失败:', error)
    ElMessage.error(error.message || '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

// 取消删除
function cancelDelete() {
  deleteDialogVisible.value = false
  deleteTarget.value = null
  deleteDetail.value = null
  deleteConfirmName.value = ''
}

// 切换状态
async function handleStatusChange(row: ApiInfoVO) {
  const oldStatus = row.status
  const newStatus = row.status === 1 ? 0 : 1
  
  // 先更新UI（乐观更新）
  row.status = newStatus
  
  try {
    await updateApiStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch (error: any) {
    // 失败时回滚状态
    row.status = oldStatus
    console.error('状态更新失败:', error)
    ElMessage.error(error.message || '状态更新失败')
  }
}

// 详情查看
const detailDialogVisible = ref(false)
const currentDetail = ref<ApiInfoVO | null>(null)
const detailLoading = ref(false)

async function handleViewDetail(row: ApiInfoVO) {
  detailLoading.value = true
  detailDialogVisible.value = true
  
  try {
    const res = await getApiDetail(row.id)
    if (res.data) {
      currentDetail.value = res.data
    }
  } catch (error: any) {
    console.error('获取详情失败:', error)
    ElMessage.error(error.message || '获取详情失败')
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

// 批量操作
const selectedRows = ref<ApiInfoVO[]>([])
const batchDialogVisible = ref(false)
const batchAction = ref<'enable' | 'disable' | 'delete'>('enable')

function handleSelectionChange(selection: ApiInfoVO[]) {
  selectedRows.value = selection
}

function handleBatchAction(action: 'enable' | 'disable' | 'delete') {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要操作的数据')
    return
  }
  batchAction.value = action
  batchDialogVisible.value = true
}

async function confirmBatchAction() {
  const rows = [...selectedRows.value]
  const action = batchAction.value
  
  batchDialogVisible.value = false
  
  try {
    let successCount = 0
    let failCount = 0
    const deletedIds: number[] = []
    
    if (action === 'delete') {
      // 批量删除
      for (const row of rows) {
        try {
          await deleteApi(row.id)
          deletedIds.push(row.id)
          successCount++
        } catch (error: any) {
          failCount++
          console.error(`删除 ${row.apiName} 失败:`, error)
        }
      }
      
      // 从列表中移除已删除的项（逻辑删除后状态变为0，会被过滤掉）
      deletedIds.forEach(id => {
        const index = tableData.value.findIndex(item => item.id === id)
        if (index !== -1) {
          tableData.value.splice(index, 1)
          total.value = Math.max(0, total.value - 1)
        }
      })
      
      if (failCount === 0) {
        ElMessage.success(`已删除 ${successCount} 条数据`)
      } else {
        ElMessage.warning(`成功删除 ${successCount} 条，失败 ${failCount} 条`)
      }
    } else {
      // 批量启用/禁用
      const newStatus = action === 'enable' ? 1 : 0
      
      for (const row of rows) {
        try {
          await updateApiStatus(row.id, newStatus)
          row.status = newStatus
          successCount++
        } catch (error: any) {
          failCount++
          console.error(`${action === 'enable' ? '启用' : '禁用'} ${row.apiName} 失败:`, error)
        }
      }
      
      if (failCount === 0) {
        ElMessage.success(`已${action === 'enable' ? '启用' : '禁用'} ${successCount} 条数据`)
      } else {
        ElMessage.warning(`成功${action === 'enable' ? '启用' : '禁用'} ${successCount} 条，失败 ${failCount} 条`)
      }
    }
    
    selectedRows.value = []
    
    // 如果当前页没有数据了，且不是第一页，则跳转到上一页
    if (tableData.value.length === 0 && queryParams.pageNum > 1) {
      queryParams.pageNum--
      fetchData()
    } else {
      // 刷新列表以确保数据同步
      fetchData()
    }
  } catch (error: any) {
    console.error('批量操作失败:', error)
    ElMessage.error(error.message || '批量操作失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="api-list">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="API名称">
          <el-input v-model="queryParams.apiName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="API路径">
          <el-input v-model="queryParams.apiPath" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>API 列表</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <div class="batch-actions" v-if="selectedRows.length > 0">
        <el-alert
          :closable="false"
          type="info"
          show-icon
        >
          <template #default>
            <span>已选择 {{ selectedRows.length }} 项</span>
            <el-button type="primary" link @click="handleBatchAction('enable')">批量启用</el-button>
            <el-button type="warning" link @click="handleBatchAction('disable')">批量禁用</el-button>
            <el-button type="danger" link @click="handleBatchAction('delete')">批量删除</el-button>
          </template>
        </el-alert>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="apiName" label="API名称" min-width="150" />
        <el-table-column prop="apiPath" label="API路径" min-width="200" />
        <el-table-column prop="method" label="请求方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.method === 'GET' ? 'success' : row.method === 'POST' ? 'primary' : 'warning'">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">详情</el-button>
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          :page-size="queryParams.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="API名称" required>
          <el-input v-model="form.apiName" placeholder="请输入API名称" />
        </el-form-item>
        <el-form-item label="API路径" required>
          <el-input v-model="form.apiPath" placeholder="如：/api/user/list" />
        </el-form-item>
        <el-form-item label="请求方式" required>
          <el-select v-model="form.method" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.apiDesc" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="API详情" width="600px">
      <div v-loading="detailLoading">
        <el-descriptions :column="1" border v-if="currentDetail">
          <el-descriptions-item label="ID">{{ currentDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="API名称">{{ currentDetail.apiName }}</el-descriptions-item>
          <el-descriptions-item label="API路径">{{ currentDetail.apiPath }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">
            <el-tag :type="currentDetail.method === 'GET' ? 'success' : currentDetail.method === 'POST' ? 'primary' : 'warning'">
              {{ currentDetail.method }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentDetail.apiDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentDetail.status === 1 ? 'success' : 'danger'">
              {{ currentDetail.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间" v-if="currentDetail.updateTime">{{ currentDetail.updateTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 批量操作确认弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量操作确认" width="400px">
      <p>确定要{{ batchAction === 'enable' ? '启用' : batchAction === 'disable' ? '禁用' : '删除' }}选中的 {{ selectedRows.length }} 条数据吗？</p>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchAction">确定</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog 
      v-model="deleteDialogVisible" 
      title="删除确认" 
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div v-if="deleteDetail">
        <!-- 危险提示 -->
        <el-alert
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        >
          <template #title>
            <span style="font-weight: bold; font-size: 14px">警告：此操作不可恢复！</span>
          </template>
          <template #default>
            <div style="margin-top: 8px; line-height: 1.6">
              <p>删除API后，该接口将被禁用，相关调用将无法使用。</p>
              <p style="margin-top: 4px">请确认您要删除的API信息：</p>
            </div>
          </template>
        </el-alert>

        <!-- API详细信息 -->
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">
            <span style="font-weight: 500">{{ deleteDetail.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="API名称">
            <span style="font-weight: 500; color: #f56c6c">{{ deleteDetail.apiName }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="API路径">
            <span style="font-family: monospace; color: #909399">{{ deleteDetail.apiPath }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="请求方式">
            <el-tag :type="deleteDetail.method === 'GET' ? 'success' : deleteDetail.method === 'POST' ? 'primary' : 'warning'">
              {{ deleteDetail.method }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述">
            {{ deleteDetail.apiDesc || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="deleteDetail.status === 1 ? 'success' : 'danger'">
              {{ deleteDetail.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ deleteDetail.createTime }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 二次确认输入 -->
        <div style="margin-top: 24px; padding: 16px; background-color: #fef0f0; border-radius: 4px; border: 1px solid #fde2e2">
          <div style="margin-bottom: 12px; color: #f56c6c; font-weight: 500">
            <el-icon style="vertical-align: middle; margin-right: 4px"><Warning /></el-icon>
            为了确认您的操作，请输入API名称进行二次确认：
          </div>
          <el-input
            v-model="deleteConfirmName"
            placeholder="请输入API名称"
            clearable
            @keyup.enter="confirmDelete"
          />
          <div style="margin-top: 8px; font-size: 12px; color: #909399">
            请输入：<span style="font-weight: 500; color: #f56c6c">{{ deleteDetail.apiName }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="cancelDelete" :disabled="deleteLoading">取消</el-button>
        <el-button 
          type="danger" 
          @click="confirmDelete"
          :loading="deleteLoading"
          :disabled="!deleteDetail || deleteConfirmName.trim() !== deleteDetail?.apiName.trim()"
        >
          确认删除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-actions {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

