<template>
  <div class="suggestion-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>诊疗建议历史记录</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-table :data="suggestions" border>
          <el-table-column prop="suggestionNo" label="建议编号" width="180" />
          <el-table-column prop="createTime" label="生成时间" width="180">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="generatorName" label="生成人" width="120" />
          <el-table-column prop="assessmentRecordId" label="评估记录ID" width="120" />
          <el-table-column label="建议内容" min-width="300">
            <template #default="scope">
              <div class="content-preview">
                {{ getContentPreview(scope.row.suggestionContent) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewDetail(scope.row)">查看详情</el-button>
              <el-button link type="success" @click="handleCopy(scope.row)">复制</el-button>
              <el-button
                v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'"
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
      </div>
    </el-card>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="诊疗建议详情" width="80%" :before-close="handleCloseDetail">
      <div v-if="currentSuggestion" class="suggestion-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="建议编号">{{ currentSuggestion.suggestionNo }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatTime(currentSuggestion.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="生成人">{{ currentSuggestion.generatorName }}</el-descriptions-item>
          <el-descriptions-item label="评估记录ID">{{ currentSuggestion.assessmentRecordId }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>建议内容</el-divider>
        <div class="suggestion-content" v-html="formatSuggestion(currentSuggestion.suggestionContent)"></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="handleCopy(currentSuggestion)">复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { treatmentSuggestionApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDateTime } from '../../utils/datetime'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const suggestions = ref([])
const detailVisible = ref(false)
const currentSuggestion = ref(null)

const patientId = computed(() => route.params.patientId)

const formatTime = (val) => formatDateTime(val) || val || ''

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await treatmentSuggestionApi.getSuggestionPage({
      patientId: patientId.value,
      current: pagination.current,
      size: pagination.size
    })
    if (res.code === 200) {
      suggestions.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error('获取诊疗建议列表失败')
    }
  } catch (error) {
    ElMessage.error('获取诊疗建议列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (row) => {
  currentSuggestion.value = row
  detailVisible.value = true
}

const handleCloseDetail = () => {
  detailVisible.value = false
  currentSuggestion.value = null
}

const handleCopy = (row) => {
  if (!row || !row.suggestionContent) return
  navigator.clipboard.writeText(row.suggestionContent).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该诊疗建议吗？', '提示', {
      type: 'warning'
    })
    await treatmentSuggestionApi.deleteSuggestion(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getContentPreview = (content) => {
  if (!content) return '-'
  // 显示前100个字符
  const text = content.replace(/\n/g, ' ').replace(/\*\*/g, '').replace(/\*/g, '')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

const formatSuggestion = (text) => {
  if (!text) return ''
  // 简单的Markdown格式化
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
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
.suggestion-history {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-preview {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.suggestion-detail {
  padding: 10px;
}

.suggestion-content {
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 60vh;
  overflow-y: auto;
  line-height: 1.8;
}

.suggestion-content h1 {
  font-size: 24px;
  margin-bottom: 16px;
  color: #303133;
}

.suggestion-content h2 {
  font-size: 20px;
  margin-top: 24px;
  margin-bottom: 12px;
  color: #606266;
}

.suggestion-content h3 {
  font-size: 16px;
  margin-top: 16px;
  margin-bottom: 8px;
  color: #909399;
}

.suggestion-content ul {
  margin: 12px 0;
  padding-left: 24px;
}

.suggestion-content li {
  margin: 8px 0;
}

.suggestion-content strong {
  color: #409eff;
  font-weight: 600;
}
</style>
