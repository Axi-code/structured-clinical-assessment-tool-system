<template>
  <div class="assessment-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ recordId ? '编辑评估' : '创建评估' }}</span>
          <div>
            <el-button @click="handleSaveDraft" :loading="saving">保存草稿</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">提交评估</el-button>
          </div>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        label-width="120px"
        style="max-width: 1000px"
      >
        <el-form-item label="选择患者" v-if="!recordId">
          <el-select
            v-model="form.patientId"
            filterable
            remote
            :remote-method="searchPatients"
            placeholder="请输入患者姓名或编号搜索"
            teleported
            style="width: 100%"
            @change="handlePatientChange"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name} (${item.patientNo})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="评估方式" v-if="!recordId">
          <el-radio-group v-model="assessmentMode">
            <el-radio-button label="FORM">填表模式</el-radio-button>
            <el-radio-button label="CHAT_TEMPLATE">对话（已有模板）</el-radio-button>
            <el-radio-button label="CHAT_AUTO">对话（AI自动建模板）</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="选择模板" v-if="!recordId && assessmentMode !== 'CHAT_AUTO'">
          <el-select v-model="form.templateId" placeholder="请选择评估模板" teleported style="width: 100%" @change="handleTemplateChange">
            <el-option
              v-for="item in templateOptions"
              :key="item.id"
              :label="item.templateName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="主诉描述" v-if="!recordId && assessmentMode === 'CHAT_AUTO'">
          <el-input
            v-model="autoSymptomText"
            type="textarea"
            :rows="2"
            placeholder="输入患者主诉，例如：近两周情绪低落、失眠、兴趣下降"
            style="width: 100%"
          />
          <el-button style="margin-top: 8px" type="primary" @click="handleGenerateAutoTemplate" :loading="autoTemplateLoading">
            AI生成模板并开始问诊
          </el-button>
        </el-form-item>

        <el-form-item label="辅助功能" v-if="!recordId && assessmentMode !== 'CHAT_AUTO'">
          <el-button link type="primary" @click="handleRecommendTemplate">
            AI推荐最接近模板
          </el-button>
        </el-form-item>
        
        <!-- 实时计算结果 -->
        <el-card v-if="fields.length > 0 && calculationResult" class="result-card" style="margin-bottom: 20px">
          <template #header>
            <span>实时计算结果</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="6" v-if="calculationResult.totalScore !== undefined && calculationResult.totalScore !== null">
              <div class="result-item">
                <div class="result-label">总分</div>
                <div class="result-value">{{ calculationResult.totalScore }}</div>
              </div>
            </el-col>
            <el-col :span="6" v-if="calculationResult.assessmentResult">
              <div class="result-item">
                <div class="result-label">评估结果</div>
                <div class="result-value">{{ calculationResult.assessmentResult }}</div>
              </div>
            </el-col>
            <el-col :span="6" v-if="calculationResult.riskLevel">
              <div class="result-item">
                <div class="result-label">风险等级</div>
                <el-tag :type="getRiskLevelTag(calculationResult.riskLevel)" size="large">
                  {{ getRiskLevelText(calculationResult.riskLevel) }}
                </el-tag>
              </div>
            </el-col>
          </el-row>
          <el-alert
            v-if="calculationResult.riskTips"
            :title="calculationResult.riskTips"
            :type="getRiskAlertType(calculationResult.riskLevel)"
            :closable="false"
            style="margin-top: 15px"
            show-icon
          />
          <el-alert
            v-if="calculationResult.abnormalDataTips && calculationResult.abnormalDataTips.length > 0"
            :title="`异常数据提示：${calculationResult.abnormalDataTips.join('; ')}`"
            type="warning"
            :closable="false"
            style="margin-top: 10px"
            show-icon
          />
        </el-card>
        
        <el-divider v-if="fields.length > 0 && assessmentMode === 'FORM'">评估数据</el-divider>

        <template v-if="assessmentMode === 'FORM'">
          <template v-for="field in fields" :key="field.id">
            <el-form-item
              :label="field.fieldLabel"
              :prop="`assessmentData.${field.fieldCode}`"
              :rules="getFieldRules(field)"
            >
              <el-input
                v-if="field.fieldType === 'TEXT'"
                v-model="form.assessmentData[field.fieldCode]"
                :placeholder="`请输入${field.fieldLabel}`"
                @input="handleDataChange"
              />
              <el-input-number
                v-else-if="field.fieldType === 'NUMBER'"
                v-model="form.assessmentData[field.fieldCode]"
                style="width: 100%"
                :disabled="field.fieldCode === 'age'"
                @change="handleDataChange"
              />
              <el-date-picker
                v-else-if="field.fieldType === 'DATE'"
                v-model="form.assessmentData[field.fieldCode]"
                type="date"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="handleDataChange"
              />
              <el-select
                v-else-if="field.fieldType === 'SELECT' || field.fieldType === 'RADIO'"
                v-model="form.assessmentData[field.fieldCode]"
                :placeholder="`请选择${field.fieldLabel}`"
                teleported
                style="width: 100%"
                :disabled="field.fieldCode === 'gender'"
                @change="handleDataChange"
              >
                <el-option
                  v-for="option in parseOptions(field.options)"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
              <el-checkbox-group
                v-else-if="field.fieldType === 'CHECKBOX'"
                v-model="form.assessmentData[field.fieldCode]"
                @change="handleDataChange"
              >
                <el-checkbox
                  v-for="option in parseOptions(field.options)"
                  :key="option"
                  :label="option"
                />
              </el-checkbox-group>
              <el-input
                v-else-if="field.fieldType === 'TEXTAREA'"
                v-model="form.assessmentData[field.fieldCode]"
                type="textarea"
                :rows="4"
                :placeholder="`请输入${field.fieldLabel}`"
                @input="handleDataChange"
              />
            </el-form-item>
          </template>
        </template>

        <div v-if="(assessmentMode === 'CHAT_TEMPLATE' || assessmentMode === 'CHAT_AUTO') && fields.length > 0" class="chat-mode">
          <el-divider>对话式动态评估</el-divider>
          <el-alert
            title="AI会通过对话自动提问并回填结构化数据，完成后可直接提交。"
            type="info"
            :closable="false"
            style="margin-bottom: 12px"
          />
          <div class="chat-window" v-loading="conversationLoading">
            <div v-for="(msg, idx) in conversationMessages" :key="idx" class="chat-item" :class="msg.role">
              <span class="chat-role">
                {{ msg.role === 'assistant' ? 'AI' : '患者' }}
                <el-tag v-if="msg.isClarify" type="warning" size="small" style="margin-left:4px">需补充</el-tag>
              </span>
              <div class="chat-content" :class="{ 'is-clarify': msg.isClarify }">{{ msg.content }}</div>
            </div>
          </div>
          <div class="chat-actions">
            <el-input
              v-model="chatInput"
              type="textarea"
              :rows="2"
              placeholder="请输入患者当前回答..."
              @keyup.ctrl.enter="handleSendChat"
            />
            <div class="chat-buttons">
              <el-button @click="handleStartConversation" :disabled="conversationLoading">开始对话</el-button>
              <el-button type="primary" @click="handleSendChat" :loading="conversationLoading">发送（Ctrl+Enter）</el-button>
              <el-button type="success" @click="handleFinalizeConversation" :loading="submitting">完成并提交</el-button>
            </div>
          </div>
          <div class="chat-progress">
            <el-progress :percentage="Math.round((conversationCompletion || 0) * 100)" />
            <div class="missing-fields" v-if="conversationMissingFields.length > 0">
              待补充字段：{{ conversationMissingFields.join('、') }}
            </div>
          </div>
        </div>
      </el-form>
    </el-card>

    <!-- 诊疗建议对话框：提交评估后弹出 -->
    <el-dialog
      v-model="suggestionVisible"
      title="AI诊疗建议"
      width="80%"
      :before-close="handleCloseSuggestion"
    >
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { assessmentApi, patientApi, templateApi, ruleApi, treatmentSuggestionApi, assessmentConversationApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const saving = ref(false)
const submitting = ref(false)
const recordId = ref(null)
const calculating = ref(false)

// 诊疗建议弹窗相关
const suggestionVisible = ref(false)
const suggestionLoading = ref(false)
const treatmentSuggestion = ref('')
const suggestionRecordId = ref(null)

const form = reactive({
  patientId: null,
  templateId: null,
  assessmentData: {}
})

const patientOptions = ref([])
const selectedPatient = ref(null)
const templateOptions = ref([])
const fields = ref([])
const calculationResult = ref(null)
let calculateTimer = null
const assessmentMode = ref('FORM')
const autoSymptomText = ref('')
const autoTemplateLoading = ref(false)

// 对话模式相关
const conversationSessionId = ref('')
const conversationMessages = ref([])
const chatInput = ref('')
const conversationLoading = ref(false)
const conversationCompletion = ref(0)
const conversationMissingFields = ref([])
const conversationNeedClarify = ref(false)

const searchPatients = async (query) => {
  if (query) {
    try {
      const params = { current: 1, size: 10 }
      // 患者编号格式：P 开头 + 数字，用 patientNo 精确查询；否则用 name 模糊查询
      if (/^P\d+/i.test(query.trim())) {
        params.patientNo = query.trim()
      } else {
        params.name = query.trim()
      }
      const res = await patientApi.getPatientList(params)
      patientOptions.value = res.data.records || []
    } catch (error) {
      // 忽略错误
    }
  }
}

// 将已选患者的基础信息（年龄、性别）填充到评估数据中
const applyPatientBaseInfo = () => {
  if (!selectedPatient.value || !fields.value || fields.value.length === 0) return

  fields.value.forEach(field => {
    if (field.fieldCode === 'age' && selectedPatient.value.age != null) {
      form.assessmentData.age = selectedPatient.value.age
    }
    if (field.fieldCode === 'gender' && selectedPatient.value.gender) {
      form.assessmentData.gender = selectedPatient.value.gender
    }
  })
}

const handlePatientChange = () => {
  const patient = patientOptions.value.find(p => p.id === form.patientId)
  selectedPatient.value = patient || null
  if (assessmentMode.value === 'CHAT_AUTO') {
    form.templateId = null
    fields.value = []
    calculationResult.value = null
  }
  resetConversationState()
  applyPatientBaseInfo()
  if (fields.value.length > 0) {
    calculateRealtime()
  }
}

const handleTemplateChange = async () => {
  resetConversationState()
  if (form.templateId) {
    try {
      const res = await templateApi.getTemplateFields(form.templateId)
      fields.value = res.data || []
      initFieldsDefaultData()

      // 重置计算结果
      calculationResult.value = null
      // 如果有数据，立即计算
      if (Object.keys(form.assessmentData).length > 0) {
        calculateRealtime()
      }
    } catch (error) {
      ElMessage.error('获取模板字段失败')
    }
  }
}

const resetConversationState = () => {
  conversationSessionId.value = ''
  conversationMessages.value = []
  chatInput.value = ''
  conversationCompletion.value = 0
  conversationMissingFields.value = []
  conversationNeedClarify.value = false
}

const initFieldsDefaultData = () => {
  fields.value.forEach(field => {
    if (form.assessmentData[field.fieldCode] === undefined || form.assessmentData[field.fieldCode] === null) {
      form.assessmentData[field.fieldCode] = field.fieldType === 'CHECKBOX' ? [] : ''
    }
  })
  applyPatientBaseInfo()
}

const handleGenerateAutoTemplate = async () => {
  if (!form.patientId) {
    ElMessage.warning('请先选择患者')
    return
  }
  if (!autoSymptomText.value || !autoSymptomText.value.trim()) {
    ElMessage.warning('请先输入主诉描述')
    return
  }
  autoTemplateLoading.value = true
  try {
    const res = await assessmentConversationApi.generateTemplate({
      patientId: form.patientId,
      symptomText: autoSymptomText.value.trim()
    })
    form.templateId = res.data.templateId
    fields.value = res.data.fields || []
    form.assessmentData = {}
    initFieldsDefaultData()
    resetConversationState()
    ElMessage.success(`已生成模板：${res.data.templateName}`)
    await handleStartConversation()
  } catch (error) {
    ElMessage.error(error.message || 'AI自动生成模板失败')
  } finally {
    autoTemplateLoading.value = false
  }
}

const handleStartConversation = async () => {
  if (!form.patientId) {
    ElMessage.warning('请先选择患者')
    return
  }
  if (!form.templateId) {
    ElMessage.warning(assessmentMode.value === 'CHAT_AUTO' ? '请先让AI生成模板' : '请先选择模板')
    return
  }
  conversationLoading.value = true
  try {
    const res = await assessmentConversationApi.startConversation({
      patientId: form.patientId,
      templateId: form.templateId,
      assessmentData: form.assessmentData
    })
    conversationSessionId.value = res.data.sessionId || ''
    conversationMessages.value = []
    if (res.data.assistantMessage) {
      conversationMessages.value.push({ role: 'assistant', content: res.data.assistantMessage })
    }
    if (res.data.mappedData) {
      Object.assign(form.assessmentData, res.data.mappedData)
      calculateRealtime()
    }
    conversationCompletion.value = res.data.completion || 0
    conversationMissingFields.value = res.data.missingFields || []
  } catch (error) {
    ElMessage.error(error.message || '开始对话失败')
  } finally {
    conversationLoading.value = false
  }
}

const handleSendChat = async () => {
  if (!chatInput.value || !chatInput.value.trim()) {
    ElMessage.warning('请先输入患者回答')
    return
  }
  if (!conversationSessionId.value) {
    await handleStartConversation()
  }
  const userMessage = chatInput.value.trim()
  conversationMessages.value.push({ role: 'user', content: userMessage })
  chatInput.value = ''

  conversationLoading.value = true
  try {
    const res = await assessmentConversationApi.sendReply({
      patientId: form.patientId,
      templateId: form.templateId,
      patientMessage: userMessage,
      messages: conversationMessages.value.map(msg => ({ role: msg.role, content: msg.content })),
      assessmentData: form.assessmentData
    })
    if (res.data.mappedData) {
      Object.assign(form.assessmentData, res.data.mappedData)
      calculateRealtime()
    }
    conversationNeedClarify.value = !!res.data.needClarify
    if (res.data.assistantMessage) {
      conversationMessages.value.push({
        role: 'assistant',
        content: res.data.assistantMessage,
        isClarify: !!res.data.needClarify
      })
    }
    conversationCompletion.value = res.data.completion || 0
    conversationMissingFields.value = res.data.missingFields || []
  } catch (error) {
    ElMessage.error(error.message || '对话失败')
  } finally {
    conversationLoading.value = false
  }
}

const handleFinalizeConversation = async () => {
  if (!form.patientId) {
    ElMessage.warning('请先选择患者')
    return
  }
  if (!form.templateId) {
    ElMessage.warning(assessmentMode.value === 'CHAT_AUTO' ? '请先让AI生成模板' : '请先选择模板')
    return
  }
  submitting.value = true
  try {
    const res = await assessmentConversationApi.finalizeConversation({
      patientId: form.patientId,
      templateId: form.templateId,
      assessmentData: form.assessmentData
    })
    recordId.value = res.data.id
    ElMessage.success('对话评估提交成功')
    await showSuggestionDialog()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const handleRecommendTemplate = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入患者主要症状描述', 'AI推荐模板', {
      confirmButtonText: '推荐',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：近两周情绪低落、失眠、兴趣下降',
      inputValidator: (v) => !!(v && v.trim()) || '请输入症状描述'
    })
    const res = await assessmentConversationApi.recommendTemplate({ symptomText: value })
    if (res.data.templateId) {
      form.templateId = res.data.templateId
      await handleTemplateChange()
      ElMessage.success(`已推荐模板：${res.data.templateName || res.data.templateId}`)
    } else {
      ElMessage.warning('未找到匹配模板，请手动选择')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '模板推荐失败')
    }
  }
}

// 数据变化处理（防抖）
const handleDataChange = () => {
  if (calculateTimer) {
    clearTimeout(calculateTimer)
  }
  calculateTimer = setTimeout(() => {
    calculateRealtime()
  }, 500) // 500ms 防抖
}

// 实时计算评估结果
const calculateRealtime = async () => {
  if (!form.templateId || !form.assessmentData || Object.keys(form.assessmentData).length === 0) {
    calculationResult.value = null
    return
  }
  
  // 过滤空值
  const filteredData = {}
  Object.keys(form.assessmentData).forEach(key => {
    const value = form.assessmentData[key]
    if (value !== null && value !== undefined && value !== '') {
      if (Array.isArray(value) && value.length === 0) {
        return
      }
      filteredData[key] = value
    }
  })
  
  if (Object.keys(filteredData).length === 0) {
    calculationResult.value = null
    return
  }
  
  calculating.value = true
  try {
    const res = assessmentMode.value === 'CHAT_AUTO'
      ? await assessmentConversationApi.calculateRealtime({
        patientId: form.patientId,
        templateId: form.templateId,
        assessmentData: filteredData
      })
      : await ruleApi.calculateRealtime({
        templateId: form.templateId,
        assessmentData: filteredData
      })
    if (res.code === 200) {
      calculationResult.value = res.data
    }
  } catch (error) {
    // 静默失败，不显示错误
    console.error('实时计算失败:', error)
  } finally {
    calculating.value = false
  }
}

// 获取风险等级文本（直接显示，PHQ-9 等量表的 无/轻度/中度/重度 不映射）
const getRiskLevelText = (level) => {
  if (!level) return ''
  const map = {
    CRITICAL: '严重',
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  }
  return map[level] || level
}

// 获取风险等级标签类型（支持 PHQ-9：无/轻度/中度/重度）
const getRiskLevelTag = (level) => {
  if (!level) return 'info'
  const map = {
    CRITICAL: 'danger',
    HIGH: 'danger',
    重度: 'danger',
    中重度障碍: 'danger',
    MEDIUM: 'warning',
    中度: 'warning',
    LOW: 'success',
    低风险: 'success',
    无: 'success',
    轻度: 'success',
    正常: 'success'
  }
  return map[level] || 'warning'
}

// 获取风险提示类型
const getRiskAlertType = (level) => {
  if (!level) return 'info'
  const map = {
    CRITICAL: 'error',
    HIGH: 'error',
    重度: 'error',
    MEDIUM: 'warning',
    中度: 'warning',
    LOW: 'info',
    无: 'info',
    轻度: 'info'
  }
  return map[level] || 'info'
}

const getFieldRules = (field) => {
  const rules = []
  if (field.required === 1) {
    rules.push({
      required: true,
      message: `请输入${field.fieldLabel}`,
      trigger: 'blur'
    })
  }
  return rules
}

const parseOptions = (optionsStr) => {
  if (!optionsStr) return []
  try {
    return JSON.parse(optionsStr)
  } catch {
    return optionsStr.split(',').map(s => s.trim())
  }
}

const handleSaveDraft = async () => {
  if (!form.patientId || !form.templateId) {
    ElMessage.warning('请先选择患者和模板')
    return
  }
  
  saving.value = true
  try {
    if (!recordId.value) {
      const res = await assessmentApi.createDraft({
        patientId: form.patientId,
        templateId: form.templateId
      })
      recordId.value = res.data.id
    }
    
    await assessmentApi.saveAssessment({
      recordId: recordId.value,
      status: 0,
      assessmentData: form.assessmentData
    })
    ElMessage.success('草稿保存成功')
  } catch (error) {
    ElMessage.error('保存草稿失败')
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  if (assessmentMode.value === 'CHAT_TEMPLATE' || assessmentMode.value === 'CHAT_AUTO') {
    await handleFinalizeConversation()
    return
  }
  if (!form.patientId || !form.templateId) {
    ElMessage.warning('请先选择患者和模板')
    return
  }
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (!recordId.value) {
          const res = await assessmentApi.createDraft({
            patientId: form.patientId,
            templateId: form.templateId
          })
          recordId.value = res.data.id
        }
        
        await assessmentApi.submitAssessment({
          recordId: recordId.value,
          assessmentData: form.assessmentData
        })
        ElMessage.success('评估提交成功')

        await showSuggestionDialog()
      } catch (error) {
        ElMessage.error(error.message || '提交评估失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const showSuggestionDialog = async () => {
  suggestionVisible.value = true
  suggestionLoading.value = true
  suggestionRecordId.value = recordId.value
  treatmentSuggestion.value = ''

  try {
    const res = await treatmentSuggestionApi.generateSuggestion(recordId.value)
    if (res.code === 200) {
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

const fetchTemplates = async () => {
  try {
    const params = { status: 1, current: 1, size: 100 }
    if (userStore.userInfo?.departmentId && !userStore.isAdmin) {
      params.departmentId = userStore.userInfo.departmentId
    }
    const res = await templateApi.getTemplateList(params)
    templateOptions.value = res.data.records || []
  } catch (error) {
    ElMessage.error('获取模板列表失败')
  }
}

onMounted(() => {
  fetchTemplates()
  if (route.params.id) {
    recordId.value = route.params.id
    // 可以加载已有评估记录数据
  }
})

watch(assessmentMode, (mode) => {
  resetConversationState()
  if (mode === 'CHAT_AUTO') {
    form.templateId = null
    fields.value = []
    calculationResult.value = null
  }
})

// 将诊疗建议内容格式化为可展示的 HTML（与诊断详情页保持一致）
const formatSuggestion = (text) => {
  if (!text) return ''
  return text
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

const handleCloseSuggestion = () => {
  suggestionVisible.value = false
  treatmentSuggestion.value = ''
  suggestionRecordId.value = null
}

const handleRegenerateSuggestion = async () => {
  if (!suggestionRecordId.value) return

  suggestionLoading.value = true
  treatmentSuggestion.value = ''

  try {
    const res = await treatmentSuggestionApi.regenerateSuggestion(suggestionRecordId.value)
    if (res.code === 200) {
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

const handleCopySuggestion = () => {
  if (!treatmentSuggestion.value) return
  navigator.clipboard.writeText(treatmentSuggestion.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style scoped>
.assessment-form {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-card {
  background: #f5f7fa;
}

.result-item {
  text-align: center;
}

.result-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.result-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
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

.chat-mode {
  margin-top: 8px;
}

.chat-window {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
  padding: 12px;
  min-height: 220px;
  max-height: 360px;
  overflow-y: auto;
}

.chat-item {
  margin-bottom: 10px;
}

.chat-item .chat-role {
  display: inline-block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.chat-item .chat-content {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 8px;
  line-height: 1.6;
  max-width: 100%;
  white-space: pre-wrap;
}

.chat-item.assistant .chat-content {
  background: #ecf5ff;
  color: #303133;
}

.chat-item.assistant .chat-content.is-clarify {
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
  color: #303133;
}

.chat-item.user {
  text-align: right;
}

.chat-item.user .chat-content {
  background: #f0f9eb;
  color: #303133;
}

.chat-actions {
  margin-top: 12px;
}

.chat-buttons {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.chat-progress {
  margin-top: 12px;
}

.missing-fields {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}
</style>

