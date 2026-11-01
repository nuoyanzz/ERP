<template>
  <div class="login-page">
    <!-- 顶部品牌条 -->
    <div class="login-top">
      <div class="login-top__brand">
        <el-icon :size="20"><OfficeBuilding /></el-icon>
        <span class="login-top__title">ERP 管理系统</span>
      </div>
      <span class="login-top__sub">企业资源计划平台</span>
    </div>

    <!-- 登录表单 -->
    <div class="login-body">
      <div class="login-box">
        <div class="login-box__header">
          <h2>用户登录</h2>
          <p>请输入账号密码以继续</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="72px" @keyup.enter="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item>
            <el-button class="login-btn" type="primary" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>默认账号：admin / admin123</span>
        </div>
      </div>

      <div class="login-foot">
        <span>采购 · 销售 · 库存一体化</span>
        <span>财务核算与实时报表</span>
        <span>生产制造与流程协同</span>
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
/* ============================================================
   登录页 —— 金蝶风格
   平面：顶部品牌条 + 居中表单，无渐变、无装饰圆球
   ============================================================ */

.login-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 480px;
  background-color: #f2f5f9;
}

/* ---------- 顶部品牌条 ---------- */
.login-top {
  display: flex;
  align-items: baseline;
  gap: 12px;
  height: 44px;
  padding: 0 20px;
  background-color: #1a5fb4;
  color: #fff;
}

.login-top__brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.login-top__title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.login-top__sub {
  font-size: 12px;
  color: rgb(255 255 255 / 70%);
}

/* ---------- 表单区 ---------- */
.login-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.login-box {
  width: 100%;
  max-width: 340px;
  padding: 24px 28px 20px;
  background-color: #fff;
  border: 1px solid var(--el-border-color);
  border-top: 3px solid var(--el-color-primary);
}

.login-box__header {
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.login-box__header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.login-box__header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.login-btn {
  width: 100%;
  font-weight: 500;
}

.login-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* ---------- 底部特性 ---------- */
.login-foot {
  display: flex;
  gap: 24px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
