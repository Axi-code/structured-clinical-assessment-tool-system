<template>
  <div class="diagnosis-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>诊断管理</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="患者姓名">
          <el-input v-model="searchForm.name" placeholder="请输入患者姓名" clearable />
        </el-form-item>
        <el-form-item label="患者编号">
          <el-input v-model="searchForm.patientNo" placeholder="请输入患者编号" clearable />
        </el-form-item>
        <el-form-item label="当前诊断">
          <el-input v-model="searchForm.diagnosis" placeholder="请输入当前诊断" clearable />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="searchForm.departmentId" placeholder="请选择科室" clearable teleported style="width: 180px">
            <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="patientNo" label="患者编号" width="150" />
        <el-table-column prop="name" label="患者姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="departmentName" label="科室" width="120" />
        <el-table-column prop="diagnosisName" label="当前诊断" min-width="200">
          <template #default="scope">
            <el-tag :type="scope.row.diagnosisName ? 'success' : 'info'">
              {{ scope.row.diagnosisName || '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latestAssessorName" label="评估医生" width="120">
          <template #default="scope">
            {{ scope.row.latestAssessorName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="assessmentCount" label="评估次数" width="100" align="center">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.assessmentCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latestAssessmentTime" label="最近评估时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.latestAssessmentTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleViewDetail(scope.row)">诊断详情</el-button>
            <el-button link type="success" @click="handleViewHistory(scope.row)">评估历史</el-button>
            <el-button v-if="canCreateAssessment" link type="warning" @click="handleCreateAssessment(scope.row)">新建评估</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { patientApi, assessmentApi, departmentApi } from '../../api'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '../../utils/datetime'

const router = useRouter()
const userStore = useUserStore()
const canCreateAssessment = computed(() => userStore.isAdmin || userStore.userRole === 'DOCTOR')
const loading = ref(false)
const tableData = ref([])
const departmentList = ref([])

const searchForm = reactive({
  name: '',
  patientNo: '',
  diagnosis: '',
  departmentId: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const formatTime = (val) => formatDateTime(val) || val || ''

const fetchData = async () => {
  loading.value = true
  try {
    const res = await patientApi.getPatientList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    
    // 获取每个患者的评估历史统计
    const patients = res.data.records || []
    for (const patient of patients) {
      try {
        const historyRes = await assessmentApi.getPatientHistory(patient.id)
        const history = historyRes.data || []
        patient.assessmentCount = history.length
        if (history.length > 0) {
          // 按时间排序，获取最新的评估时间及评估医生
          const sortedHistory = history.sort((a, b) =>
            new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
          )
          const latest = sortedHistory[0]
          patient.latestAssessmentTime = formatTime(latest.createTime)
          patient.latestAssessorName = latest.assessorName || '-'
        } else {
          patient.latestAssessmentTime = '-'
          patient.latestAssessorName = '-'
        }
      } catch (error) {
        patient.assessmentCount = 0
        patient.latestAssessmentTime = '-'
        patient.latestAssessorName = '-'
      }
    }
    
    tableData.value = patients
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取诊断列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    patientNo: '',
    diagnosis: '',
    departmentId: null
  })
  handleSearch()
}

const handleViewDetail = (row) => {
  router.push(`/diagnosis/detail/${row.id}`)
}

const handleViewHistory = (row) => {
  router.push(`/assessment/history/${row.id}`)
}

const handleCreateAssessment = (row) => {
  router.push({
    path: '/assessment/create',
    query: { patientId: row.id }
  })
}

const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const loadDepartments = async () => {
  try {
    const res = await departmentApi.listAll()
    departmentList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载科室列表失败')
  }
}

onMounted(async () => {
  await loadDepartments()
  fetchData()
})
</script>

<style scoped>
.diagnosis-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
