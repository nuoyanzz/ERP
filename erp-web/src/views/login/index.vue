<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-hero">
      <div class="login-hero__decor login-hero__decor--1" />
      <div class="login-hero__decor login-hero__decor--2" />
      <div class="login-hero__decor login-hero__decor--3" />
      <div class="login-hero__content">
        <div class="login-hero__logo">
          <el-icon :size="40"><OfficeBuilding /></el-icon>
        </div>
        <h1 class="login-hero__title">ERP 管理系统</h1>
        <p class="login-hero__subtitle">企业资源计划平台</p>
        <ul class="login-hero__features">
          <li><el-icon><ShoppingCart /></el-icon><span>采购 · 销售 · 库存一体化</span></li>
          <li><el-icon><TrendCharts /></el-icon><span>财务核算与实时报表</span></li>
          <li><el-icon><SetUp /></el-icon><span>生产制造与流程协同</span></li>
        </ul>
      </div>
      <p class="login-hero__footer">Enterprise Resource Planning · v1.0</p>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="login-panel">
      <div class="login-box">
        <div class="login-box__header">
          <h2>欢迎登录</h2>
          <p>请输入账号密码以继续</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <div class="login-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>默认账号：admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  InfoFilled,
  Lock,
  OfficeBuilding,
  SetUp,
  ShoppingCart,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100%;
  min-height: 520px;
  background: var(--ep-bg-page);
}

/* ---------- 左侧品牌区 ---------- */
.login-hero {
  position: relative;
  flex: 1.1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 64px;
  background: linear-gradient(150deg, #0b1b33 0%, #14294b 45%, #1d4ed8 130%);
  color: #fff;
  overflow: hidden;
}

.login-hero__decor {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.35;
}

.login-hero__decor--1 {
  width: 420px;
  height: 420px;
  top: -140px;
  right: -120px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.55), transparent 70%);
}

.login-hero__decor--2 {
  width: 320px;
  height: 320px;
  bottom: -100px;
  left: -80px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.35), transparent 70%);
}

.login-hero__decor--3 {
  width: 220px;
  height: 220px;
  top: 40%;
  left: 45%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12), transparent 70%);
}

.login-hero__content {
  position: relative;
  z-index: 1;
}

.login-hero__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
}

.login-hero__title {
  margin: 0;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 2px;
}

.login-hero__subtitle {
  margin: 8px 0 40px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 6px;
}

.login-hero__features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.login-hero__features li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.86);
}

.login-hero__features .el-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 17px;
  flex-shrink: 0;
}

.login-hero__footer {
  position: absolute;
  bottom: 28px;
  left: 64px;
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 1px;
}

/* ---------- 右侧登录卡片 ---------- */
.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.login-box {
  width: 100%;
  max-width: 380px;
  padding: 40px 36px 32px;
  background: var(--ep-bg-card);
  border-radius: var(--ep-radius-lg);
  box-shadow: var(--ep-shadow-lg);
  animation: box-in 0.4s ease both;
}

@keyframes box-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
}

.login-box__header {
  margin-bottom: 28px;
}

.login-box__header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--ep-text-1);
}

.login-box__header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ep-text-3);
}

.login-btn {
  width: 100%;
  margin-top: 4px;
  font-weight: 600;
  letter-spacing: 4px;
}

.login-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  font-size: 12px;
  color: var(--ep-text-3);
}

/* ---------- 响应式：窄屏隐藏品牌区 ---------- */
@media (max-width: 900px) {
  .login-hero {
    display: none;
  }

  .login-panel {
    background: linear-gradient(150deg, #0b1b33, #1d4ed8 130%);
  }
}
</style>








