<template>
  <div class="rule-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评估规则列表</span>
          <el-button v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'" type="primary" @click="handleAdd">
            新增规则
          </el-button>
        </div>
      </template>
      
      <el-form :model="searchForm" class="search-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="模板">
              <el-select
                v-model="searchForm.templateId"
                placeholder="请选择模板"
                clearable
                filterable
                teleported
                style="width: 100%"
              >
                <el-option
                  v-for="template in templateList"
                  :key="template.id"
                  :label="template.templateName"
                  :value="template.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="规则名称">
              <el-input v-model="searchForm.ruleName" placeholder="请输入规则名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="规则类型">
              <el-select v-model="searchForm.ruleType" placeholder="请选择规则类型" clearable teleported style="width: 100%">
                <el-option label="评分规则" value="SCORE" />
                <el-option label="风险规则" value="RISK" />
                <el-option label="计算规则" value="CALCULATE" />
                <el-option label="异常检测" value="ABNORMAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable teleported style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="24" :lg="24">
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      
      <el-empty v-if="!loading && tableData.length === 0" description="暂无评估规则数据">
        <el-button type="primary" @click="handleAdd">新增规则</el-button>
      </el-empty>
      
      <el-table v-else :data="tableData" v-loading="loading" border>
        <el-table-column prop="ruleName" label="规则名称" width="200" />
        <el-table-column prop="ruleCode" label="规则编码" width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="120">
          <template #default="scope">
            <el-tag :type="getRuleTypeTag(scope.row.ruleType)">
              {{ getRuleTypeText(scope.row.ruleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="templateName" label="所属模板" width="200" />
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-switch
              v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'"
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
        <el-table-column prop="conditionExpression" label="条件表达式" min-width="200" show-overflow-tooltip />
        <el-table-column prop="resultExpression" label="结果表达式" min-width="200" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'"
              link
              type="primary"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.isAdmin"
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ruleApi } from '@/api'
import { templateApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const templateList = ref([])

const searchForm = reactive({
  templateId: null,
  ruleName: '',
  ruleType: '',
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 获取模板列表
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

// 加载规则列表
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }
    // 过滤空值
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined || params[key] === '') {
        delete params[key]
      }
    })
    
    const res = await ruleApi.getRuleList(params)
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      pagination.total = res.data?.total || 0
      
      // 为每条规则添加模板名称
      for (const rule of tableData.value) {
        const template = templateList.value.find(t => t.id === rule.templateId)
        if (template) {
          rule.templateName = template.templateName
        } else {
          rule.templateName = '未知模板'
        }
      }
      
      if (tableData.value.length === 0 && pagination.total === 0) {
        console.log('规则列表为空，请检查：')
        console.log('1. 数据库中是否有评估规则数据')
        console.log('2. 当前用户是否有权限（需要 ADMIN 或 DOCTOR 角色）')
        console.log('3. 查询条件是否过于严格')
      }
    } else {
      ElMessage.error(res.message || '加载规则列表失败')
    }
  } catch (error) {
    console.error('加载规则列表失败:', error)
    ElMessage.error('加载规则列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 获取规则类型文本
const getRuleTypeText = (type) => {
  const map = {
    SCORE: '评分规则',
    RISK: '风险规则',
    CALCULATE: '计算规则',
    ABNORMAL: '异常检测'
  }
  return map[type] || type
}

// 获取规则类型标签
const getRuleTypeTag = (type) => {
  const map = {
    SCORE: 'success',
    RISK: 'danger',
    CALCULATE: 'warning',
    ABNORMAL: 'info'
  }
  return map[type] || ''
}

// 查询
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.templateId = null
  searchForm.ruleName = ''
  searchForm.ruleType = ''
  searchForm.status = null
  handleSearch()
}

// 新增
const handleAdd = () => {
  router.push('/rule/form')
}

// 编辑
const handleEdit = (row) => {
  router.push(`/rule/form?id=${row.id}`)
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await ruleApi.deleteRule(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除规则失败:', error)
      ElMessage.error('删除规则失败')
    }
  }
}

// 状态变更
const handleStatusChange = async (row) => {
  try {
    const res = await ruleApi.updateRuleStatus(row.id, row.status)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
    } else {
      ElMessage.error(res.message || '状态更新失败')
      // 恢复原状态
      row.status = row.status === 1 ? 0 : 1
    }
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadData()
}

onMounted(() => {
  loadTemplates()
  
  // 检查是否有模板ID参数
  const templateId = route.query.templateId
  if (templateId) {
    searchForm.templateId = parseInt(templateId)
  }
  
  loadData()
})
</script>

<style scoped>
.rule-list {
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
