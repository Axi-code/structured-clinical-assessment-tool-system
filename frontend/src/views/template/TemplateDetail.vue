<template>
  <div class="template-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模板详情</span>
          <div>
            <el-button
              v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'"
              type="warning"
              @click="handleViewRules"
            >
              规则管理
            </el-button>
            <el-button v-if="userStore.isAdmin" type="success" @click="handleViewVersions">版本管理</el-button>
            <el-button @click="$router.back()">返回</el-button>
          </div>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板名称">{{ templateInfo.templateName }}</el-descriptions-item>
          <el-descriptions-item label="模板编码">{{ templateInfo.templateCode }}</el-descriptions-item>
          <el-descriptions-item label="评估类别">{{ templateInfo.category }}</el-descriptions-item>
          <el-descriptions-item label="版本号">{{ templateInfo.version }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="templateInfo.status === 1 ? 'success' : 'info'">
              {{ templateInfo.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ templateInfo.description }}</el-descriptions-item>
          <el-descriptions-item label="理论最低分">{{ templateInfo.minScore != null ? templateInfo.minScore : '-' }}</el-descriptions-item>
          <el-descriptions-item label="理论最高分">{{ templateInfo.maxScore != null ? templateInfo.maxScore : '-' }}</el-descriptions-item>
        </el-descriptions>
        
        <el-divider>评估字段</el-divider>
        
        <el-table :data="fields" border style="margin-top: 20px">
          <el-table-column prop="fieldLabel" label="字段标签" width="150" />
          <el-table-column prop="fieldCode" label="字段编码" width="150" />
          <el-table-column prop="fieldType" label="字段类型" width="120">
            <template #default="scope">
              <el-tag>{{ getFieldTypeName(scope.row.fieldType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="required" label="是否必填" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.required === 1 ? 'danger' : 'info'">
                {{ scope.row.required === 1 ? '必填' : '选填' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="options" label="选项值" />
          <el-table-column prop="validationRule" label="验证规则" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { templateApi } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const templateInfo = reactive({})
const fields = ref([])

const getFieldTypeName = (type) => {
  const typeMap = {
    TEXT: '文本',
    NUMBER: '数字',
    DATE: '日期',
    SELECT: '下拉选择',
    RADIO: '单选',
    CHECKBOX: '多选',
    TEXTAREA: '多行文本'
  }
  return typeMap[type] || type
}

const fetchData = async () => {
  loading.value = true
  try {
    const [templateRes, fieldsRes] = await Promise.all([
      templateApi.getTemplate(route.params.id),
      templateApi.getTemplateFields(route.params.id)
    ])
    Object.assign(templateInfo, templateRes.data)
    fields.value = fieldsRes.data || []
  } catch (error) {
    ElMessage.error('获取模板详情失败')
  } finally {
    loading.value = false
  }
}

const handleViewVersions = () => {
  router.push(`/template/versions/${templateInfo.templateCode}`)
}

const handleViewRules = () => {
  router.push(`/rule?templateId=${templateInfo.id}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.template-detail {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

