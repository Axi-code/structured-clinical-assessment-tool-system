<template>
  <div class="template-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评估模板列表</span>
          <el-button v-if="userStore.isAdmin" type="primary" @click="handleAdd">新增模板</el-button>
        </div>
      </template>
      
      <el-form :model="searchForm" class="search-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="模板名称">
              <el-input v-model="searchForm.templateName" placeholder="请输入模板名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="评估类别">
              <el-input v-model="searchForm.category" placeholder="请输入评估类别" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable teleported style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="24" :lg="6">
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="templateName" label="模板名称" width="200" />
        <el-table-column prop="templateCode" label="模板编码" width="150" />
        <el-table-column prop="category" label="评估类别" width="150" />
        <el-table-column prop="version" label="版本号" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-switch
              v-if="userStore.isAdmin"
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(scope.row)"
            />
            <el-tag v-else :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleViewDetail(scope.row)">查看详情</el-button>
            <el-button
              v-if="userStore.isAdmin"
              link
              type="primary"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.isAdmin"
              link
              type="success"
              @click="handleViewVersions(scope.row)"
            >
              版本
            </el-button>
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
import { templateApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  templateName: '',
  category: '',
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await templateApi.getTemplateList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取模板列表失败')
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
    templateName: '',
    category: '',
    status: null
  })
  handleSearch()
}

const handleAdd = () => {
  router.push('/template/add')
}

const handleViewDetail = (row) => {
  router.push(`/template/detail/${row.id}`)
}

const handleEdit = (row) => {
  router.push(`/template/edit/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', {
      type: 'warning'
    })
    await templateApi.deleteTemplate(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (row) => {
  try {
    await templateApi.updateTemplateStatus(row.id, row.status)
    ElMessage.success(row.status === 1 ? '启用成功' : '停用成功')
  } catch (error) {
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleViewVersions = (row) => {
  router.push(`/template/versions/${row.templateCode}`)
}

const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.template-list {
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

.search-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

