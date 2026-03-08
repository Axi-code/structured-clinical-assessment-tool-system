<template>
  <div class="operation-log-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <span class="desc">记录患者、评估等关键操作，便于审计</span>
        </div>
      </template>

      <div class="filters">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
        <el-select v-model="query.module" placeholder="模块" clearable style="width: 140px">
          <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
        <el-select v-model="query.action" placeholder="动作" clearable style="width: 140px">
          <el-option v-for="a in actionOptions" :key="a.value" :label="a.label" :value="a.value" />
        </el-select>
        <el-input
          v-model="query.username"
          placeholder="操作人用户名"
          style="width: 180px"
          clearable
        />
        <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="logs" border stripe v-loading="loading">
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作人" min-width="150">
          <template #default="{ row }">
            <div>{{ row.realName || row.username }}</div>
            <div class="sub">{{ row.username }} · {{ row.role }}</div>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="140">
          <template #default="{ row }">
            {{ getModuleLabel(row.module) }}
          </template>
        </el-table-column>
        <el-table-column label="动作" width="120">
          <template #default="{ row }">
            {{ getActionLabel(row.action) }}
          </template>
        </el-table-column>
        <el-table-column label="目标" min-width="150">
          <template #default="{ row }">
            <div>{{ getModuleLabel(row.targetType) || '-' }}</div>
            <div class="sub">ID: {{ row.targetId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="260" />
        <el-table-column prop="ip" label="IP" width="140" />
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { operationLogApi } from '../../api'
import { formatDateTime } from '../../utils/datetime'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const dateRange = ref([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const query = reactive({
  module: '',
  action: '',
  username: ''
})

// 模块映射
const moduleMap = {
  'PATIENT': '患者',
  'ASSESSMENT_RECORD': '评估记录',
  'ASSESSMENT_TEMPLATE': '评估模板',
  'ASSESSMENT_TEMPLATE_FIELD': '评估模板字段',
  'ASSESSMENT_RULE': '评估规则',
  'REPORT': '报告',
  'REPORT_TEMPLATE': '报告模板',
  'TREATMENT_SUGGESTION': '诊疗建议',
  'USER': '用户'
}

// 动作映射
const actionMap = {
  'CREATE': '创建',
  'SAVE': '保存',
  'SUBMIT': '提交',
  'UPDATE': '更新',
  'DELETE': '删除',
  'COMPLETE': '完成'
}

const moduleOptions = [
  { label: '患者', value: 'PATIENT' },
  { label: '评估记录', value: 'ASSESSMENT_RECORD' },
  { label: '评估模板', value: 'ASSESSMENT_TEMPLATE' },
  { label: '评估模板字段', value: 'ASSESSMENT_TEMPLATE_FIELD' },
  { label: '评估规则', value: 'ASSESSMENT_RULE' },
  { label: '报告', value: 'REPORT' },
  { label: '报告模板', value: 'REPORT_TEMPLATE' },
  { label: '诊疗建议', value: 'TREATMENT_SUGGESTION' },
  { label: '用户', value: 'USER' }
]

const actionOptions = [
  { label: '创建', value: 'CREATE' },
  { label: '保存', value: 'SAVE' },
  { label: '提交', value: 'SUBMIT' },
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '完成', value: 'COMPLETE' }
]

// 获取模块中文标签
const getModuleLabel = (module) => {
  return moduleMap[module] || module || '-'
}

// 获取动作中文标签
const getActionLabel = (action) => {
  return actionMap[action] || action || '-'
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  dateRange.value = []
  query.module = ''
  query.action = ''
  query.username = ''
  pagination.current = 1
  loadData()
}

const handlePageChange = (page) => {
  pagination.current = page
  loadData()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      module: query.module || undefined,
      action: query.action || undefined,
      username: query.username || undefined,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    }
    const res = await operationLogApi.getLogPage(params)
    const data = res.data || {}
    logs.value = data.records || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('加载操作日志失败', error)
    ElMessage.error('加载操作日志失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.operation-log-page {
  padding: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.desc {
  color: #909399;
  font-size: 12px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
