import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'patient',
        name: 'PatientList',
        component: () => import('../views/patient/PatientList.vue'),
        meta: { title: '患者管理' }
      },
      {
        path: 'patient/add',
        name: 'PatientAdd',
        component: () => import('../views/patient/PatientForm.vue'),
        meta: { title: '新增患者' }
      },
      {
        path: 'patient/edit/:id',
        name: 'PatientEdit',
        component: () => import('../views/patient/PatientForm.vue'),
        meta: { title: '编辑患者' }
      },
      {
        path: 'template',
        name: 'TemplateList',
        component: () => import('../views/template/TemplateList.vue'),
        meta: { title: '评估模板管理' }
      },
      {
        path: 'template/add',
        name: 'TemplateAdd',
        component: () => import('../views/template/TemplateForm.vue'),
        meta: { title: '新增模板', requiresAdmin: true }
      },
      {
        path: 'template/edit/:id',
        name: 'TemplateEdit',
        component: () => import('../views/template/TemplateForm.vue'),
        meta: { title: '编辑模板', requiresAdmin: true }
      },
      {
        path: 'template/detail/:id',
        name: 'TemplateDetail',
        component: () => import('../views/template/TemplateDetail.vue'),
        meta: { title: '模板详情' }
      },
      {
        path: 'template/versions/:templateCode',
        name: 'TemplateVersions',
        component: () => import('../views/template/TemplateVersions.vue'),
        meta: { title: '模板版本管理' }
      },
      {
        path: 'rule',
        name: 'RuleList',
        component: () => import('../views/rule/RuleList.vue'),
        meta: { title: '评估规则管理' }
      },
      {
        path: 'rule/form',
        name: 'RuleForm',
        component: () => import('../views/rule/RuleForm.vue'),
        meta: { title: '规则配置' }
      },
      {
        path: 'assessment/create',
        name: 'AssessmentCreate',
        component: () => import('../views/assessment/AssessmentForm.vue'),
        meta: { title: '创建评估', requiresDoctorOrAdmin: true }
      },
      {
        path: 'assessment/edit/:id',
        name: 'AssessmentEdit',
        component: () => import('../views/assessment/AssessmentForm.vue'),
        meta: { title: '编辑评估', requiresDoctorOrAdmin: true }
      },
      {
        path: 'assessment/history/:patientId',
        name: 'AssessmentHistory',
        component: () => import('../views/assessment/AssessmentHistory.vue'),
        meta: { title: '评估历史' }
      },
      {
        path: 'department',
        name: 'DepartmentList',
        component: () => import('../views/department/DepartmentList.vue'),
        meta: { title: '科室管理', requiresAdmin: true }
      },
      {
        path: 'diagnosis',
        name: 'DiagnosisList',
        component: () => import('../views/diagnosis/DiagnosisList.vue'),
        meta: { title: '诊断管理' }
      },
      {
        path: 'diagnosis/detail/:id',
        name: 'DiagnosisDetail',
        component: () => import('../views/diagnosis/DiagnosisDetail.vue'),
        meta: { title: '诊断详情' }
      },
      {
        path: 'diagnosis/suggestion-history/:patientId',
        name: 'SuggestionHistory',
        component: () => import('../views/diagnosis/SuggestionHistory.vue'),
        meta: { title: '诊疗建议历史' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/statistics/StatisticsDashboard.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'operation-log',
        name: 'OperationLog',
        component: () => import('../views/log/OperationLogList.vue'),
        meta: { title: '操作日志', requiresAdmin: true }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('../views/user/UserList.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    const restored = await userStore.tryRestoreSession()
    if (!restored) {
      next('/login')
      return
    }
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/dashboard')
    return
  }
  if (to.meta.requiresDoctorOrAdmin && !userStore.isAdmin && userStore.userRole !== 'DOCTOR') {
    next('/dashboard')
    return
  }
  next()
})

export default router

