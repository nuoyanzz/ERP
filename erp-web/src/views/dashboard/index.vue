<template>
  <div class="page-container">
    <!-- 欢迎横幅 -->
    <div class="banner">
      <div class="banner__decor banner__decor--1" />
      <div class="banner__decor banner__decor--2" />
      <div class="banner__content">
        <h1 class="banner__title">
          {{ greeting }}，{{ nickname || '管理员' }}
          <span class="banner__emoji">👋</span>
        </h1>
        <p class="banner__desc">
          统计区间：{{ stats?.periodLabel || '本月' }}
          <el-tag v-if="stats?.pendingAuditCount" type="warning" size="small" effect="light">
            {{ stats.pendingAuditCount }} 张单据待审核
          </el-tag>
        </p>
      </div>
      <div class="banner__side">
        <div class="banner__date">{{ today }}</div>
        <div class="banner__week">{{ week }}</div>
      </div>
    </div>

    <!-- 待我处理 -->
    <el-card v-if="stats?.todoItems?.length" shadow="never" class="panel todo-panel">
      <template #header>
        <div class="panel__header">
          <span class="section-title">待我处理</span>
          <span class="section-hint">点击卡片直达业务页面</span>
        </div>
      </template>
      <div class="todo-grid">
        <div
          v-for="item in stats.todoItems"
          :key="item.type"
          class="todo-card"
          @click="router.push(item.route)"
        >
          <div class="todo-card__count">{{ item.count }}</div>
          <div class="todo-card__title">{{ item.title }}</div>
        </div>
      </div>
    </el-card>

    <!-- 核心经营指标 -->
    <div class="stat-grid">
      <div class="stat-card stat-card--primary">
        <div class="stat-card__icon"><el-icon :size="22"><Money /></el-icon></div>
        <div class="stat-card__body">
          <span class="stat-card__label">本期销售额</span>
          <span class="stat-card__value">{{ formatAmount(stats?.salesAmount) }}</span>
        </div>
      </div>
      <div class="stat-card stat-card--success">
        <div class="stat-card__icon"><el-icon :size="22"><ShoppingCart /></el-icon></div>
        <div class="stat-card__body">
          <span class="stat-card__label">本期采购额</span>
          <span class="stat-card__value">{{ formatAmount(stats?.purchaseAmount) }}</span>
        </div>
      </div>
      <div class="stat-card stat-card--warning">
        <div class="stat-card__icon"><el-icon :size="22"><Box /></el-icon></div>
        <div class="stat-card__body">
          <span class="stat-card__label">库存总金额</span>
          <span class="stat-card__value">{{ formatAmount(stats?.stockAmount) }}</span>
        </div>
      </div>
      <div class="stat-card stat-card--danger">
        <div class="stat-card__icon"><el-icon :size="22"><Wallet /></el-icon></div>
        <div class="stat-card__body">
          <span class="stat-card__label">应收余额</span>
          <span class="stat-card__value">{{ formatAmount(stats?.receivableAmount) }}</span>
        </div>
      </div>
      <div class="stat-card stat-card--info">
        <div class="stat-card__icon"><el-icon :size="22"><Tickets /></el-icon></div>
        <div class="stat-card__body">
          <span class="stat-card__label">应付余额</span>
          <span class="stat-card__value">{{ formatAmount(stats?.payableAmount) }}</span>
        </div>
      </div>
      <div class="stat-card stat-card--purple">
        <div class="stat-card__icon"><el-icon :size="22"><SetUp /></el-icon></div>
        <div class="stat-card__body">
          <span class="stat-card__label">在制工单</span>
          <span class="stat-card__value">{{ stats?.wipMoCount ?? 0 }}</span>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 销售趋势 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel__header">
              <span class="section-title">本年销售趋势</span>
              <span class="section-hint">单位：元</span>
            </div>
          </template>
          <div v-if="trendPoints.length" class="chart">
            <div class="chart__bars">
              <div
                v-for="point in trendPoints"
                :key="point.label"
                class="chart__bar-item"
                :title="`${point.label}：${formatAmount(point.amount)}`"
              >
                <div class="chart__bar-value">{{ shortAmount(point.amount) }}</div>
                <div class="chart__bar" :style="{ height: barHeight(point.amount) }" />
                <div class="chart__bar-label">{{ point.label }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无销售数据" :image-size="80" />
        </el-card>
      </el-col>

      <!-- 库存 TOP -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel__header">
              <span class="section-title">库存金额 TOP</span>
              <span class="section-hint">前 {{ stats?.stockTop?.length || 0 }} 项</span>
            </div>
          </template>
          <div v-if="stats?.stockTop?.length" class="rank-list">
            <div
              v-for="(item, index) in stats.stockTop"
              :key="item.name"
              class="rank-item"
            >
              <span class="rank-item__index" :class="{ 'rank-item__index--top': index < 3 }">
                {{ index + 1 }}
              </span>
              <span class="rank-item__name">{{ item.name }}</span>
              <span class="rank-item__extra">{{ item.extra }}</span>
              <span class="rank-item__amount">{{ formatAmount(item.amount) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无库存数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 逾期应收 TOP -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel__header">
              <span class="section-title">逾期应收 TOP</span>
              <el-button link type="primary" @click="router.push('/ar/aging')">
                查看账龄分析<el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <div v-if="stats?.overdueTop?.length" class="rank-list">
            <div
              v-for="(item, index) in stats.overdueTop"
              :key="item.name"
              class="rank-item"
            >
              <span class="rank-item__index" :class="{ 'rank-item__index--top': index < 3 }">
                {{ index + 1 }}
              </span>
              <span class="rank-item__name">{{ item.name }}</span>
              <span class="rank-item__extra">{{ item.extra }}</span>
              <span class="rank-item__amount">{{ formatAmount(item.amount) }}</span>
            </div>
          </div>
          <el-empty v-else description="没有逾期应收，很好" :image-size="80" />
        </el-card>
      </el-col>

      <!-- 快捷入口 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel">
          <template #header>
            <span class="section-title">快捷入口</span>
          </template>
          <div class="quick-grid">
            <div
              v-for="action in shortcuts"
              :key="action.path"
              class="quick-item"
              @click="router.push(action.path)"
            >
              <el-icon :size="20" :color="action.color"><component :is="action.icon" /></el-icon>
              <span>{{ action.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Box,
  Money,
  SetUp,
  ShoppingCart,
  Tickets,
  Wallet
} from '@element-plus/icons-vue'
import { dashboardApi, type DashboardVO } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()
const nickname = computed(() => userStore.nickname)

const stats = ref<DashboardVO | null>(null)

const loadData = async () => {
  try {
    stats.value = await dashboardApi.overview()
  } catch (e) {
    ElMessage.error('加载统计数据失败')
  }
}

onMounted(loadData)

/**
 * 金额格式化：按万/亿缩写，便于卡片展示。
 */
const formatAmount = (value?: number) => {
  if (value === undefined || value === null) return '0.00'
  const num = Number(value)
  if (Math.abs(num) >= 100000000) return (num / 100000000).toFixed(2) + ' 亿'
  if (Math.abs(num) >= 10000) return (num / 10000).toFixed(2) + ' 万'
  return num.toFixed(2)
}

/**
 * 柱状图上方的短金额。
 */
const shortAmount = (value?: number) => {
  const num = Number(value || 0)
  if (Math.abs(num) >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toFixed(0)
}

const trendPoints = computed(() => stats.value?.salesTrend || [])

/**
 * 计算柱高百分比。最大值占满高度，其余按比例。
 */
const maxTrend = computed(() => {
  const values = trendPoints.value.map((p) => Number(p.amount || 0))
  return values.length ? Math.max(...values) : 0
})

const barHeight = (amount?: number) => {
  const max = maxTrend.value
  if (!max || max <= 0) return '2px'
  const ratio = Number(amount || 0) / max
  return Math.max(2, Math.round(ratio * 100)) + '%'
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const now = new Date()
const weekText = ['日', '一', '二', '三', '四', '五', '六']
const today = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
const week = `星期${weekText[now.getDay()]}`

const shortcuts = [
  { label: '采购订单', path: '/pa/order', color: '#2563eb' },
  { label: '销售订单', path: '/sa/order', color: '#059669' },
  { label: '库存查询', path: '/ic/stock', color: '#d97706' },
  { label: '记账凭证', path: '/fi/voucher', color: '#7c3aed' },
  { label: '应收账龄', path: '/ar/aging', color: '#dc2626' },
  { label: 'MRP 运算', path: '/pm/mrp', color: '#0891b2' }
]
</script>

<style scoped lang="scss">
.banner {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(120deg, #2563eb 0%, #4f46e5 55%, #7c3aed 100%);
  color: #fff;

  &__decor {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.12);
  }

  &__decor--1 {
    width: 200px;
    height: 200px;
    top: -80px;
    right: 60px;
  }

  &__decor--2 {
    width: 120px;
    height: 120px;
    bottom: -60px;
    right: 200px;
  }

  &__content {
    position: relative;
    z-index: 1;
  }

  &__title {
    margin: 0 0 6px;
    font-size: 22px;
    font-weight: 600;
  }

  &__desc {
    margin: 0;
    font-size: 13px;
    opacity: 0.92;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__side {
    position: relative;
    z-index: 1;
    text-align: right;
  }

  &__date {
    font-size: 15px;
    font-weight: 500;
  }

  &__week {
    font-size: 12px;
    opacity: 0.85;
  }
}

.todo-panel {
  margin-bottom: 16px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.todo-card {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--el-fill-color-blank);

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 10px rgb(0 0 0 / 8%);
    transform: translateY(-1px);
  }

  &__count {
    font-size: 22px;
    font-weight: 600;
    color: var(--el-color-primary);
    line-height: 1.2;
  }

  &__title {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 10px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 10px;
    flex-shrink: 0;
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  &__label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &--primary .stat-card__icon { background: rgb(37 99 235 / 12%); color: #2563eb; }
  &--success .stat-card__icon { background: rgb(5 150 105 / 12%); color: #059669; }
  &--warning .stat-card__icon { background: rgb(217 119 6 / 12%); color: #d97706; }
  &--danger .stat-card__icon { background: rgb(220 38 38 / 12%); color: #dc2626; }
  &--info .stat-card__icon { background: rgb(8 145 178 / 12%); color: #0891b2; }
  &--purple .stat-card__icon { background: rgb(124 58 237 / 12%); color: #7c3aed; }
}

.panel {
  margin-bottom: 16px;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chart {
  padding: 8px 0;

  &__bars {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    height: 220px;
  }

  &__bar-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    height: 100%;
    min-width: 0;
  }

  &__bar-value {
    font-size: 11px;
    color: var(--el-text-color-secondary);
    margin-bottom: 4px;
    white-space: nowrap;
  }

  &__bar {
    width: 100%;
    max-width: 32px;
    min-height: 2px;
    border-radius: 4px 4px 0 0;
    background: linear-gradient(180deg, #4f46e5 0%, #2563eb 100%);
    transition: height 0.3s;
  }

  &__bar-label {
    margin-top: 6px;
    font-size: 11px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: none;
  }

  &__index {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    background: var(--el-fill-color);
    color: var(--el-text-color-secondary);
    flex-shrink: 0;

    &--top {
      background: var(--el-color-primary);
      color: #fff;
    }
  }

  &__name {
    flex: 1;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__extra {
    font-size: 11px;
    color: var(--el-text-color-secondary);
    flex-shrink: 0;
  }

  &__amount {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    min-width: 90px;
    text-align: right;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;

  &:hover {
    border-color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
}
</style>








