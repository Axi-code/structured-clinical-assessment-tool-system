<template>
  <div class="diagnosis-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>诊断详情</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      
      <div v-if="patient">
        <!-- 患者基本信息 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <span>患者基本信息</span>
          </template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="患者编号">{{ patient.patientNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ patient.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ patient.gender }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ patient.age }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ patient.phone }}</el-descriptions-item>
            <el-descriptions-item label="科室">{{ patient.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="当前诊断" :span="3">
              <el-tag :type="patient.diagnosisName ? 'success' : 'warning'" size="large">
                {{ patient.diagnosisName || '未明确' }}
              </el-tag>
              <el-tag
                v-if="latestAiDiagnosis"
                :type="patient.diagnosisName && latestAiDiagnosisMatched ? 'success' : 'info'"
                size="large"
                style="margin-left: 10px"
              >
                AI建议：{{ latestAiDiagnosis }}
              </el-tag>
              <el-button
                v-if="canConfirmDiagnosis && canAdoptAiDiagnosis"
                link
                type="warning"
                style="margin-left: 10px"
                :loading="aiDiagnosisAdopting"
                @click="handleAdoptLatestAiDiagnosis"
              >
                加入诊断字典并采用
              </el-button>
              <el-button 
                v-if="canConfirmDiagnosis" 
                link 
                type="primary" 
                style="margin-left: 10px" 
                @click="handleUpdateDiagnosis"
              >
                {{ diagnosisActionText }}
              </el-button>
              <div class="diagnosis-helper">{{ diagnosisHelperText }}</div>
              <el-alert
                v-if="canAdoptAiDiagnosis"
                class="diagnosis-alert"
                type="warning"
                :closable="false"
                title="最新 AI 建议诊断尚未匹配到当前诊断，可一键加入当前科室的诊断字典并同步到患者信息。"
              />
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="3">{{ patient.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- 评估记录统计 -->
        <el-card class="info-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>评估记录统计</span>
              <el-button v-if="canCreateAssessment" type="primary" @click="handleCreateAssessment">新建评估</el-button>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="总评估次数" :value="assessmentStats.total" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="最近评估时间" :value="formatTime(assessmentStats.latestTime) || '-'" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="平均总分" :value="assessmentStats.avgScore || 0" :precision="2" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="高风险次数" :value="assessmentStats.highRiskCount || 0">
                <template #suffix>
                  <el-tag type="danger" size="small">次</el-tag>
                </template>
              </el-statistic>
            </el-col>
          </el-row>
        </el-card>
        
        <!-- 诊疗建议历史 -->
        <el-card class="info-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>诊疗建议历史</span>
              <el-button link type="primary" @click="handleViewSuggestionHistory">查看全部</el-button>
            </div>
          </template>
          <el-table :data="suggestionHistory" border v-loading="suggestionHistoryLoading">
            <el-table-column prop="suggestionNo" label="建议编号" width="180" />
            <el-table-column prop="createTime" label="生成时间" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="generatorName" label="生成人" width="120" />
            <el-table-column label="建议内容预览" min-width="300">
              <template #default="scope">
                <div class="content-preview">
                  {{ getContentPreview(scope.row.suggestionContent) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="handleViewSuggestionDetail(scope.row)">查看详情</el-button>
                <el-button link type="success" @click="handleCopySuggestion(scope.row)">复制</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        
        <!-- 最近评估记录 -->
        <el-card class="info-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>最近评估记录</span>
              <el-button link type="primary" @click="handleViewAllHistory">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentAssessments" border v-loading="historyLoading">
            <el-table-column prop="recordNo" label="记录编号" width="150" />
            <el-table-column prop="createTime" label="评估时间" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="assessorName" label="评估人" width="120" />
            <el-table-column prop="totalScore" label="总分" width="100" align="center" />
            <el-table-column prop="assessmentResult" label="评估结果" width="150" />
            <el-table-column prop="riskLevel" label="风险等级" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.riskLevel" :type="getRiskTagType(scope.row.riskLevel)">
                  {{ scope.row.riskLevel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="handleViewRecordDetail(scope.row)">查看详情</el-button>
                <el-button link type="warning" @click="handleGenerateSuggestion(scope.row)">生成建议</el-button>
                <el-button link type="success" @click="handleExportPdf(scope.row)">导出PDF</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
      
      <el-empty v-else description="未找到患者信息" />
    </el-card>
    
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

    <!-- 更新诊断对话框 -->
    <el-dialog v-model="diagnosisVisible" title="确认当前诊断" width="420px">
      <el-form :model="diagnosisForm" label-width="80px">
        <el-form-item label="AI建议">
          <el-input
            :model-value="latestAiDiagnosis || '暂无 AI 建议诊断'"
            readonly
            placeholder="暂无 AI 建议诊断"
          />
        </el-form-item>
        <el-form-item label="确诊诊断">
          <el-select
            v-model="diagnosisForm.diagnosisId"
            placeholder="请选择当前确诊诊断"
            style="width: 100%"
            filterable
            clearable
            no-data-text="当前科室暂无可选诊断，请先在诊断管理中维护"
          >
            <el-option v-for="d in diagnosisOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="canAdoptAiDiagnosis"
          type="warning"
          :closable="false"
          show-icon
          title="当前科室诊断字典中暂未匹配到该 AI 建议诊断。"
        />
        <div class="dialog-tip">系统会先根据最新 AI 评估结果自动匹配当前诊断；这里仅用于医生确认或修正。</div>
      </el-form>
      <template #footer>
        <el-button @click="diagnosisVisible = false">取消</el-button>
        <el-button
          v-if="canAdoptAiDiagnosis"
          type="warning"
          @click="handleAdoptLatestAiDiagnosis"
          :loading="aiDiagnosisAdopting"
        >
          加入字典并采用
        </el-button>
        <el-button type="primary" @click="submitDiagnosis" :loading="diagnosisSubmitting">保存诊断</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { patientApi, assessmentApi, reportApi, treatmentSuggestionApi, diagnosisApi } from '../../api'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '../../utils/datetime'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canCreateAssessment = computed(() => userStore.isAdmin || userStore.userRole === 'DOCTOR')
const canConfirmDiagnosis = computed(() => userStore.isAdmin || userStore.userRole === 'DOCTOR')
const loading = ref(false)
const historyLoading = ref(false)
const suggestionHistoryLoading = ref(false)
const patient = ref(null)
const assessmentHistory = ref([])
const recentAssessments = ref([])
const suggestionHistory = ref([])

const suggestionVisible = ref(false)
const suggestionLoading = ref(false)
const treatmentSuggestion = ref('')
const suggestionRecordId = ref(null)
const currentSuggestion = ref(null)

const diagnosisVisible = ref(false)
const diagnosisSubmitting = ref(false)
const aiDiagnosisAdopting = ref(false)
const diagnosisOptions = ref([])
const diagnosisForm = reactive({
  diagnosisId: null
})

const assessmentStats = reactive({
  total: 0,
  latestTime: '',
  avgScore: 0,
  highRiskCount: 0
})

const patientId = computed(() => route.params.id)
const latestAssessment = computed(() => recentAssessments.value[0] || null)
const latestAiDiagnosis = computed(() => latestAssessment.value?.aiDiagnosisName || '')
const latestAiDiagnosisMatched = computed(() => {
  if (!patient.value?.diagnosisName || !latestAiDiagnosis.value) return false
  return isDiagnosisNameMatched(patient.value.diagnosisName, latestAiDiagnosis.value)
})
const canAdoptAiDiagnosis = computed(() => !!latestAiDiagnosis.value && !latestAiDiagnosisMatched.value && !!patient.value?.departmentId)
const diagnosisActionText = computed(() => (patient.value?.diagnosisName ? '修改诊断' : '确认诊断'))
const diagnosisHelperText = computed(() => {
  if (latestAssessment.value) {
    const summary = []
    if (latestAssessment.value.assessmentResult) {
      summary.push(`最近评估结论：${latestAssessment.value.assessmentResult}`)
    }
    if (latestAssessment.value.riskLevel) {
      summary.push(`风险等级：${latestAssessment.value.riskLevel}`)
    }
    if (latestAiDiagnosis.value) {
      summary.push(`AI建议诊断：${latestAiDiagnosis.value}`)
    }
    const prefix = summary.length > 0 ? `${summary.join('；')}。` : ''
    return `${prefix}系统会优先根据最新 AI 结果自动更新当前诊断，医生可结合病史和检查结果确认或修正。`
  }
  return '患者建档时只录入基本信息；完成评估后，系统会依据 AI 结果自动匹配诊断，医生可进一步确认。'
})

const formatTime = (val) => formatDateTime(val) || val || ''
const normalizeDiagnosisName = (value) => (value || '')
  .replace(/[\s,，。；;、:：()（）\-_/]+/g, '')
  .toLowerCase()

const isDiagnosisNameMatched = (left, right) => {
  const normalizedLeft = normalizeDiagnosisName(left)
  const normalizedRight = normalizeDiagnosisName(right)
  if (!normalizedLeft || !normalizedRight) return false
  return normalizedLeft === normalizedRight ||
    normalizedLeft.includes(normalizedRight) ||
    normalizedRight.includes(normalizedLeft)
}

const findDiagnosisOptionByAiSuggestion = () => {
  if (!latestAiDiagnosis.value) return null
  const normalizedSuggestion = normalizeDiagnosisName(latestAiDiagnosis.value)
  if (!normalizedSuggestion) return null

  const exactMatch = diagnosisOptions.value.find(option =>
    normalizeDiagnosisName(option.name) === normalizedSuggestion
  )
  if (exactMatch) return exactMatch

  return diagnosisOptions.value.find(option => {
    const normalizedName = normalizeDiagnosisName(option.name)
    return normalizedName.includes(normalizedSuggestion) || normalizedSuggestion.includes(normalizedName)
  }) || null
}

const fetchPatient = async () => {
  loading.value = true
  try {
    const res = await patientApi.getPatient(patientId.value)
    patient.value = res.data
  } catch (error) {
    ElMessage.error('获取患者信息失败')
  } finally {
    loading.value = false
  }
}

const fetchAssessmentHistory = async () => {
  historyLoading.value = true
  try {
    const res = await assessmentApi.getPatientHistory(patientId.value)
    assessmentHistory.value = res.data || []
    
    // 计算统计信息
    assessmentStats.total = assessmentHistory.value.length
    if (assessmentHistory.value.length > 0) {
      const sortedHistory = assessmentHistory.value.sort((a, b) =>
        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      )
      assessmentStats.latestTime = sortedHistory[0].createTime
      
      // 计算平均分
      const scores = sortedHistory
        .map(r => r.totalScore)
        .filter(s => s != null && !isNaN(s))
      if (scores.length > 0) {
        assessmentStats.avgScore = scores.reduce((a, b) => a + b, 0) / scores.length
      }
      
      // 统计高风险次数
      assessmentStats.highRiskCount = sortedHistory.filter(r => 
        r.riskLevel && (r.riskLevel.includes('高') || r.riskLevel === 'HIGH')
      ).length
      
      // 显示最近5条记录
      recentAssessments.value = sortedHistory.slice(0, 5)
    }
  } catch (error) {
    ElMessage.error('获取评估历史失败')
  } finally {
    historyLoading.value = false
  }
}

const fetchSuggestionHistory = async () => {
  suggestionHistoryLoading.value = true
  try {
    const res = await treatmentSuggestionApi.getSuggestionsByPatientId(patientId.value)
    if (res.code === 200) {
      // 显示最近5条记录
      suggestionHistory.value = (res.data || []).slice(0, 5)
    }
  } catch (error) {
    ElMessage.error('获取诊疗建议历史失败')
  } finally {
    suggestionHistoryLoading.value = false
  }
}

const handleCreateAssessment = () => {
  router.push({
    path: '/assessment/create',
    query: { patientId: patientId.value }
  })
}

const refreshDiagnosisContext = async () => {
  await Promise.all([
    fetchPatient(),
    fetchAssessmentHistory()
  ])
}

const handleViewAllHistory = () => {
  router.push(`/assessment/history/${patientId.value}`)
}

const handleViewSuggestionHistory = () => {
  router.push(`/diagnosis/suggestion-history/${patientId.value}`)
}

const handleViewSuggestionDetail = (row) => {
  currentSuggestion.value = row
  suggestionVisible.value = true
  treatmentSuggestion.value = row.suggestionContent
}

const getContentPreview = (content) => {
  if (!content) return '-'
  // 显示前100个字符
  const text = content.replace(/\n/g, ' ').replace(/\*\*/g, '').replace(/\*/g, '')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

const handleViewRecordDetail = (row) => {
  router.push(`/assessment/history/${patientId.value}`)
}

const handleGenerateSuggestion = async (row) => {
  suggestionLoading.value = true
  suggestionVisible.value = true
  suggestionRecordId.value = row.id
  treatmentSuggestion.value = ''
  
  try {
    const res = await treatmentSuggestionApi.generateSuggestion(row.id)
    if (res.code === 200) {
      // 现在返回的是TreatmentSuggestion对象，包含suggestionContent字段
      treatmentSuggestion.value = res.data.suggestionContent || res.data
      ElMessage.success('诊疗建议生成成功')
      // 刷新历史记录列表
      fetchSuggestionHistory()
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
    if (res.code === 200) {
      // 现在返回的是TreatmentSuggestion对象，包含suggestionContent字段
      treatmentSuggestion.value = res.data.suggestionContent || res.data
      ElMessage.success('诊疗建议重新生成成功')
      // 刷新历史记录列表
      fetchSuggestionHistory()
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

const handleUpdateDiagnosis = async () => {
  if (!patient.value.departmentId) {
    ElMessage.warning('患者尚未关联科室，无法选择诊断')
    return
  }
  try {
    const res = await diagnosisApi.listByDepartment(patient.value.departmentId)
    diagnosisOptions.value = res.data || []
    const aiMatchedOption = findDiagnosisOptionByAiSuggestion()
    diagnosisForm.diagnosisId = patient.value.diagnosisId || aiMatchedOption?.id || null
    diagnosisVisible.value = true
  } catch (error) {
    ElMessage.error('获取诊断列表失败')
  }
}

const handleAdoptLatestAiDiagnosis = async () => {
  if (!canAdoptAiDiagnosis.value) return
  aiDiagnosisAdopting.value = true
  try {
    const res = await patientApi.adoptLatestAiDiagnosis(patientId.value)
    ElMessage.success(res.message || 'AI 诊断已同步')
    diagnosisVisible.value = false
    await refreshDiagnosisContext()
  } catch (error) {
    if (!error?.message) {
      ElMessage.error('AI 诊断采用失败')
    }
  } finally {
    aiDiagnosisAdopting.value = false
  }
}

const submitDiagnosis = async () => {
  diagnosisSubmitting.value = true
  try {
    await patientApi.updateDiagnosis(patientId.value, diagnosisForm.diagnosisId)
    ElMessage.success('诊断更新成功')
    diagnosisVisible.value = false
    await refreshDiagnosisContext()
  } catch (error) {
    if (!error?.message) {
      ElMessage.error('诊断更新失败')
    }
  } finally {
    diagnosisSubmitting.value = false
  }
}

const handleCopySuggestion = (row) => {
  const content = row ? row.suggestionContent : treatmentSuggestion.value
  if (!content) return
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const handleExportPdf = async (row) => {
  try {
    const res = await reportApi.generatePdf(row.id)
    const blob = new Blob([res], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `评估报告_${row.recordNo}.pdf`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const getRiskTagType = (riskLevel) => {
  if (!riskLevel) return ''
  if (riskLevel.includes('高') || riskLevel === 'HIGH') return 'danger'
  if (riskLevel.includes('中') || riskLevel === 'MEDIUM') return 'warning'
  if (riskLevel.includes('低') || riskLevel === 'LOW') return 'success'
  return 'info'
}

const formatSuggestion = (text) => {
  if (!text) return ''
  // 简单的Markdown格式化
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

onMounted(() => {
  fetchPatient()
  fetchAssessmentHistory()
  fetchSuggestionHistory()
})
</script>

<style scoped>
.diagnosis-detail {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-card {
  margin-bottom: 20px;
}

.diagnosis-helper {
  margin-top: 10px;
  color: #909399;
  line-height: 1.6;
}

.diagnosis-alert {
  margin-top: 12px;
}

.dialog-tip {
  margin-top: 4px;
  color: #909399;
  line-height: 1.6;
  font-size: 13px;
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
