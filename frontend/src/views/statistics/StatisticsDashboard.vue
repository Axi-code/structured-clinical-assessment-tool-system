<template>
  <div class="statistics-dashboard">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据统计与分析</span>
          <div>
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="margin-right: 10px"
            />
            <el-select
              v-model="filterDepartment"
              placeholder="选择科室"
              clearable
              teleported
              style="width: 150px; margin-right: 10px"
            >
              <el-option
                v-for="dept in departments"
                :key="dept"
                :label="dept"
                :value="dept"
              />
            </el-select>
            <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          </div>
        </div>
      </template>
      
      <div v-loading="loading">
        <!-- 统计概览 -->
        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value">{{ overview.totalRecords || 0 }}</div>
                <div class="stat-label">总评估记录</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value">{{ overview.averageScore || 0 }}</div>
                <div class="stat-label">平均分</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value">{{ riskCount }}</div>
                <div class="stat-label">高风险记录</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value">{{ departments.length }}</div>
                <div class="stat-label">参与科室</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <!-- 风险等级异常预警 -->
        <el-card class="alert-card" style="margin-bottom: 20px">
          <template #header>
            <div class="card-sub-header">
              <span>风险等级异常预警</span>
              <span class="sub-tip">对比基线期，快速发现某风险等级突然增加</span>
            </div>
          </template>
          <el-empty v-if="!riskAlerts.length" description="暂无异常预警" />
          <el-table v-else :data="riskAlerts" size="small" border>
            <el-table-column label="风险等级" prop="riskLevel" width="120">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前数量" prop="currentCount" width="100" />
            <el-table-column label="基线数量" prop="baselineCount" width="100" />
            <el-table-column label="增长" width="100">
              <template #default="{ row }">
                +{{ row.increase }}
              </template>
            </el-table-column>
            <el-table-column label="增长率" width="120">
              <template #default="{ row }">
                {{ (row.growthRate * 100).toFixed(0) }}%
              </template>
            </el-table-column>
            <el-table-column label="统计期" min-width="200">
              <template #default="{ row }">
                <div>{{ row.startTime }} ~ {{ row.endTime }}</div>
              </template>
            </el-table-column>
            <el-table-column label="基线期" min-width="200">
              <template #default="{ row }">
                <div>{{ row.baselineStartTime }} ~ {{ row.baselineEndTime }}</div>
              </template>
            </el-table-column>
            <el-table-column label="提示" prop="message" min-width="220" />
          </el-table>
        </el-card>

        <!-- 图表区域 -->
        <el-row :gutter="20">
          <!-- 时间趋势图 -->
          <el-col :span="12">
            <el-card>
              <template #header>评估记录时间趋势</template>
              <div ref="timeChartRef" style="width: 100%; height: 300px"></div>
            </el-card>
          </el-col>
          
          <!-- 科室分布图 -->
          <el-col :span="12">
            <el-card>
              <template #header>科室分布统计</template>
              <div ref="departmentChartRef" style="width: 100%; height: 300px"></div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" style="margin-top: 20px">
          <!-- 模板使用统计 -->
          <el-col :span="12">
            <el-card>
              <template #header>评估模板使用统计</template>
              <div ref="templateChartRef" style="width: 100%; height: 300px"></div>
            </el-card>
          </el-col>
          
          <!-- 风险等级分布 -->
          <el-col :span="12">
            <el-card>
              <template #header>风险等级分布</template>
              <div ref="riskChartRef" style="width: 100%; height: 300px"></div>
            </el-card>
          </el-col>
        </el-row>
        
        <!-- 数据表格 -->
        <el-card style="margin-top: 20px">
          <template #header>
            <el-tabs v-model="activeTab" @tab-change="handleTabChange">
              <el-tab-pane label="科室接诊数量" name="department"></el-tab-pane>
              <el-tab-pane label="模板使用数量" name="template"></el-tab-pane>
              <el-tab-pane label="评估结果数量" name="risk"></el-tab-pane>
            </el-tabs>
          </template>
          
          <el-table :data="tableData" border>
            <el-table-column
              v-if="activeTab === 'department'"
              prop="department"
              label="科室"
            />
            <el-table-column
              v-if="activeTab === 'template'"
              prop="templateName"
              label="模板名称"
            />
            <el-table-column
              v-if="activeTab === 'risk'"
              prop="riskLevel"
              label="风险等级"
            />
            <el-table-column prop="count" label="数量" />
          </el-table>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { statisticsApi, templateApi } from '../../api'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const loading = ref(false)
const dateRange = ref([])
const filterDepartment = ref('')
const departments = ref([])
const overview = ref({})
const activeTab = ref('department')
const tableData = ref([])
const riskAlerts = ref([])

const timeChartRef = ref(null)
const departmentChartRef = ref(null)
const templateChartRef = ref(null)
const riskChartRef = ref(null)

let timeChart = null
let departmentChart = null
let templateChart = null
let riskChart = null

// 初始化日期范围（默认最近30天）
const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  
  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  }
  
  dateRange.value = [
    formatDate(start),
    formatDate(end)
  ]
}

const handleSearch = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }
  
  loading.value = true
  try {
    await Promise.all([
      loadOverview(),
      loadTimeStatistics(),
      loadDepartmentStatistics(),
      loadTemplateStatistics(),
      loadRiskStatistics(),
      loadRiskAlerts()
    ])
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const loadOverview = async () => {
  try {
    const res = await statisticsApi.getStatisticsOverview({
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      department: filterDepartment.value || undefined
    })
    overview.value = res.data || {}
    
    // 提取科室列表
    if (res.data?.topDepartments) {
      departments.value = res.data.topDepartments.map(d => d.department)
    }
  } catch (error) {
    console.error('加载概览失败', error)
  }
}

const loadTimeStatistics = async () => {
  try {
    const res = await statisticsApi.statisticsByTime({
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      groupBy: 'day'
    })
    const data = res.data || []
    
    await nextTick()
    if (timeChartRef.value) {
      if (!timeChart) {
        timeChart = echarts.init(timeChartRef.value)
      }
      
      const option = {
        title: {
          text: '评估记录时间趋势',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: data.map(item => item.time)
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          data: data.map(item => item.count),
          type: 'line',
          smooth: true,
          areaStyle: {}
        }]
      }
      timeChart.setOption(option)
    }
  } catch (error) {
    console.error('加载时间统计失败', error)
  }
}

const loadDepartmentStatistics = async () => {
  try {
    const res = await statisticsApi.statisticsByDepartment({
      startTime: dateRange.value[0],
      endTime: dateRange.value[1]
    })
    const data = res.data || []
    tableData.value = data
    
    await nextTick()
    if (departmentChartRef.value) {
      if (!departmentChart) {
        departmentChart = echarts.init(departmentChartRef.value)
      }
      
      const option = {
        title: {
          text: '科室分布统计',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        series: [{
          type: 'pie',
          radius: '60%',
          data: data.map(item => ({
            value: item.count,
            name: item.department
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      }
      departmentChart.setOption(option)
    }
  } catch (error) {
    console.error('加载科室统计失败', error)
  }
}

const loadTemplateStatistics = async () => {
  try {
    const res = await statisticsApi.statisticsByTemplate({
      startTime: dateRange.value[0],
      endTime: dateRange.value[1]
    })
    const data = res.data || []
    
    await nextTick()
    if (templateChartRef.value) {
      if (!templateChart) {
        templateChart = echarts.init(templateChartRef.value)
      }
      
      const option = {
        title: {
          text: '评估模板使用统计',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: data.map(item => item.templateName),
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          data: data.map(item => item.count),
          type: 'bar',
          itemStyle: {
            color: '#409EFF'
          }
        }]
      }
      templateChart.setOption(option)
    }
  } catch (error) {
    console.error('加载模板统计失败', error)
  }
}

const loadRiskStatistics = async () => {
  try {
    const res = await statisticsApi.statisticsByRiskLevel({
      startTime: dateRange.value[0],
      endTime: dateRange.value[1]
    })
    const data = res.data || []
    
    await nextTick()
    if (riskChartRef.value) {
      if (!riskChart) {
        riskChart = echarts.init(riskChartRef.value)
      }
      
      const riskColors = {
        '低风险': '#67C23A',
        '中风险': '#E6A23C',
        '高风险': '#F56C6C',
        '未知': '#909399'
      }
      
      const option = {
        title: {
          text: '风险等级分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}: {c} ({d}%)'
          },
          data: data.map(item => ({
            value: item.count,
            name: item.riskLevel,
            itemStyle: {
              color: riskColors[item.riskLevel] || '#909399'
            }
          }))
        }]
      }
      riskChart.setOption(option)
    }
  } catch (error) {
    console.error('加载风险统计失败', error)
  }
}

const loadRiskAlerts = async () => {
  try {
    const res = await statisticsApi.getRiskAlert({
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      baselineDays: 7,
      growthRateThreshold: 0.5,
      minIncrease: 5
    })
    riskAlerts.value = res.data || []
  } catch (error) {
    console.error('加载风险预警失败', error)
  }
}

const riskTagType = (level) => {
  if (!level) return 'info'
  if (level.includes('高')) return 'danger'
  if (level.includes('中')) return 'warning'
  if (level.includes('低')) return 'success'
  return 'info'
}

const handleTabChange = async (tabName) => {
  activeTab.value = tabName
  
  if (!dateRange.value || dateRange.value.length !== 2) {
    return
  }
  
  if (tabName === 'department') {
    await loadDepartmentStatistics()
  } else if (tabName === 'template') {
    try {
      const res = await statisticsApi.statisticsByTemplate({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1]
      })
      tableData.value = res.data || []
    } catch (error) {
      ElMessage.error('加载模板统计失败')
    }
  } else if (tabName === 'risk') {
    try {
      const res = await statisticsApi.statisticsByRiskLevel({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1]
      })
      tableData.value = res.data || []
    } catch (error) {
      ElMessage.error('加载风险统计失败')
    }
  }
}

const riskCount = computed(() => {
  if (overview.value.riskDistribution) {
    const riskDist = overview.value.riskDistribution
    if (typeof riskDist === 'object') {
      return riskDist['高风险'] || riskDist['HIGH'] || 0
    }
  }
  return 0
})

onMounted(() => {
  initDateRange()
  handleSearch()
  
  // 窗口大小改变时重新调整图表
  window.addEventListener('resize', () => {
    timeChart?.resize()
    departmentChart?.resize()
    templateChart?.resize()
    riskChart?.resize()
  })
})
</script>

<style scoped>
.statistics-dashboard {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 20px 0;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.alert-card :deep(.el-table) {
  font-size: 13px;
}

.card-sub-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sub-tip {
  color: #909399;
  font-size: 12px;
}
</style>
