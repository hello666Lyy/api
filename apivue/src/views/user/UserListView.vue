<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, User, Finished, Clock, Plus, Operation } from '@element-plus/icons-vue'
import { 
  batchQueryAk, 
  batchCreateAk, 
  batchOperateAkStatus, 
  manageAkExpireTime,
  manageAkPermission,
  checkAkValid,
  logicDeleteAk,
  type PageResult
} from '@/api/ak'

interface SysUserAk {
  id: number
  username: string
  accessKey: string
  status: number
  permissionType: number
  expireTime: string | null
  updateTime: string
}

// 初始化时加载数据
onMounted(() => {
  fetchData()
})

const loading = ref(false)
const tableData = ref<SysUserAk[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryForm = ref({
  targetAkLike: '',
  status: undefined as number | undefined,
  permissionType: undefined as number | undefined
})

// 过期时间弹窗
const expireDialogVisible = ref(false)
const currentAk = ref<SysUserAk | null>(null)
const expireValue = ref<string | null>(null)

async function fetchData() {
  loading.value = true
  try {
    const res = await batchQueryAk({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      targetAkLike: queryForm.value.targetAkLike || undefined,
      status: queryForm.value.status,
      permissionType: queryForm.value.permissionType
    })
    
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('查询失败:', error)
    const errorMsg = error.message || '查询失败'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchData()
}

function handleReset() {
  queryForm.value.targetAkLike = ''
  queryForm.value.status = undefined
  queryForm.value.permissionType = undefined
  handleSearch()
}

function handlePageChange(p: number) {
  pageNum.value = p
  fetchData()
}

// 启用 / 禁用
async function toggleStatus(row: SysUserAk) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${actionText} AK：${row.accessKey} 吗？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    // 使用批量操作接口（单个也可以）
    await batchOperateAkStatus(
      newStatus === 1 ? 2 : 3, // 2=启用, 3=禁用
      [row.accessKey]
    )
    ElMessage.success(`${actionText}成功`)
    fetchData()
  } catch (e: any) {
    console.error(e)
    ElMessage.error(`${actionText}失败: ${e.message || '未知错误'}`)
  }
}

// 打开过期时间弹窗
function openExpireDialog(row: SysUserAk) {
  currentAk.value = row
  expireValue.value = row.expireTime || null
  expireDialogVisible.value = true
}

async function submitExpire() {
  if (!currentAk.value) return
  try {
    await manageAkExpireTime(
      currentAk.value.accessKey,
      expireValue.value || undefined
    )
    
    ElMessage.success('设置过期时间成功')
    expireDialogVisible.value = false
    fetchData()
  } catch (e: any) {
    console.error(e)
    const errorMsg = e.message || '设置过期时间失败'
    ElMessage.error(errorMsg)
  }
}

// 批量创建AK
const batchCreateDialogVisible = ref(false)
const batchCreateForm = ref({
  count: 1,
  permissionType: 1
})
const batchCreateLoading = ref(false)
const createdAkList = ref<Array<{ accessKey: string; secretKey: string }>>([])
const showCreatedAkDialog = ref(false)

function openBatchCreateDialog() {
  batchCreateForm.value = { count: 1, permissionType: 1 }
  batchCreateDialogVisible.value = true
}

async function handleBatchCreate() {
  if (batchCreateForm.value.count < 1 || batchCreateForm.value.count > 50) {
    ElMessage.warning('创建数量必须在1-50之间')
    return
  }
  
  batchCreateLoading.value = true
  try {
    const res = await batchCreateAk(
      batchCreateForm.value.count,
      batchCreateForm.value.permissionType
    )
    
    if (res.data) {
      // 后端返回的是Map格式，需要转换为数组
      createdAkList.value = Object.entries(res.data).map(([accessKey, secretKey]) => ({
        accessKey,
        secretKey: secretKey as string
      }))
      
      batchCreateDialogVisible.value = false
      showCreatedAkDialog.value = true
      fetchData()
    }
  } catch (e: any) {
    console.error(e)
    const errorMsg = e.message || '批量创建失败'
    ElMessage.error(errorMsg)
  } finally {
    batchCreateLoading.value = false
  }
}

function copyToClipboard(text: string, label: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`${label}已复制到剪贴板`)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 批量操作
const selectedRows = ref<SysUserAk[]>([])
const batchOperateDialogVisible = ref(false)
const batchOperateType = ref<'enable' | 'disable'>('enable')

function handleSelectionChange(selection: SysUserAk[]) {
  selectedRows.value = selection
}

function handleBatchOperate(type: 'enable' | 'disable') {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要操作的数据')
    return
  }
  batchOperateType.value = type
  batchOperateDialogVisible.value = true
}

async function confirmBatchOperate() {
  try {
    const operateType = batchOperateType.value === 'enable' ? 2 : 3 // 2=启用, 3=禁用
    await batchOperateAkStatus(
      operateType,
      selectedRows.value.map(r => r.accessKey)
    )
    
    ElMessage.success(`已${batchOperateType.value === 'enable' ? '启用' : '禁用'} ${selectedRows.value.length} 个AK`)
    selectedRows.value = []
    batchOperateDialogVisible.value = false
    fetchData()
  } catch (e: any) {
    console.error(e)
    const errorMsg = e.message || '批量操作失败'
    ElMessage.error(errorMsg)
  }
}
</script>

<template>
  <div class="user-ak-page">
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="search-header">
          <div>
            <h3>用户与 AK 管理</h3>
            <p>支持按AK、状态和权限类型分页查询，并对 AK 进行批量操作和过期时间管理。</p>
          </div>
          <el-icon><User /></el-icon>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <!-- 注意：后端接口不支持按用户名搜索，只支持按AK搜索 -->
        <el-form-item label="AccessKey">
          <el-input v-model="queryForm.targetAkLike" placeholder="模糊搜索 AK" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限类型">
          <el-select
            v-model="queryForm.permissionType"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="只读" :value="1" />
            <el-option label="读写" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <div>
            <span>AK 分页列表</span>
            <span class="table-sub">展示符合条件的用户及其 AK 信息，可分页查看和管理。</span>
          </div>
          <el-button type="primary" :icon="Plus" @click="openBatchCreateDialog">批量创建AK</el-button>
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
            <el-button type="primary" link @click="handleBatchOperate('enable')">批量启用</el-button>
            <el-button type="warning" link @click="handleBatchOperate('disable')">批量禁用</el-button>
          </template>
        </el-alert>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        empty-text="暂无数据"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="accessKey" label="AccessKey" min-width="220" />
        <el-table-column prop="permissionType" label="权限类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.permissionType === 3 ? 'danger' : row.permissionType === 2 ? 'warning' : 'info'">
              {{ row.permissionType === 3 ? '管理员' : row.permissionType === 2 ? '读写' : '只读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" min-width="180">
          <template #default="{ row }">
            {{ row.expireTime ? new Date(row.expireTime).toLocaleString() : '永不过期' }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最后更新时间" min-width="180">
          <template #default="{ row }">
            {{ row.updateTime ? new Date(row.updateTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :icon="Finished"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              type="primary"
              link
              :icon="Clock"
              @click="openExpireDialog(row)"
            >
              设置过期
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 设置过期时间弹窗 -->
    <el-dialog v-model="expireDialogVisible" title="设置 AK 过期时间" width="420px">
      <div v-if="currentAk">
        <p class="dialog-ak">当前 AK：{{ currentAk.accessKey }}</p>
        <el-form label-width="90px">
          <el-form-item label="过期时间">
            <el-date-picker
              v-model="expireValue"
              type="datetime"
              placeholder="选择过期时间（留空表示永不过期）"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 260px"
              clearable
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="expireDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExpire">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量创建AK弹窗 -->
    <el-dialog v-model="batchCreateDialogVisible" title="批量创建AK" width="500px">
      <el-form :model="batchCreateForm" label-width="100px">
        <el-form-item label="创建数量" required>
          <el-input-number
            v-model="batchCreateForm.count"
            :min="1"
            :max="50"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            一次最多创建50个AK
          </div>
        </el-form-item>
        <el-form-item label="权限类型" required>
          <el-radio-group v-model="batchCreateForm.permissionType">
            <el-radio :value="1">只读</el-radio>
            <el-radio :value="2">读写</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchCreateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchCreateLoading" @click="handleBatchCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 显示创建的AK列表 -->
    <el-dialog v-model="showCreatedAkDialog" title="AK创建成功" width="700px">
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
      
      <el-table :data="createdAkList" border max-height="400">
        <el-table-column prop="accessKey" label="AccessKey" min-width="200">
          <template #default="{ row }">
            <div class="ak-display">
              <span class="ak-text">{{ row.accessKey }}</span>
              <el-button type="primary" link size="small" @click="copyToClipboard(row.accessKey, 'AccessKey')">
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="secretKey" label="SecretKey" min-width="200">
          <template #default="{ row }">
            <div class="ak-display">
              <span class="ak-text">{{ row.secretKey }}</span>
              <el-button type="primary" link size="small" @click="copyToClipboard(row.secretKey, 'SecretKey')">
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <el-button type="primary" @click="showCreatedAkDialog = false">我已保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量操作确认弹窗 -->
    <el-dialog v-model="batchOperateDialogVisible" title="批量操作确认" width="400px">
      <p>确定要{{ batchOperateType === 'enable' ? '启用' : '禁用' }}选中的 {{ selectedRows.length }} 个AK吗？</p>
      <template #footer>
        <el-button @click="batchOperateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchOperate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-ak-page {
  padding: 0;
}

.search-card,
.table-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-header h3 {
  margin: 0 0 4px;
}

.search-header p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.search-form {
  margin-top: 8px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.table-sub {
  font-size: 12px;
  color: #909399;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-ak {
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-actions {
  margin-bottom: 16px;
}

.ak-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ak-text {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}
</style>
