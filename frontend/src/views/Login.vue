<template>
  <div class="login-page">
    <div class="login-panel">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon :size="48"><Document /></el-icon>
          </div>
          <h1 class="brand-title">智安临评</h1>
          <p class="brand-subtitle">结构化临床评估工具系统，助力精准临床决策</p>
          <div class="brand-features">
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>标准化评估流程</span>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>多维度风险分析</span>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>数据驱动决策支持</span>
            </div>
          </div>
        </div>
        <div class="brand-pattern"></div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="form-section">
        <div class="form-wrapper">
          <div class="form-header">
            <h2 class="form-title">欢迎登录</h2>
            <p class="form-desc">请输入您的账号信息以访问系统</p>
          </div>
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            class="login-form"
            size="large"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item v-if="needCaptcha" label="验证码" prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="loginForm.captchaCode"
                  placeholder="请输入验证码"
                  maxlength="4"
                  @keyup.enter="handleLogin"
                />
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  class="captcha-img"
                  alt="验证码"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Document, User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { userApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref(null)
const loading = ref(false)
const needCaptcha = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

watch(needCaptcha, (val) => {
  if (val) {
    loginRules.captchaCode = [{ required: true, message: '请输入验证码', trigger: 'blur' }]
    fetchCaptcha()
  } else {
    delete loginRules.captchaCode
    loginForm.captchaKey = ''
    loginForm.captchaCode = ''
    captchaImage.value = ''
    captchaKey.value = ''
  }
})

async function fetchCaptcha() {
  try {
    const res = await userApi.getCaptcha()
    if (res.data && res.data.captchaKey && res.data.captchaImage) {
      captchaKey.value = res.data.captchaKey
      loginForm.captchaKey = res.data.captchaKey
      captchaImage.value = res.data.captchaImage
    }
  } catch (e) {
    ElMessage.error('获取验证码失败')
  }
}

function refreshCaptcha() {
  if (needCaptcha.value) fetchCaptcha()
}

const handleLogin = async () => {
  if (needCaptcha.value) {
    loginForm.captchaKey = captchaKey.value
  }
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload = { username: loginForm.username, password: loginForm.password }
        if (needCaptcha.value) {
          payload.captchaKey = loginForm.captchaKey
          payload.captchaCode = loginForm.captchaCode
        }
        await userStore.login(payload)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (error) {
        if (error.code === 400 && error.message === '请完成验证码') {
          needCaptcha.value = true
          ElMessage.warning('请完成验证码后再登录')
        } else {
          ElMessage.error(error.message || '登录失败')
        }
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f2f5 0%, #e8ebef 100%);
}

.login-panel {
  display: flex;
  width: 920px;
  min-height: 560px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08), 0 4px 20px rgba(79, 172, 254, 0.06);
  overflow: hidden;
}

/* 左侧品牌区 - 与侧边栏风格一致 */
.brand-section {
  flex: 1;
  min-width: 380px;
  background:
    radial-gradient(circle at 0% 0%, #3b5776 0, transparent 45%),
    linear-gradient(180deg, #273447 0%, #1f2836 100%);
  padding: 48px 40px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid rgba(148, 163, 184, 0.15);
}

.brand-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 80%, rgba(79, 172, 254, 0.08) 0%, transparent 50%),
                    radial-gradient(circle at 80% 20%, rgba(0, 242, 254, 0.06) 0%, transparent 40%);
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 32px;
  box-shadow: 0 6px 18px rgba(79, 172, 254, 0.4);
}

.brand-title {
  font-size: 26px;
  font-weight: 600;
  color: #e5f0ff;
  line-height: 1.35;
  margin: 0 0 16px;
  letter-spacing: 0.5px;
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(229, 240, 255, 0.75);
  line-height: 1.6;
  margin: 0 0 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(229, 240, 255, 0.9);
}

.feature-dot {
  width: 6px;
  height: 6px;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  border-radius: 50%;
  flex-shrink: 0;
}

/* 右侧表单区 */
.form-section {
  flex: 1;
  min-width: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.form-wrapper {
  width: 100%;
  max-width: 340px;
}

.form-header {
  margin-bottom: 36px;
  padding-left: 16px;
  border-left: 4px solid #4facfe;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.form-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e5e7eb;
  padding: 4px 15px;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #94a3b8;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4facfe;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  box-shadow: 0 6px 18px rgba(79, 172, 254, 0.35);
}

.login-btn:hover {
  background: linear-gradient(135deg, #3d9aed 0%, #00d9e6 100%);
  box-shadow: 0 8px 24px rgba(79, 172, 254, 0.45);
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.captcha-img:hover {
  border-color: #4facfe;
}

/* 响应式 */
@media (max-width: 900px) {
  .login-panel {
    flex-direction: column;
    width: 100%;
    max-width: 440px;
    min-height: auto;
  }

  .brand-section {
    min-width: auto;
    padding: 36px 32px;
  }

  .brand-title {
    font-size: 22px;
  }

  .brand-features {
    display: none;
  }

  .form-section {
    min-width: auto;
    padding: 36px 32px;
  }
}
</style>
