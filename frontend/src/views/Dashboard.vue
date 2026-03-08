<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card stat-card-1">
          <div class="stat-item">
            <div class="stat-icon-wrapper">
              <div class="stat-icon icon-1"></div>
            </div>
            <div class="stat-value">{{ stats.patientCount }}</div>
            <div class="stat-label">患者总数</div>
          </div>
          <div class="card-glow"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-2">
          <div class="stat-item">
            <div class="stat-icon-wrapper">
              <div class="stat-icon icon-2"></div>
            </div>
            <div class="stat-value">{{ stats.assessmentCount }}</div>
            <div class="stat-label">评估记录</div>
          </div>
          <div class="card-glow"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-3">
          <div class="stat-item">
            <div class="stat-icon-wrapper">
              <div class="stat-icon icon-3"></div>
            </div>
            <div class="stat-value">{{ stats.templateCount }}</div>
            <div class="stat-label">评估模板</div>
          </div>
          <div class="card-glow"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-4">
          <div class="stat-item">
            <div class="stat-icon-wrapper">
              <div class="stat-icon icon-4"></div>
            </div>
            <div class="stat-value">{{ stats.completedCount }}</div>
            <div class="stat-label">已完成评估</div>
          </div>
          <div class="card-glow"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-banner-content">
        <div class="welcome-text">
          <h2 class="welcome-title">欢迎使用智安临评</h2>
          <p class="welcome-subtitle">结构化临床评估工具系统，助力精准临床决策</p>
        </div>
        <div class="welcome-decoration">
          <div class="decoration-circle circle-1"></div>
          <div class="decoration-circle circle-2"></div>
          <div class="decoration-circle circle-3"></div>
        </div>
      </div>
      <div class="welcome-pattern"></div>
    </div>

    <!-- 快速操作区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="action-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">快速操作</span>
            </div>
          </template>
          <div class="action-grid">
            <div class="action-item" @click="navigateTo('/patient/add')">
              <div class="action-icon action-icon-1">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="action-text">
                <div class="action-name">新增患者</div>
                <div class="action-desc">添加新的患者信息</div>
              </div>
            </div>
            <div v-if="canCreateAssessment" class="action-item" @click="navigateTo('/assessment/create')">
              <div class="action-icon action-icon-2">
                <el-icon><EditPen /></el-icon>
              </div>
              <div class="action-text">
                <div class="action-name">创建评估</div>
                <div class="action-desc">开始新的评估流程</div>
              </div>
            </div>
            <div class="action-item" @click="navigateTo('/template/add')">
              <div class="action-icon action-icon-3">
                <el-icon><Document /></el-icon>
              </div>
              <div class="action-text">
                <div class="action-name">新建模板</div>
                <div class="action-desc">创建评估模板</div>
              </div>
            </div>
            <div class="action-item" @click="navigateTo('/statistics')">
              <div class="action-icon action-icon-4">
                <el-icon><DataLine /></el-icon>
              </div>
              <div class="action-text">
                <div class="action-name">数据统计</div>
                <div class="action-desc">查看数据分析</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { statisticsApi } from '../api'

const router = useRouter()
const userStore = useUserStore()
const canCreateAssessment = computed(() => userStore.isAdmin || userStore.userRole === 'DOCTOR')

const stats = ref({
  patientCount: 0,
  assessmentCount: 0,
  templateCount: 0,
  completedCount: 0
})

const navigateTo = (path) => {
  router.push(path)
}

onMounted(async () => {
  try {
    const res = await statisticsApi.getDashboardStats()
    if (res.code === 200 && res.data) {
      stats.value = {
        patientCount: res.data.patientCount ?? 0,
        assessmentCount: res.data.assessmentCount ?? 0,
        templateCount: res.data.templateCount ?? 0,
        completedCount: res.data.completedCount ?? 0
      }
    }
  } catch (e) {
    console.error('获取首页统计数据失败', e)
  }
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
  background: linear-gradient(135deg, #f0f2f5 0%, #e8ebef 100%);
  min-height: calc(100vh - 60px);
}

/* 统计卡片基础样式 */
.stat-card {
  text-align: center;
  position: relative;
  overflow: hidden;
  border: none;
  border-radius: 16px;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  cursor: pointer;
  background: transparent;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  padding: 2px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.3), rgba(103, 194, 58, 0.3));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.3s;
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

/* 卡片1 - 蓝色科技感 */
.stat-card-1 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card-1:hover {
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.4);
}

/* 卡片2 - 青色科技感 */
.stat-card-2 {
  background: linear-gradient(135deg, #00c9ff 0%, #92fe9d 100%);
}

.stat-card-2:hover {
  box-shadow: 0 12px 40px rgba(0, 201, 255, 0.4);
}

/* 卡片3 - 橙色科技感 */
.stat-card-3 {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card-3:hover {
  box-shadow: 0 12px 40px rgba(245, 87, 108, 0.4);
}

/* 卡片4 - 绿色科技感 */
.stat-card-4 {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card-4:hover {
  box-shadow: 0 12px 40px rgba(79, 172, 254, 0.4);
}

/* 卡片内容 */
.stat-item {
  padding: 30px 20px;
  position: relative;
  z-index: 2;
}

.stat-icon-wrapper {
  margin-bottom: 15px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  position: relative;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.stat-card:hover .stat-icon {
  transform: rotate(360deg) scale(1.1);
  background: rgba(255, 255, 255, 0.3);
}

.icon-1::after {
  content: '👥';
  font-size: 24px;
}

.icon-2::after {
  content: '📊';
  font-size: 24px;
}

.icon-3::after {
  content: '📋';
  font-size: 24px;
}

.icon-4::after {
  content: '✅';
  font-size: 24px;
}

.stat-value {
  font-size: 42px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 12px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  letter-spacing: -1px;
  transition: all 0.3s;
  font-family: 'Arial', sans-serif;
}

.stat-card:hover .stat-value {
  transform: scale(1.1);
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.stat-label {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

/* 发光效果 */
.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.5s;
  pointer-events: none;
  z-index: 1;
}

.stat-card:hover .card-glow {
  opacity: 1;
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.6;
  }
}

/* 欢迎横幅 */
.welcome-banner {
  margin-top: 20px;
  height: 180px;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.3);
  transition: all 0.3s;
}

.welcome-banner:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 50px rgba(102, 126, 234, 0.4);
}

.welcome-banner-content {
  position: relative;
  z-index: 3;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 50px;
}

.welcome-text {
  flex: 1;
}

.welcome-title {
  margin: 0 0 15px 0;
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  letter-spacing: 1px;
}

.welcome-subtitle {
  margin: 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 400;
  letter-spacing: 0.5px;
}

.welcome-decoration {
  position: relative;
  width: 200px;
  height: 200px;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 80px;
  height: 80px;
  top: 0;
  right: 0;
  animation-delay: 0s;
}

.circle-2 {
  width: 60px;
  height: 60px;
  top: 40px;
  right: 60px;
  animation-delay: 2s;
}

.circle-3 {
  width: 40px;
  height: 40px;
  top: 80px;
  right: 20px;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translateY(-20px) scale(1.1);
    opacity: 0.9;
  }
}

.welcome-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
  opacity: 0.5;
  z-index: 1;
}

/* 快速操作卡片 */
.action-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  background: #ffffff;
}

.action-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  position: relative;
  padding-left: 12px;
}

.card-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.action-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-color: rgba(102, 126, 234, 0.3);
  transform: translateX(5px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.2);
}

.action-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: #ffffff;
  transition: all 0.3s;
}

.action-icon-1 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.action-icon-2 {
  background: linear-gradient(135deg, #00c9ff 0%, #92fe9d 100%);
}

.action-icon-3 {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.action-icon-4 {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.action-item:hover .action-icon {
  transform: scale(1.1) rotate(5deg);
}

.action-text {
  flex: 1;
}

.action-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.action-desc {
  font-size: 13px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stat-value {
    font-size: 32px;
  }
  
  .stat-card {
    margin-bottom: 15px;
  }
}

/* Element Plus 卡片样式覆盖 */
:deep(.el-card__body) {
  padding: 0;
}
</style>

