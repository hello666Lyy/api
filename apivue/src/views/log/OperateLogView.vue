<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Search, Refresh } from '@element-plus/icons-vue'
import { queryAkOperateLog, type AkOperateLogVO, type PageResult } from '@/api/ak'

// 操作日志列表
const logList = ref<AkOperateLogVO[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryParams = ref({
  targetAk: '',
  operateType: undefined as number | undefined,
  startTime: '',
  endTime: ''
})

// 操作类型选项
const operateTypeOptions = [
  { label: '全部', value: undefined },
  { label: '创建AK', value: 1 },
  { label: '启用AK', value: 2 },
  { label: '禁用AK', value: 3 },
  { label: '生成新AK/SK', value: 4 },
  { label: '设置权限', value: 5 },
  { label: '设置过期时间', value: 6 },
  { label: '逻辑删除', value: 7 }
]

async function handleSearch() {
  loading.value = true
  try {
    const res = await queryAkOperateLog({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      targetAk: queryParams.value.targetAk || undefined,
      operateType: queryParams.value.operateType,
      startTime: queryParams.value.startTime || undefined,
      endTime: queryParams.value.endTime || undefined
    })
    
    if (res.data) {
      logList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('查询日志失败:', error)
    const errorMsg = error.message || '查询失败'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.value = {
    targetAk: '',
    operateType: undefined,
    startTime: '',
    endTime: ''
  }
  handleSearch()
}

function handlePageChange(page: number) {
  pageNum.value = page
  handleSearch()
}

function getOperateTypeTag(operateType: number) {
  const typeMap: Record<number, { type: string; color: string }> = {
    1: { type: 'success', color: 'success' },
    2: { type: 'success', color: 'success' },
    3: { type: 'danger', color: 'danger' },
    4: { type: 'warning', color: 'warning' },
    5: { type: 'info', color: 'info' },
    6: { type: 'info', color: 'info' },
    7: { type: 'danger', color: 'danger' }
  }
  return typeMap[operateType] || { type: 'info', color: 'info' }
}

onMounted(() => {
  handleSearch()
})
</script>

<template>
  <div class="operate-log-page">
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-header">
          <span>AK 操作日志</span>
          <span class="header-desc">记录所有 AK 相关的操作历史，包括创建、启用、禁用、权限变更等</span>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="目标AK">
          <el-input v-model="queryParams.targetAk" placeholder="请输入AccessKey" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryParams.operateType" placeholder="全部" clearable style="width: 150px">
            <el-option
              v-for="option in operateTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.startTime"
            type="datetime"
            placeholder="开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
          />
          <span style="margin: 0 8px">-</span>
          <el-date-picker
            v-model="queryParams.endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="logList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="targetAk" label="目标AK" min-width="180" />
        <el-table-column prop="operateTypeText" label="操作类型" width="140">
          <template #default="{ row }">
            <el-tag :type="getOperateTypeTag(row.operateType).color">
              {{ row.operateTypeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
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
  </div>
</template>

<style scoped>
.operate-log-page {
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

.header-desc {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.search-form {
  margin-top: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

