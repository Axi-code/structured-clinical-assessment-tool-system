<template>
  <div class="template-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ templateId ? '编辑模板' : '新增模板' }}</span>
          <div>
            <el-button v-if="templateId" type="success" @click="handleCreateVersion">创建新版本</el-button>
            <el-button @click="$router.back()">返回</el-button>
          </div>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 1200px"
      >
        <el-card class="form-section">
          <template #header>基本信息</template>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="模板名称" prop="templateName">
                <el-input v-model="form.templateName" placeholder="请输入模板名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模板编码" prop="templateCode">
                <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="评估类别" prop="category">
                <el-input v-model="form.category" placeholder="请输入评估类别" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="版本号" prop="version">
                <el-input-number v-model="form.version" :min="1" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-select v-model="form.status" teleported style="width: 100%">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="理论最低分" prop="minScore">
                <el-input-number v-model="form.minScore" :precision="2" :min="0" placeholder="根据SCORE规则计算" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="理论最高分" prop="maxScore">
                <el-input-number v-model="form.maxScore" :precision="2" :min="0" placeholder="根据SCORE规则计算" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="适用科室" prop="departmentIds" required>
            <el-select
              v-model="form.departmentIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择可使用该模板的科室（至少选一个）"
              teleported
              style="width: 100%"
            >
              <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
            <div class="form-tip">仅被选中的科室可使用此模板进行评估，避免如内科使用压疮模板等不匹配情况</div>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入模板描述"
            />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-card>
        
        <el-card class="form-section" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>评估字段</span>
              <el-button type="primary" @click="handleAddField">添加字段</el-button>
            </div>
          </template>
          
          <el-table :data="fields" border>
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="fieldLabel" label="字段标签" width="150">
              <template #default="scope">
                <el-input
                  v-model="scope.row.fieldLabel"
                  placeholder="字段标签"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column prop="fieldCode" label="字段编码" width="150">
              <template #default="scope">
                <el-input
                  v-model="scope.row.fieldCode"
                  placeholder="字段编码"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column prop="fieldType" label="字段类型" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.fieldType" teleported size="small" style="width: 100%">
                  <el-option label="文本" value="TEXT" />
                  <el-option label="数字" value="NUMBER" />
                  <el-option label="日期" value="DATE" />
                  <el-option label="下拉选择" value="SELECT" />
                  <el-option label="单选" value="RADIO" />
                  <el-option label="多选" value="CHECKBOX" />
                  <el-option label="多行文本" value="TEXTAREA" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="required" label="必填" width="80">
              <template #default="scope">
                <el-switch v-model="scope.row.required" :active-value="1" :inactive-value="0" />
              </template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值" width="120">
              <template #default="scope">
                <el-input
                  v-model="scope.row.defaultValue"
                  placeholder="默认值"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column prop="options" label="选项值" min-width="260">
              <template #default="scope">
                <div class="options-editor">
                  <el-select
                    v-model="scope.row._optionList"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    collapse-tags
                    size="small"
                    placeholder="输入后回车创建，可选择已有项"
                    @change="val => handleOptionsChange(scope.row, val)"
                    @paste="event => handleOptionsPaste(event, scope.row)"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="opt in scope.row._optionList"
                      :key="opt"
                      :label="opt"
                      :value="opt"
                    />
                  </el-select>
                  <div class="options-helper">
                    <span>支持多行/逗号/分号批量粘贴，自动去重</span>
                    <span class="options-count">共 {{ scope.row._optionList?.length || 0 }} 项</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="80">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.sortOrder"
                  :min="0"
                  size="small"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button link type="danger" @click="handleDeleteField(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        
        <el-form-item style="margin-top: 20px">
          <el-button @click="$router.back()">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { templateApi, departmentApi } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const templateId = ref(null)
const fields = ref([])
const departmentList = ref([])

// 将用户输入的选项值规范化为数组（内部再转为 JSON 字符串）
const normalizeOptionsInput = (input) => {
  if (input === null || input === undefined) return []
  const raw = String(input).trim()
  if (!raw) return []

  // 1) 已是合法 JSON 数组
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return Array.from(new Set(parsed.map(item => String(item).trim()).filter(Boolean)))
    }
  } catch (_) {
    // ignore, fallback to split
  }

  // 2) 兼容 “每行一个” 或 “逗号/分号” 分隔的输入
  const split = raw.split(/[\r\n,，;；]+/).map(s => s.trim()).filter(Boolean)
  return Array.from(new Set(split))
}

const handleOptionsChange = (row, val) => {
  const list = Array.from(new Set((val || []).map(item => String(item).trim()).filter(Boolean)))
  row._optionList = list
  row._normalizedOptions = list.length ? JSON.stringify(list) : null
  row.options = row._normalizedOptions // 保持提交时的兼容字段
}

const handleOptionsPaste = (event, row) => {
  const text = event.clipboardData?.getData('text') || ''
  if (!text) return
  event.preventDefault()
  const pasted = normalizeOptionsInput(text)
  const merged = Array.from(new Set([...(row._optionList || []), ...pasted]))
  handleOptionsChange(row, merged)
}

const form = reactive({
  templateName: '',
  templateCode: '',
  category: '',
  version: 1,
  status: 1,
  description: '',
  remark: '',
  minScore: null,
  maxScore: null,
  departmentIds: []
})

const rules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请输入评估类别', trigger: 'blur' }
  ],
  departmentIds: [
    { required: true, type: 'array', min: 1, message: '请至少选择一个适用科室', trigger: 'change' }
  ]
}

const handleAddField = () => {
  fields.value.push({
    fieldLabel: '',
    fieldCode: '',
    fieldType: 'TEXT',
    required: 0,
    defaultValue: '',
    options: '',
    _optionList: [],
    validationRule: '',
    sortOrder: fields.value.length + 1,
    groupName: '',
    remark: ''
  })
}

const handleDeleteField = (index) => {
  fields.value.splice(index, 1)
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    // 验证字段
    if (fields.value.length === 0) {
      ElMessage.warning('请至少添加一个评估字段')
      return
    }
    
    for (let i = 0; i < fields.value.length; i++) {
      const field = fields.value[i]
      if (!field.fieldLabel || !field.fieldCode) {
        ElMessage.warning(`第${i + 1}个字段的标签和编码不能为空`)
        return
      }
      
      // 验证选项字段
      if (['SELECT', 'RADIO', 'CHECKBOX'].includes(field.fieldType)) {
        const optionList = field._optionList && field._optionList.length
          ? field._optionList
          : normalizeOptionsInput(field.options)
        if (!optionList.length) {
          ElMessage.warning(`第${i + 1}个字段（${field.fieldLabel}）的选项值需填写为有效列表，可用换行或逗号分隔`)
          return
        }
        // 缓存规范化后的值，后续提交时直接使用
        handleOptionsChange(field, optionList)
      }
    }
    
    submitting.value = true
    try {
      // 保存模板（含 departmentIds 科室绑定）
      const templateData = {
        ...form,
        id: templateId.value || undefined,
        departmentIds: form.departmentIds && form.departmentIds.length ? form.departmentIds : []
      }
      
      if (templateId.value) {
        await templateApi.updateTemplate(templateData)
      } else {
        const addRes = await templateApi.addTemplate(templateData)
        // 获取新创建的模板ID
        if (addRes.data && addRes.data.id) {
          templateId.value = addRes.data.id
        } else {
          // 如果后端没有返回ID，通过编码查询
          const res = await templateApi.getTemplateList({
            templateCode: form.templateCode,
            current: 1,
            size: 1
          })
          if (res.data.records && res.data.records.length > 0) {
            templateId.value = res.data.records[0].id
          }
        }
      }
      
      // 保存字段
      if (templateId.value) {
        // 先获取现有字段，删除不在列表中的字段
        const existingFieldsRes = await templateApi.getTemplateFields(templateId.value)
        const existingFields = existingFieldsRes.data || []
        const newFieldIds = fields.value.filter(f => f.id).map(f => f.id)
        
        // 删除已存在的字段（如果不在新列表中）
        for (const existingField of existingFields) {
          if (existingField.id && !newFieldIds.includes(existingField.id)) {
            await templateApi.deleteField(existingField.id)
          }
        }
        
        // 添加或更新字段
        for (const field of fields.value) {
          const fieldData = {
            ...field,
            templateId: templateId.value,
            options: field._normalizedOptions
              ? field._normalizedOptions
              : (field.options && field.options.trim() ? field.options.trim() : null),
            validationRule: field.validationRule && field.validationRule.trim() ? field.validationRule.trim() : null
          }
          
          // 如果字段有ID，说明是已存在的字段，执行更新
          if (field.id) {
            await templateApi.updateField(fieldData)
          } else {
            // 新字段，执行添加
            await templateApi.addField(fieldData)
          }
        }
      }
      
      ElMessage.success(templateId.value ? '更新成功' : '添加成功')
      router.push('/template')
    } catch (error) {
      ElMessage.error(error.message || (templateId.value ? '更新失败' : '添加失败'))
    } finally {
      submitting.value = false
    }
  })
}

const handleCreateVersion = async () => {
  if (!templateId.value) {
    ElMessage.warning('请先保存当前模板')
    return
  }
  
  try {
    const res = await templateApi.createNewVersion(templateId.value, {
      templateName: form.templateName,
      category: form.category,
      description: form.description,
      remark: `基于版本${form.version}创建`
    })
    ElMessage.success('创建新版本成功')
    // 跳转到新版本的编辑页面
    router.push(`/template/edit/${res.data.id}`)
  } catch (error) {
    ElMessage.error('创建新版本失败')
  }
}

const loadDepartments = async () => {
  try {
    const res = await departmentApi.listAll()
    departmentList.value = res.data || []
  } catch (e) {
    departmentList.value = []
  }
}

const fetchData = async () => {
  await loadDepartments()
  if (route.params.id) {
    templateId.value = parseInt(route.params.id)
    try {
      const [templateRes, fieldsRes] = await Promise.all([
        templateApi.getTemplate(templateId.value),
        templateApi.getTemplateFields(templateId.value)
      ])
      
      Object.assign(form, templateRes.data)
      form.departmentIds = templateRes.data?.departmentIds || []
      fields.value = (fieldsRes.data || []).map(field => ({
        ...field,
        options: field.options || '',
        _optionList: normalizeOptionsInput(field.options),
        _normalizedOptions: normalizeOptionsInput(field.options).length
          ? JSON.stringify(normalizeOptionsInput(field.options))
          : null,
        validationRule: field.validationRule || ''
      }))
    } catch (error) {
      ElMessage.error('获取模板数据失败')
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.template-form {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-section {
  margin-bottom: 20px;
}

.form-section :deep(.el-card__header) {
  padding: 15px 20px;
}

.form-section :deep(.el-card__body) {
  padding: 20px;
}

.options-editor {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.options-helper {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.options-count {
  color: #606266;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
