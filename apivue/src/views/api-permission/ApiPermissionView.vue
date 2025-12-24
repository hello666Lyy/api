<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Key, Connection, Plus, Delete } from '@element-plus/icons-vue'
import { batchQueryAk, type PageResult } from '@/api/ak'
import { getUserApiPermissions, grantApiPermission, revokeApiPermission, type ApiPermissionVO } from '@/api/apiPermission'
import { getApiList, type ApiInfoVO } from '@/api/apiInfo'

interface SysUserAk {
  id: number
  username: string
  accessKey: string
  status: number
  permissionType: number
}

// 用户列表
const userList = ref<SysUserAk[]>([])
const userLoading = ref(false)
const userTotal = ref(0)
const userPageNum = ref(1)
const userPageSize = ref(10)

// 搜索条件
const searchForm = ref({
  targetAkLike: ''
})

// 当前选中的用户
const currentUser = ref<SysUserAk | null>(null)

// 权限管理弹窗
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)

// 接口列表（用于选择）
const apiList = ref<ApiInfoVO[]>([])
const apiListLoading = ref(false)
const selectedApiIds = ref<number[]>([])

// 用户已开通的接口权限列表
const userPermissions = ref<ApiPermissionVO[]>([])
const permissionTotal = ref(0)
const permissionPageNum = ref(1)
const permissionPageSize = ref(10)
const permissionListLoading = ref(false)

// 过期时间
const expireTime = ref<string | null>(null)

// 获取用户列表
async function fetchUserList() {
  userLoading.value = true
  try {
    const res = await batchQueryAk({
      pageNum: userPageNum.value,
      pageSize: userPageSize.value,
      targetAkLike: searchForm.value.targetAkLike || undefined,
      status: 1 // 只查询启用的AK
    })
    
    if (res.data) {
      userList.value = res.data.records || []
      userTotal.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('查询用户列表失败:', error)
    ElMessage.error(error.message || '查询失败')
  } finally {
    userLoading.value = false
  }
}

// 获取所有接口列表（用于选择）
async function fetchApiList() {
  apiListLoading.value = true
  try {
    const res = await getApiList({
      pageNum: 1,
      pageSize: 1000, // 获取所有接口
      status: 1 // 只查询启用的接口
    })
    
    if (res.data) {
      apiList.value = res.data.records || []
    }
  } catch (error: any) {
    console.error('获取接口列表失败:', error)
    ElMessage.error(error.message || '获取接口列表失败')
  } finally {
    apiListLoading.value = false
  }
}

// 获取用户已开通的接口权限
async function fetchUserPermissions() {
  if (!currentUser.value) return
  
  permissionListLoading.value = true
  try {
    const res = await getUserApiPermissions(
      currentUser.value.accessKey,
      permissionPageNum.value,
      permissionPageSize.value
    )
    
    if (res.data) {
      userPermissions.value = res.data.records || []
      permissionTotal.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('查询用户权限失败:', error)
    ElMessage.error(error.message || '查询失败')
  } finally {
    permissionListLoading.value = false
  }
}

// 打开权限管理弹窗
async function openPermissionDialog(user: SysUserAk) {
  currentUser.value = user
  selectedApiIds.value = []
  expireTime.value = null
  permissionDialogVisible.value = true
  
  // 加载接口列表和用户权限
  await Promise.all([
    fetchApiList(),
    fetchUserPermissions()
  ])
}

// 开通权限
async function handleGrantPermission() {
  if (!currentUser.value) return
  
  if (selectedApiIds.value.length === 0) {
    ElMessage.warning('请至少选择一个接口')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要为用户 ${currentUser.value.accessKey} 开通 ${selectedApiIds.value.length} 个接口的权限吗？`,
      '确认开通',
      {
        type: 'warning'
      }
    )
    
    permissionLoading.value = true
    await grantApiPermission(
      currentUser.value.accessKey,
      selectedApiIds.value,
      expireTime.value || undefined
    )
    
    ElMessage.success('权限开通成功')
    selectedApiIds.value = []
    expireTime.value = null
    await fetchUserPermissions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('开通权限失败:', error)
      ElMessage.error(error.message || '开通权限失败')
    }
  } finally {
    permissionLoading.value = false
  }
}

// 撤销权限
async function handleRevokePermission(permission: ApiPermissionVO) {
  if (!currentUser.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定要撤销接口 "${permission.apiName}" 的权限吗？`,
      '确认撤销',
      {
        type: 'warning'
      }
    )
    
    permissionLoading.value = true
    await revokeApiPermission(
      currentUser.value.accessKey,
      [permission.apiId]
    )
    
    ElMessage.success('权限撤销成功')
    await fetchUserPermissions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('撤销权限失败:', error)
      ElMessage.error(error.message || '撤销权限失败')
    }
  } finally {
    permissionLoading.value = false
  }
}

// 批量撤销权限
async function handleBatchRevoke() {
  if (!currentUser.value) return
  
  const selectedPermissions = userPermissions.value.filter(p => 
    selectedApiIds.value.includes(p.apiId)
  )
  
  if (selectedPermissions.length === 0) {
    ElMessage.warning('请先选择要撤销的权限')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要撤销 ${selectedPermissions.length} 个接口的权限吗？`,
      '确认批量撤销',
      {
        type: 'warning'
      }
    )
    
    permissionLoading.value = true
    await revokeApiPermission(
      currentUser.value.accessKey,
      selectedPermissions.map(p => p.apiId)
    )
    
    ElMessage.success('批量撤销成功')
    selectedApiIds.value = []
    await fetchUserPermissions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量撤销失败:', error)
      ElMessage.error(error.message || '批量撤销失败')
    }
  } finally {
    permissionLoading.value = false
  }
}

// 搜索
function handleSearch() {
  userPageNum.value = 1
  fetchUserList()
}

// 重置
function handleReset() {
  searchForm.value.targetAkLike = ''
  handleSearch()
}

// 分页变化
function handleUserPageChange(page: number) {
  userPageNum.value = page
  fetchUserList()
}

function handlePermissionPageChange(page: number) {
  permissionPageNum.value = page
  fetchUserPermissions()
}

// 初始化
onMounted(() => {
  fetchUserList()
})
</script>

<template>
  <div class="api-permission-page">
    <!-- 搜索卡片 -->
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-header">
          <div>
            <h3>接口权限管理</h3>
            <p>为用户开通或撤销业务接口的调用权限</p>
          </div>
          <el-icon><Key /></el-icon>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="用户AK">
          <el-input 
            v-model="searchForm.targetAkLike" 
            placeholder="模糊搜索AccessKey" 
            clearable 
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="userLoading" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <span>用户列表</span>
        </div>
      </template>

      <el-table 
        :data="userList" 
        v-loading="userLoading" 
        stripe
        @row-click="openPermissionDialog"
        style="cursor: pointer"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accessKey" label="AccessKey" min-width="250" />
        <el-table-column prop="permissionType" label="权限类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.permissionType === 3 ? 'danger' : row.permissionType === 2 ? 'warning' : 'info'">
              {{ row.permissionType === 3 ? '管理员' : row.permissionType === 2 ? '读写' : '只读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openPermissionDialog(row)">
              管理权限
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="userPageNum"
          :page-size="userPageSize"
          :total="userTotal"
          layout="total, prev, pager, next"
          @current-change="handleUserPageChange"
        />
      </div>
    </el-card>

    <!-- 权限管理弹窗 -->
    <el-dialog 
      v-model="permissionDialogVisible" 
      title="接口权限管理" 
      width="900px"
      :close-on-click-modal="false"
    >
      <div v-if="currentUser">
        <el-alert
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            <div>
              <p style="margin: 0; font-weight: 600;">当前用户：{{ currentUser.accessKey }}</p>
            </div>
          </template>
        </el-alert>

        <!-- 开通权限区域 -->
        <el-card shadow="never" style="margin-bottom: 20px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>开通接口权限</span>
              <el-button 
                type="primary" 
                :icon="Plus" 
                :loading="permissionLoading"
                @click="handleGrantPermission"
              >
                开通选中接口
              </el-button>
            </div>
          </template>

          <el-form label-width="100px">
            <el-form-item label="选择接口">
              <el-select
                v-model="selectedApiIds"
                multiple
                placeholder="请选择要开通的接口"
                style="width: 100%"
                :loading="apiListLoading"
              >
                <el-option
                  v-for="api in apiList"
                  :key="api.id"
                  :label="`${api.apiName} (${api.method} ${api.apiPath})`"
                  :value="api.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="expireTime"
                type="datetime"
                placeholder="选择过期时间（留空表示永不过期）"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
                clearable
              />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 已开通权限列表 -->
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>已开通的接口权限（共 {{ permissionTotal }} 个）</span>
              <el-button 
                type="danger" 
                :icon="Delete" 
                :loading="permissionLoading"
                @click="handleBatchRevoke"
              >
                批量撤销选中
              </el-button>
            </div>
          </template>

          <el-table 
            :data="userPermissions" 
            v-loading="permissionListLoading"
            stripe
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="apiName" label="接口名称" min-width="150" />
            <el-table-column prop="apiPath" label="接口路径" min-width="200" />
            <el-table-column prop="method" label="请求方式" width="100">
              <template #default="{ row }">
                <el-tag :type="row.method === 'GET' ? 'success' : row.method === 'POST' ? 'primary' : 'warning'">
                  {{ row.method }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="expireTime" label="过期时间" width="180">
              <template #default="{ row }">
                {{ row.expireTime ? new Date(row.expireTime).toLocaleString() : '永不过期' }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="开通时间" width="180">
              <template #default="{ row }">
                {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button 
                  type="danger" 
                  link 
                  :loading="permissionLoading"
                  @click="handleRevokePermission(row)"
                >
                  撤销
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination" style="margin-top: 16px;">
            <el-pagination
              v-model:current-page="permissionPageNum"
              :page-size="permissionPageSize"
              :total="permissionTotal"
              layout="total, prev, pager, next"
              @current-change="handlePermissionPageChange"
            />
          </div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.api-permission-page {
  padding: 0;
}

.search-card,
.table-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0 0 4px 0;
}

.card-header p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.search-form {
  margin-top: 8px;
}

.table-header {
  font-size: 16px;
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>


