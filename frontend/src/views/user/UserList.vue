<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable teleported>
            <el-option label="管理员" value="ADMIN" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="护士" value="NURSE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">
              {{ getRoleName(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="科室" width="150" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="email" label="邮箱" />
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
  </div>
  <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增用户' : '编辑用户'" width="520px">
    <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="formModel.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="formModel.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="formModel.role" placeholder="请选择角色" style="width: 100%">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="护士" value="NURSE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="dialogMode === 'add'" label="密码" prop="password">
        <el-input v-model="formModel.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item label="科室" prop="departmentId">
        <el-select v-model="formModel.departmentId" placeholder="请选择科室" style="width: 100%" teleported>
          <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="formModel.phone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="formModel.email" placeholder="请输入邮箱" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { userApi, departmentApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const departmentList = ref([])
const tableData = ref([])

const searchForm = reactive({
  username: '',
  role: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getRoleName = (role) => {
  const roleMap = {
    ADMIN: '管理员',
    DOCTOR: '医生',
    NURSE: '护士'
  }
  return roleMap[role] || role
}

const getRoleTagType = (role) => {
  const typeMap = {
    ADMIN: 'danger',
    DOCTOR: 'success',
    NURSE: 'info'
  }
  return typeMap[role] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getUserList({
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取用户列表失败')
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
    username: '',
    role: ''
  })
  handleSearch()
}

const dialogVisible = ref(false)
const dialogMode = ref('add') // add | edit
const formRef = ref()
const formModel = reactive({
  id: null,
  username: '',
  realName: '',
  role: '',
  departmentId: null,
  phone: '',
  email: '',
  password: ''
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur',
      validator: (_, value, callback) => {
        if (dialogMode.value === 'add' && !value) {
          callback(new Error('请输入密码'))
        } else {
          callback()
        }
      }
    }
  ]
}

const resetForm = () => {
  Object.assign(formModel, {
    id: null,
    username: '',
    realName: '',
    role: '',
    departmentId: null,
    phone: '',
    email: '',
    password: ''
  })
  formRef.value && formRef.value.clearValidate()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(formModel, row, { password: '' })
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (dialogMode.value === 'add') {
        await userApi.addUser({ ...formModel })
        ElMessage.success('添加成功')
      } else {
        const payload = { ...formModel }
        delete payload.password // 不在编辑时提交密码
        await userApi.updateUser(payload)
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
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      type: 'warning'
    })
    await userApi.deleteUser(row.id)
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
.user-list {
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

