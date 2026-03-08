<template>
  <div class="rule-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ formData.id ? '编辑规则' : '新增规则' }}</span>
          <el-button @click="handleCancel">返回</el-button>
        </div>
      </template>
      
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="所属模板" prop="templateId">
          <el-select
            v-model="formData.templateId"
            placeholder="请选择模板"
            filterable
            teleported
            style="width: 100%"
            :disabled="!!formData.id"
          >
            <el-option
              v-for="template in templateList"
              :key="template.id"
              :label="template.templateName"
              :value="template.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="formData.ruleCode" placeholder="请输入规则编码（可选）" />
        </el-form-item>
        
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" placeholder="请选择规则类型" teleported style="width: 100%">
            <el-option label="评分规则" value="SCORE" />
            <el-option label="风险规则" value="RISK" />
            <el-option label="计算规则" value="CALCULATE" />
            <el-option label="异常检测" value="ABNORMAL" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="formData.priority" :min="0" :max="999" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px">
            数字越小优先级越高，相同优先级按创建时间排序
          </span>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-divider>表达式配置</el-divider>

        <el-alert
          title="规则录入规范（避免不生效/算错分）"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        >
          <template #default>
            <div style="font-size: 12px; line-height: 20px">
              <div><b>字段引用</b>：用 <code>${'{字段编码}'}</code> 或 <code>$字段编码</code>（推荐用上面的“字段插入”）</div>
              <div><b>SCORE（评分规则）</b>：结果表达式必须返回数字，例如 <code>5</code> / <code>10</code> / <code>Number(${'{age}'}) &gt;= 65 ? 5 : 0</code>；也支持 <code>score += 5</code>（系统会提供 score 初始值 0）</div>
              <div><b>RISK（风险规则）</b>：可在条件里直接用 <code>totalScore</code>（提交/实时计算时系统会提供总分变量）</div>
              <div><b>保存前校验</b>：保存时会先自动跑一次“测试表达式”，有语法/变量错误会阻止保存</div>
            </div>
          </template>
        </el-alert>

        <el-form-item label="字段插入">
          <div style="display: flex; gap: 10px; width: 100%; flex-wrap: wrap; align-items: center">
            <el-select
              v-model="selectedFieldCode"
              placeholder="从模板字段中选择"
              clearable
              teleported
              style="flex: 1; min-width: 260px"
              :disabled="!formData.templateId"
            >
              <el-option
                v-for="f in templateFields"
                :key="f.fieldCode"
                :label="`${f.fieldLabel}（${f.fieldCode}）`"
                :value="f.fieldCode"
              />
            </el-select>
            <el-button :disabled="!selectedFieldCode" @click="insertField('condition')">插入到条件</el-button>
            <el-button :disabled="!selectedFieldCode" @click="insertField('result')">插入到结果</el-button>
            <span style="color:#909399;font-size:12px">会插入 <code>${'{字段编码}'}</code></span>
          </div>
        </el-form-item>
        
        <el-form-item label="条件表达式" prop="conditionExpression">
          <el-input
            v-model="formData.conditionExpression"
            type="textarea"
            :rows="3"
            placeholder="请输入条件表达式，例如：${age} > 18 && ${gender} === '男'"
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px">
            <div>表达式语法：使用 ${字段编码} 或 $字段编码 引用评估数据字段</div>
            <div>支持JavaScript表达式，例如：${age} > 18、${score} >= 60 && ${score} <= 80</div>
            <div>如果为空，则规则始终执行</div>
          </div>
        </el-form-item>
        
        <el-form-item label="结果表达式" prop="resultExpression">
          <el-input
            v-model="formData.resultExpression"
            type="textarea"
            :rows="3"
            placeholder="请输入结果表达式，例如：${score} * 0.5"
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px">
            <div>用于计算规则（CALCULATE）和评分规则（SCORE）</div>
            <div>评分规则：表达式结果会累加到总分中</div>
            <div>计算规则：表达式结果会保存到结果中，键名为规则编码</div>
          </div>
        </el-form-item>
        
        <el-form-item label="规则内容（JSON）" prop="ruleContent">
          <el-input
            v-model="formData.ruleContent"
            type="textarea"
            :rows="5"
            placeholder='请输入规则内容（JSON格式），例如：{"score": 10, "riskLevel": "HIGH", "riskTip": "风险提示信息"}'
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px">
            <div>用于风险规则（RISK）和异常检测（ABNORMAL）</div>
            <div>风险规则示例：{"riskLevel": "HIGH", "riskTip": "高风险提示"}</div>
            <div>异常检测示例：{"tip": "检测到异常数据"}</div>
          </div>
        </el-form-item>
        
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="info" @click="handleTest" :disabled="!formData.conditionExpression && !formData.resultExpression">
            测试表达式
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ruleApi } from '@/api'
import { templateApi } from '@/api'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const submitting = ref(false)

const templateList = ref([])
const templateFields = ref([])
const selectedFieldCode = ref('')
const formData = reactive({
  id: null,
  templateId: null,
  ruleName: '',
  ruleCode: '',
  ruleType: 'SCORE',
  ruleContent: '',
  conditionExpression: '',
  resultExpression: '',
  priority: 0,
  status: 1,
  remark: ''
})

const rules = {
  templateId: [{ required: true, message: '请选择所属模板', trigger: 'change' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }]
}

const parseOptions = (optionsStr) => {
  if (!optionsStr) return []
  try {
    const arr = JSON.parse(optionsStr)
    return Array.isArray(arr) ? arr : []
  } catch {
    return optionsStr.split(',').map(s => s.trim()).filter(Boolean)
  }
}

const buildTestDataFromFields = () => {
  const testData = {
    // 常用变量
    score: 0,
    totalScore: 0,
    riskLevel: 'LOW'
  }
  for (const f of (templateFields.value || [])) {
    const code = f.fieldCode
    if (!code) continue
    switch ((f.fieldType || '').toUpperCase()) {
      case 'NUMBER':
        testData[code] = code === 'age' ? 70 : 1
        break
      case 'DATE':
        testData[code] = '2026-01-01'
        break
      case 'CHECKBOX': {
        const opts = parseOptions(f.options)
        testData[code] = opts.length ? [opts[0]] : []
        break
      }
      case 'SELECT':
      case 'RADIO': {
        const opts = parseOptions(f.options)
        testData[code] = opts.length ? opts[0] : '示例'
        break
      }
      case 'TEXTAREA':
      case 'TEXT':
      default:
        testData[code] = '示例'
        break
    }
  }
  return testData
}

const loadTemplateFields = async (templateId) => {
  if (!templateId) {
    templateFields.value = []
    selectedFieldCode.value = ''
    return
  }
  try {
    const res = await templateApi.getTemplateFields(templateId)
    templateFields.value = res.data || []
  } catch (error) {
    console.error('加载模板字段失败:', error)
    templateFields.value = []
  }
}

const insertField = (target) => {
  if (!selectedFieldCode.value) return
  const token = '${' + selectedFieldCode.value + '}'
  if (target === 'condition') {
    formData.conditionExpression = (formData.conditionExpression || '') + token
  } else {
    formData.resultExpression = (formData.resultExpression || '') + token
  }
}

// 加载模板列表
const loadTemplates = async () => {
  try {
    const res = await templateApi.getTemplateList({ current: 1, size: 1000 })
    if (res.code === 200) {
      templateList.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载模板列表失败:', error)
  }
}

// 加载规则详情
const loadRule = async (id) => {
  try {
    const res = await ruleApi.getRule(id)
    if (res.code === 200) {
      Object.assign(formData, res.data)
    } else {
      ElMessage.error(res.message || '加载规则详情失败')
      router.back()
    }
  } catch (error) {
    console.error('加载规则详情失败:', error)
    ElMessage.error('加载规则详情失败')
    router.back()
  }
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    // 保存前先校验表达式，避免录入后不生效/算错分
    try {
      const res = await ruleApi.testRule({
        conditionExpression: formData.conditionExpression,
        resultExpression: formData.resultExpression,
        testData: buildTestDataFromFields()
      })
      if (!(res?.code === 200 && res.data?.success)) {
        ElMessage.error(res?.data?.message || res?.message || '表达式校验失败，请检查条件/结果表达式')
        return
      }
      // SCORE 规则：结果表达式应返回数字（允许 score += x 这种写法返回非数字时给提示）
      if (formData.ruleType === 'SCORE' && formData.resultExpression) {
        const v = res.data.expressionResult
        if (v !== undefined && v !== null && typeof v !== 'number') {
          ElMessage.warning('评分规则建议让结果表达式返回数字（例如 5/10），否则可能无法累加到总分')
        }
      }
    } catch (e) {
      ElMessage.error('表达式校验失败，请检查变量名/语法')
      return
    }

    submitting.value = true
    try {
      let res
      if (formData.id) {
        res = await ruleApi.updateRule(formData.id, formData)
      } else {
        res = await ruleApi.addRule(formData)
      }
      
      if (res.code === 200) {
        ElMessage.success(formData.id ? '更新成功' : '创建成功')
        router.back()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch (error) {
      console.error('保存规则失败:', error)
      ElMessage.error('保存规则失败')
    } finally {
      submitting.value = false
    }
  })
}

// 测试表达式
const handleTest = async () => {
  try {
    const testData = {
      conditionExpression: formData.conditionExpression,
      resultExpression: formData.resultExpression,
      testData: {
        age: 25,
        score: 75,
        gender: '男'
      }
    }
    
    const res = await ruleApi.testRule(testData)
    if (res.code === 200 && res.data) {
      const result = res.data
      if (result.success) {
        let message = '表达式测试通过\n'
        if (result.conditionResult !== undefined) {
          message += `条件表达式结果: ${result.conditionResult}\n`
        }
        if (result.expressionResult !== undefined) {
          message += `结果表达式结果: ${result.expressionResult}`
        }
        ElMessage.success(message)
      } else {
        ElMessage.error(result.message || '表达式测试失败')
      }
    } else {
      ElMessage.error(res.message || '表达式测试失败')
    }
  } catch (error) {
    console.error('测试表达式失败:', error)
    ElMessage.error('测试表达式失败: ' + (error.message || '未知错误'))
  }
}

// 取消
const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadTemplates()
  
  const id = route.query.id
  if (id) {
    formData.id = parseInt(id)
    loadRule(formData.id)
  }
})

watch(
  () => formData.templateId,
  (val) => {
    loadTemplateFields(val)
  },
  { immediate: true }
)
</script>

<style scoped>
.rule-form {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
