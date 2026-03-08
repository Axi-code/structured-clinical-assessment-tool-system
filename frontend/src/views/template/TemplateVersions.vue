<template>
  <div class="template-versions">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模板版本管理 - {{ templateCode }}</span>
          <div>
            <el-button @click="$router.back()">返回</el-button>
            <el-button v-if="userStore.isAdmin && currentTemplate" type="primary" @click="handleCreateVersion">创建新版本</el-button>
          </div>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-table :data="versions" border>
          <el-table-column prop="version" label="版本号" width="100" />
          <el-table-column prop="templateName" label="模板名称" width="200" />
          <el-table-column prop="category" label="评估类别" width="150" />
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
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180">
            <template #default="scope">
              {{ formatTime(scope.row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
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
                @click="handleCreateFromVersion(scope.row)"
              >
                基于此版本创建
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
    
    <!-- 创建新版本对话框 -->
    <el-dialog v-model="createVersionVisible" title="创建新版本" width="600px">
      <el-form :model="newVersionForm" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="newVersionForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="评估类别">
          <el-input v-model="newVersionForm.category" placeholder="请输入评估类别" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="newVersionForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="newVersionForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVersionVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCreateVersion" :loading="creating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { templateApi } from '../../api'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '../../utils/datetime'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const creating = ref(false)
const templateCode = ref('')
const versions = ref([])
const currentTemplate = ref(null)
const createVersionVisible = ref(false)
const newVersionForm = ref({
  templateName: '',
  category: '',
  description: '',
  remark: ''
})

const formatTime = (val) => formatDateTime(val) || val || ''

const fetchVersions = async () => {
  loading.value = true
  try {
    const res = await templateApi.getTemplateVersions(templateCode.value)
    versions.value = res.data || []
    if (versions.value.length > 0) {
      currentTemplate.value = versions.value[0]
    }
  } catch (error) {
    ElMessage.error('获取版本列表失败')
  } finally {
    loading.value = false
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

const handleViewDetail = (row) => {
  router.push(`/template/detail/${row.id}`)
}

const handleEdit = (row) => {
  router.push(`/template/edit/${row.id}`)
}

const handleCreateVersion = () => {
  if (currentTemplate.value) {
    newVersionForm.value = {
      templateName: currentTemplate.value.templateName,
      category: currentTemplate.value.category,
      description: currentTemplate.value.description,
      remark: `基于版本${currentTemplate.value.version}创建`
    }
  }
  createVersionVisible.value = true
}

const handleCreateFromVersion = (row) => {
  newVersionForm.value = {
    templateName: row.templateName,
    category: row.category,
    description: row.description,
    remark: `基于版本${row.version}创建`
  }
  currentTemplate.value = row
  createVersionVisible.value = true
}

const handleConfirmCreateVersion = async () => {
  if (!currentTemplate.value) {
    ElMessage.warning('请先选择基础版本')
    return
  }
  
  creating.value = true
  try {
    const res = await templateApi.createNewVersion(currentTemplate.value.id, newVersionForm.value)
    ElMessage.success('创建新版本成功')
    createVersionVisible.value = false
    await fetchVersions()
    // 跳转到新创建的版本编辑页面
    router.push(`/template/edit/${res.data.id}`)
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '创建新版本失败'
    ElMessage.error(msg)
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  templateCode.value = route.params.templateCode
  fetchVersions()
})
</script>

<style scoped>
.template-versions {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
