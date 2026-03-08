<template>
  <div class="patient-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>患者列表</span>
          <el-button type="primary" @click="handleAdd">新增患者</el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="患者姓名">
          <el-input v-model="searchForm.name" placeholder="请输入患者姓名" clearable />
        </el-form-item>
        <el-form-item label="患者编号">
          <el-input v-model="searchForm.patientNo" placeholder="请输入患者编号" clearable />
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
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="departmentName" label="科室" width="120" />
        <el-table-column prop="diagnosisName" label="当前诊断">
          <template #default="scope">
            <el-tag :type="scope.row.diagnosisName ? 'success' : 'info'">
              {{ scope.row.diagnosisName || '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleViewHistory(scope.row)">评估历史</el-button>
            <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button
              v-if="userStore.isAdmin"
              link
              type="danger"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { patientApi, departmentApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const departmentList = ref([])

const searchForm = reactive({
  name: '',
  patientNo: '',
  departmentId: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await patientApi.getPatientList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取患者列表失败')
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
    departmentId: null
  })
  handleSearch()
}

const handleAdd = () => {
  router.push('/patient/add')
}

const handleEdit = (row) => {
  router.push(`/patient/edit/${row.id}`)
}

const handleViewHistory = (row) => {
  router.push(`/assessment/history/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该患者吗？', '提示', {
      type: 'warning'
    })
    await patientApi.deletePatient(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
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
.patient-list {
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

