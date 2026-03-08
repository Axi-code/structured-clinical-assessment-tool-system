<template>
  <div class="assessment-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评估历史记录</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-table :data="records" border>
          <el-table-column prop="recordNo" label="记录编号" width="150" />
          <el-table-column prop="createTime" label="评估时间" width="180">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="assessorName" label="评估人" width="120" />
          <el-table-column prop="department" label="科室" width="120" />
          <el-table-column prop="totalScore" label="总分" width="100" />
          <el-table-column prop="assessmentResult" label="评估结果" width="150" />
          <el-table-column prop="riskLevel" label="风险等级" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.riskLevel" :type="getRiskTagType(scope.row.riskLevel)">
                {{ scope.row.riskLevel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="450" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewDetail(scope.row)">查看详情</el-button>
              <el-button link type="warning" @click="handleGenerateSuggestion(scope.row)">生成建议</el-button>
              <el-button link type="info" @click="handlePreviewPdf(scope.row)">预览PDF</el-button>
              <el-button link type="success" @click="handleExportPdf(scope.row)">导出PDF</el-button>
              <el-button link type="success" @click="handleExportWord(scope.row)">导出Word</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-card v-if="selectedRecords.length > 0" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>记录对比</span>
              <el-button @click="selectedRecords = []">清空</el-button>
            </div>
          </template>
          <el-table :data="comparisonData" border>
            <el-table-column prop="field" label="字段" width="150" />
            <el-table-column
              v-for="record in selectedRecords"
              :key="record.id"
              :label="record.recordNo"
              :prop="`record_${record.id}`"
            />
          </el-table>
        </el-card>
      </div>
    </el-card>
    
    <el-dialog v-model="detailVisible" title="评估详情" width="80%">
      <div v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录编号">{{ currentRecord.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="评估时间">{{ formatTime(currentRecord.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="评估人">{{ currentRecord.assessorName }}</el-descriptions-item>
          <el-descriptions-item label="科室">{{ currentRecord.department }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ currentRecord.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="评估结果">{{ currentRecord.assessmentResult }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">{{ currentRecord.riskLevel }}</el-descriptions-item>
          <el-descriptions-item label="风险提示" :span="2">{{ currentRecord.riskTips }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>评估数据</el-divider>
        <el-descriptions :column="1" border>
          <el-descriptions-item
            v-for="item in displayAssessmentItems"
            :key="item.key"
            :label="item.label"
          >
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
    
    <!-- PDF预览对话框 -->
    <el-dialog v-model="previewVisible" title="报告预览" width="90%" :before-close="handleClosePreview">
      <div v-loading="previewLoading" style="height: 80vh">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          style="width: 100%; height: 100%; border: none"
        />
        <el-empty v-else description="暂无预览内容" />
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleDownloadFromPreview">下载</el-button>
      </template>
    </el-dialog>
    
    <!-- 诊疗建议对话框 -->
    <el-dialog v-model="suggestionVisible" title="AI诊疗建议" width="80%" :before-close="handleCloseSuggestion">
      <div v-loading="suggestionLoading">
        <div v-if="treatmentSuggestion" class="suggestion-content">
          <div v-html="formatSuggestion(treatmentSuggestion)"></div>
        </div>
        <el-empty v-else description="暂无诊疗建议" />
      </div>
      <template #footer>
        <el-button @click="suggestionVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleRegenerateSuggestion">重新生成</el-button>
        <el-button type="success" @click="handleCopySuggestion">复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { assessmentApi, reportApi, treatmentSuggestionApi, templateApi } from '../../api'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '../../utils/datetime'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const selectedRecords = ref([])
const detailVisible = ref(false)
const currentRecord = ref(null)
const assessmentData = ref({})
const templateFields = ref([])
const fieldLabelMap = ref({})
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const previewRecordId = ref(null)
const previewType = ref('PDF')
const suggestionVisible = ref(false)
const suggestionLoading = ref(false)
const treatmentSuggestion = ref('')
const suggestionRecordId = ref(null)

const comparisonData = computed(() => {
  if (selectedRecords.value.length === 0) return []
  // 这里可以实现对比数据的格式化
  return []
})

const displayAssessmentItems = computed(() => {
  const data = assessmentData.value || {}
  const fields = templateFields.value || []
  const map = fieldLabelMap.value || {}

  const items = []

  // 按模板字段顺序展示（优先中文fieldLabel）
  for (const f of fields) {
    const keys = [f.fieldName, f.fieldCode].filter(Boolean)
    if (keys.length === 0) continue
    const matchedKey = keys.find(k => Object.prototype.hasOwnProperty.call(data, k))
    if (matchedKey) {
      items.push({
        key: matchedKey,
        label: f.fieldLabel || map[matchedKey] || matchedKey,
        value: data[matchedKey]
      })
    }
  }

  // 兜底：模板字段里没有的key也展示出来
  for (const key of Object.keys(data)) {
    if (items.some(i => i.key === key)) continue
    items.push({
      key,
      label: map[key] || key,
      value: data[key]
    })
  }

  return items
})

const getRiskTagType = (riskLevel) => {
  const typeMap = {
    '低风险': 'success',
    '中风险': 'warning',
    '高风险': 'danger'
  }
  return typeMap[riskLevel] || 'info'
}

const formatTime = (val) => formatDateTime(val) || val || ''

const fetchData = async () => {
  loading.value = true
  try {
    const res = await assessmentApi.getPatientHistory(route.params.patientId)
    records.value = res.data || []
  } catch (error) {
    ElMessage.error('获取评估历史失败')
  } finally {
    loading.value = false
  }
}

const handleViewDetail = async (row) => {
  currentRecord.value = row
  try {
    assessmentData.value = JSON.parse(row.assessmentData || '{}')
  } catch {
    assessmentData.value = {}
  }

  templateFields.value = []
  fieldLabelMap.value = {}
  // 优先从模板字段拿中文名称（fieldLabel）
  const templateId = row?.templateId ?? row?.template_id
  if (templateId) {
    try {
      const res = await templateApi.getTemplateFields(templateId)
      const fields = res?.data || []
      templateFields.value = fields
      const map = {}
      for (const f of fields) {
        if (f?.fieldName) map[f.fieldName] = f.fieldLabel || f.fieldName
        if (f?.fieldCode) map[f.fieldCode] = f.fieldLabel || f.fieldCode
      }
      fieldLabelMap.value = map
    } catch (e) {
      // 不阻塞详情展示
      templateFields.value = []
      fieldLabelMap.value = {}
    }
  }

  detailVisible.value = true
}

const handlePreviewPdf = async (row) => {
  previewLoading.value = true
  previewVisible.value = true
  previewRecordId.value = row.id
  previewType.value = 'PDF'
  
  try {
    const res = await reportApi.previewPdf(row.id)
    if (res.code === 200 && res.data) {
      // 将Base64转换为Blob URL
      const byteCharacters = atob(res.data)
      const byteNumbers = new Array(byteCharacters.length)
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i)
      }
      const byteArray = new Uint8Array(byteNumbers)
      const blob = new Blob([byteArray], { type: 'application/pdf' })
      previewUrl.value = URL.createObjectURL(blob)
    } else {
      ElMessage.error('预览失败：' + (res.message || '未知错误'))
      previewVisible.value = false
    }
  } catch (error) {
    ElMessage.error('预览失败：' + (error.message || '未知错误'))
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

const handleClosePreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  previewVisible.value = false
  previewRecordId.value = null
}

const handleDownloadFromPreview = async () => {
  if (!previewRecordId.value) return
  
  try {
    const blob = await reportApi.generatePdf(previewRecordId.value)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `assessment_report_${previewRecordId.value}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('PDF导出成功')
  } catch (error) {
    ElMessage.error('PDF导出失败')
  }
}

const handleExportPdf = async (row) => {
  try {
    const blob = await reportApi.generatePdf(row.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `assessment_report_${row.recordNo}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('PDF导出成功')
  } catch (error) {
    ElMessage.error('PDF导出失败')
  }
}

const handleExportWord = async (row) => {
  try {
    const blob = await reportApi.generateWord(row.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `assessment_report_${row.recordNo}.docx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Word导出成功')
  } catch (error) {
    ElMessage.error('Word导出失败')
  }
}

const handleGenerateSuggestion = async (row) => {
  suggestionLoading.value = true
  suggestionVisible.value = true
  suggestionRecordId.value = row.id
  treatmentSuggestion.value = ''
  
  try {
    const res = await treatmentSuggestionApi.generateSuggestion(row.id)
    if (res.code === 200 && res.data) {
      // 现在返回的是TreatmentSuggestion对象，包含suggestionContent字段
      treatmentSuggestion.value = res.data.suggestionContent || res.data
      ElMessage.success('诊疗建议生成成功')
    } else {
      ElMessage.error('生成诊疗建议失败：' + (res.message || '未知错误'))
    }
  } catch (error) {
    ElMessage.error('生成诊疗建议失败：' + (error.message || '未知错误'))
  } finally {
    suggestionLoading.value = false
  }
}

const handleRegenerateSuggestion = async () => {
  if (!suggestionRecordId.value) return
  
  suggestionLoading.value = true
  treatmentSuggestion.value = ''
  
  try {
    const res = await treatmentSuggestionApi.regenerateSuggestion(suggestionRecordId.value)
    if (res.code === 200 && res.data) {
      // 现在返回的是TreatmentSuggestion对象，包含suggestionContent字段
      treatmentSuggestion.value = res.data.suggestionContent || res.data
      ElMessage.success('诊疗建议重新生成成功')
    } else {
      ElMessage.error('重新生成失败：' + (res.message || '未知错误'))
    }
  } catch (error) {
    ElMessage.error('重新生成失败：' + (error.message || '未知错误'))
  } finally {
    suggestionLoading.value = false
  }
}

const handleCloseSuggestion = () => {
  suggestionVisible.value = false
  treatmentSuggestion.value = ''
  suggestionRecordId.value = null
}

const handleCopySuggestion = () => {
  if (!treatmentSuggestion.value) return
  
  navigator.clipboard.writeText(treatmentSuggestion.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const formatSuggestion = (text) => {
  if (!text) return ''
  
  // 将Markdown格式转换为HTML
  let html = text
    // 标题
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
    // 列表
    .replace(/^\d+\.\s+(.*$)/gim, '<li>$1</li>')
    // 换行
    .replace(/\n/g, '<br>')
  
  // 包装列表项
  html = html.replace(/(<li>.*<\/li>)/gim, '<ul>$1</ul>')
  
  return html
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.assessment-history {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.suggestion-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
  line-height: 1.8;
  font-size: 14px;
}

.suggestion-content h1 {
  font-size: 20px;
  margin: 20px 0 10px 0;
  color: #303133;
}

.suggestion-content h2 {
  font-size: 18px;
  margin: 18px 0 8px 0;
  color: #409eff;
}

.suggestion-content h3 {
  font-size: 16px;
  margin: 16px 0 6px 0;
  color: #606266;
}

.suggestion-content ul {
  margin: 10px 0;
  padding-left: 30px;
}

.suggestion-content li {
  margin: 8px 0;
}

.suggestion-content strong {
  color: #e6a23c;
  font-weight: bold;
}
</style>

