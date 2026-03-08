<template>
  <div class="department-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>科室管理</span>
          <el-button type="primary" @click="handleAdd">新增科室</el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="科室名称">
          <el-input v-model="searchForm.name" placeholder="请输入科室名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="name" label="科室名称" width="150" />
        <el-table-column prop="code" label="科室编码" width="120" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
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
    
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增科室' : '编辑科室'" width="480px">
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="90px">
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="formModel.name" placeholder="请输入科室名称" />
        </el-form-item>
        <el-form-item label="科室编码" prop="code">
          <el-input v-model="formModel.code" placeholder="请输入科室编码（可选）" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formModel.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formModel.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { departmentApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('add')
const formRef = ref()

const searchForm = reactive({
  name: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const formModel = reactive({
  id: null,
  name: '',
  code: '',
  sortOrder: 0,
  remark: ''
})

const formRules = {
  name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await departmentApi.getPage({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取科室列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { name: '' })
  handleSearch()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  Object.assign(formModel, {
    id: null,
    name: '',
    code: '',
    sortOrder: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  Object.assign(formModel, { ...row })
  dialogVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (dialogMode.value === 'add') {
        await departmentApi.add(formModel)
        ElMessage.success('添加成功')
      } else {
        await departmentApi.update(formModel)
        ElMessage.success('更新成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      ElMessage.error(dialogMode.value === 'add' ? '添加失败' : '更新失败')
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该科室吗？', '提示', { type: 'warning' })
    await departmentApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSizeChange = () => fetchData()
const handleCurrentChange = () => fetchData()

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.department-list {
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
