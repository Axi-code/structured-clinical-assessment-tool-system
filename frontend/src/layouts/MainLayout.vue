<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">
          <el-icon><Operation /></el-icon>
        </div>
        <div class="logo-text">
          <h2>智安临评</h2>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/patient">
          <el-icon><User /></el-icon>
          <span>患者管理</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/department">
          <el-icon><Grid /></el-icon>
          <span>科室管理</span>
        </el-menu-item>
        <el-menu-item index="/diagnosis">
          <el-icon><Reading /></el-icon>
          <span>诊断管理</span>
        </el-menu-item>
        <el-menu-item index="/template">
          <el-icon><Document /></el-icon>
          <span>评估模板</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'" index="/rule">
          <el-icon><Operation /></el-icon>
          <span>评估规则</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin || userStore.userRole === 'DOCTOR'" index="/assessment/create">
          <el-icon><EditPen /></el-icon>
          <span>创建评估</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><DataLine /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/operation-log">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/user">
          <el-icon><Setting /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="page-title">
            <el-icon class="title-icon"><component :is="getPageIcon()" /></el-icon>
            <span class="title-text">{{ pageTitle }}</span>
          </div>
          <div class="breadcrumb-wrapper">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.meta?.title && route.path !== '/dashboard'">
                {{ route.meta.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="header-center">
          <div class="time-display">
            <el-icon><Clock /></el-icon>
            <span>{{ currentTime }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-actions">
            <el-tooltip content="全屏" placement="bottom">
              <div class="action-btn" @click="toggleFullscreen">
                <el-icon><FullScreen /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="刷新" placement="bottom">
              <div class="action-btn" @click="refreshPage">
                <el-icon><Refresh /></el-icon>
              </div>
            </el-tooltip>
          </div>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><Avatar /></el-icon>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { 
  House, User, Reading, Document, Operation, EditPen, DataLine, Setting,
  Clock, FullScreen, Refresh, Avatar, ArrowDown, SwitchButton, Grid
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '首页')
const currentTime = ref('')

let timeInterval = null

// 获取页面图标
const getPageIcon = () => {
  const iconMap = {
    '/dashboard': House,
    '/patient': User,
    '/diagnosis': Reading,
    '/template': Document,
    '/rule': Operation,
    '/assessment': EditPen,
    '/statistics': DataLine,
    '/user': Setting,
    '/operation-log': Document,
    '/department': Grid
  }
  
  for (const [path, icon] of Object.entries(iconMap)) {
    if (route.path.startsWith(path)) {
      return icon
    }
  }
  return House
}

// 更新时间
const updateTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  
  currentTime.value = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(err => {
      ElMessage.warning('无法进入全屏模式')
    })
  } else {
    document.exitFullscreen()
  }
}

// 刷新页面
const refreshPage = () => {
  router.go(0)
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f7 50%, #e4ebf5 100%);
}

.sidebar {
  background:
    radial-gradient(circle at 0% 0%, #3b5776 0, transparent 45%),
    linear-gradient(180deg, #273447 0%, #1f2836 100%);
  overflow-y: auto;
  box-shadow: 4px 0 18px rgba(15, 23, 42, 0.45);
  border-right: 1px solid rgba(148, 163, 184, 0.2);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #e5f0ff;
  background: linear-gradient(135deg, #2f3f5b 0%, #202a3a 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.4);
  flex-shrink: 0;
}

.logo-icon .el-icon {
  font-size: 20px;
  color: #ffffff;
}

.logo-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.logo h2 {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  line-height: 1.4;
  margin: 0;
  color: #e5f0ff;
  white-space: normal;
  word-break: break-all;
}

/* 左侧菜单细节，保持稳重但有科技感 */
:deep(.el-menu) {
  background-color: transparent;
  border-right: none;
}

:deep(.el-menu-item) {
  height: 44px;
  margin: 4px 10px;
  border-radius: 10px;
  padding-left: 18px !important;
  transition: all 0.25s ease;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 8px;
  font-size: 18px;
}

:deep(.el-menu-item:hover) {
  background: rgba(148, 163, 184, 0.18);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.35);
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #ffffff;
}

.header {
  background: linear-gradient(135deg, #ffffff 0%, #f5f9ff 40%, #eef6ff 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.08);
  position: relative;
  z-index: 2;
  height: 64px;
}

.header::before {
  content: '';
  position: absolute;
  inset: 0;
  border-bottom: 2px solid transparent;
  border-image: linear-gradient(90deg, #4facfe, #00f2fe) 1;
  opacity: 0.55;
  pointer-events: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.1) 0%, rgba(0, 242, 254, 0.1) 100%);
  border-radius: 10px;
  border-left: 3px solid #4facfe;
}

.title-icon {
  font-size: 20px;
  color: #4facfe;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.5px;
}

.breadcrumb-wrapper {
  display: flex;
  align-items: center;
}

:deep(.el-breadcrumb) {
  font-size: 14px;
}

:deep(.el-breadcrumb__inner) {
  color: #64748b;
  font-weight: 500;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #4facfe;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 20px;
}

.time-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  font-size: 14px;
  color: #475569;
  font-weight: 500;
  font-family: 'Courier New', monospace;
}

.time-display .el-icon {
  color: #4facfe;
  font-size: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
  color: #64748b;
}

.action-btn:hover {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-color: #4facfe;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.3);
}

.action-btn .el-icon {
  font-size: 18px;
}

.user-info {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: #374151;
  padding: 8px 16px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 10px rgba(148, 163, 184, 0.4);
  border: 1px solid rgba(148, 163, 184, 0.6);
  transition: all 0.2s ease;
  gap: 8px;
}

.user-info:hover {
  box-shadow: 0 6px 14px rgba(148, 163, 184, 0.6);
  transform: translateY(-1px);
  border-color: #4facfe;
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.1) 0%, rgba(0, 242, 254, 0.1) 100%);
}

.user-info .el-icon {
  color: #4facfe;
}

.username {
  font-weight: 500;
  color: #1e293b;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-content {
  background: radial-gradient(circle at top, #ffffff 0%, #f3f5fb 45%, #e7edf8 100%);
  padding: 20px;
}
</style>

