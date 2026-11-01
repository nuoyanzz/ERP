<template>
  <div class="page-container dash">
    <!-- 顶部信息条 -->
    <div class="dash-bar">
      <span class="dash-bar__title">{{ nickname || '管理员' }}</span>
      <span class="dash-bar__sep" />
      <span class="dash-bar__item">统计区间：{{ stats?.periodLabel || '本月' }}</span>
      <el-tag v-if="stats?.pendingAuditCount" type="warning" size="small" effect="plain">
        {{ stats.pendingAuditCount }} 张单据待审核
      </el-tag>
      <div class="dash-bar__spacer" />
      <span class="dash-bar__item">{{ today }} {{ week }}</span>
    </div>

    <!-- 待我处理 -->
    <el-card v-if="stats?.todoItems?.length" shadow="never" class="panel">
      <template #header>
        <span class="section-title">待我处理</span>
        <span class="section-hint">点击直达业务页面</span>
      </template>
      <div class="todo-grid">
        <div
          v-for="item in stats.todoItems"
          :key="item.type"
          class="todo-card"
          @click="router.push(item.route)"
        >
          <span class="todo-card__count">{{ item.count }}</span>
          <span class="todo-card__title">{{ item.title }}</span>
        </div>
      </div>
    </el-card>

    <!-- 核心经营指标 -->
    <el-card shadow="never" class="panel">
      <template #header>
        <span class="section-title">核心经营指标</span>
      </template>
      <table class="metric-table">
        <tbody>
          <tr>
            <td class="metric-table__label">本期销售额</td>
            <td class="metric-table__value">{{ formatAmount(stats?.salesAmount) }}</td>
            <td class="metric-table__label">本期采购额</td>
            <td class="metric-table__value">{{ formatAmount(stats?.purchaseAmount) }}</td>
            <td class="metric-table__label">库存总金额</td>
            <td class="metric-table__value">{{ formatAmount(stats?.stockAmount) }}</td>
          </tr>
          <tr>
            <td class="metric-table__label">应收余额</td>
            <td class="metric-table__value">{{ formatAmount(stats?.receivableAmount) }}</td>
            <td class="metric-table__label">应付余额</td>
            <td class="metric-table__value">{{ formatAmount(stats?.payableAmount) }}</td>
            <td class="metric-table__label">在制工单</td>
            <td class="metric-table__value">{{ stats?.wipMoCount ?? 0 }}</td>
          </tr>
        </tbody>
      </table>
    </el-card>

    <el-row :gutter="8">
      <!-- 销售趋势 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <span class="section-title">本年销售趋势</span>
            <span class="section-hint">单位：元</span>
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
          <el-empty v-else description="暂无销售数据" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 库存 TOP -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <span class="section-title">库存金额 TOP</span>
            <span class="section-hint">前 {{ stats?.stockTop?.length || 0 }} 项</span>
          </template>
          <div v-if="stats?.stockTop?.length" class="rank-list">
            <div
              v-for="(item, index) in stats.stockTop"
              :key="item.name"
              class="rank-item"
            >
              <span class="rank-item__index">{{ index + 1 }}</span>
              <span class="rank-item__name">{{ item.name }}</span>
              <span class="rank-item__extra">{{ item.extra }}</span>
              <span class="rank-item__amount">{{ formatAmount(item.amount) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无库存数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="8">
      <!-- 逾期应收 TOP -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel">
          <template #header>
            <span class="section-title">逾期应收 TOP</span>
            <el-button link type="primary" size="small" @click="router.push('/ar/aging')">
              查看账龄分析<el-icon><ArrowRight /></el-icon>
            </el-button>
          </template>
          <div v-if="stats?.overdueTop?.length" class="rank-list">
            <div
              v-for="(item, index) in stats.overdueTop"
              :key="item.name"
              class="rank-item"
            >
              <span class="rank-item__index">{{ index + 1 }}</span>
              <span class="rank-item__name">{{ item.name }}</span>
              <span class="rank-item__extra">{{ item.extra }}</span>
              <span class="rank-item__amount">{{ formatAmount(item.amount) }}</span>
            </div>
          </div>
          <el-empty v-else description="没有逾期应收" :image-size="60" />
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
              {{ action.label }}
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
import { ArrowRight } from '@element-plus/icons-vue'
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
 * 金额格式化：按万/亿缩写，便于展示。
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

const now = new Date()
const weekText = ['日', '一', '二', '三', '四', '五', '六']
const today = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
const week = `星期${weekText[now.getDay()]}`

const shortcuts = [
  { label: '采购订单', path: '/pa/order' },
  { label: '销售订单', path: '/sa/order' },
  { label: '库存查询', path: '/ic/stock' },
  { label: '记账凭证', path: '/fi/voucher' },
  { label: '应收账龄', path: '/ar/aging' },
  { label: 'MRP 运算', path: '/pm/mrp' }
]
</script>

<style scoped lang="scss">
/* ============================================================
   工作台 —— 金蝶风格
   平面数据区：表格化的指标行，无渐变、无彩色图标底
   ============================================================ */

.dash {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ---------- 顶部信息条 ---------- */
.dash-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 32px;
  padding: 0 10px;
  background-color: #fff;
  border: 1px solid var(--el-border-color-light);
  border-left: 3px solid var(--el-color-primary);
  font-size: 13px;
}

.dash-bar__title {
  font-size: 14px;
  font-weight: 600;
}

.dash-bar__sep {
  width: 1px;
  height: 14px;
  background-color: var(--el-border-color);
}

.dash-bar__item {
  color: var(--el-text-color-secondary);
}

.dash-bar__spacer {
  flex: 1;
}

/* ---------- 面板 ---------- */
.panel {
  margin-bottom: 0;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-hint {
  float: right;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

/* ---------- 待我处理 ---------- */
.todo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.todo-card {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 160px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  background-color: var(--el-fill-color-light);
  cursor: pointer;
  transition: border-color 0.15s;
}

.todo-card:hover {
  border-color: var(--el-color-primary);
  background-color: #e8f0fb;
}

.todo-card__count {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.todo-card__title {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

/* ---------- 指标表 ---------- */
/* 用表格排布指标，比卡片更接近金蝶的账表观感 */
.metric-table {
  width: 100%;
  border-collapse: collapse;
}

.metric-table td {
  padding: 7px 10px;
  border: 1px solid var(--el-border-color-lighter);
}

.metric-table__label {
  width: 110px;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 13px;
  white-space: nowrap;
}

.metric-table__value {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  text-align: right;
  white-space: nowrap;
}

/* ---------- 柱状图 ---------- */
.chart {
  padding: 4px 0;
}

.chart__bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 190px;
}

.chart__bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  min-width: 0;
}

.chart__bar-value {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  margin-bottom: 2px;
}

.chart__bar {
  width: 70%;
  max-width: 26px;
  background-color: var(--el-color-primary);
  border-radius: 0;
  transition: background-color 0.15s;
}

.chart__bar-item:hover .chart__bar {
  background-color: var(--el-color-primary-light-3);
}

.chart__bar-label {
  margin-top: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

/* ---------- 排行榜 ---------- */
.rank-list {
  border: 1px solid var(--el-border-color-lighter);
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-item__index {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.rank-item__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-item__extra {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.rank-item__amount {
  min-width: 90px;
  text-align: right;
  font-weight: 600;
  white-space: nowrap;
}

/* ---------- 快捷入口 ---------- */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 6px;
}

.quick-item {
  padding: 8px 4px;
  border: 1px solid var(--el-border-color-light);
  background-color: var(--el-fill-color-light);
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.15s;
}

.quick-item:hover {
  border-color: var(--el-color-primary);
  background-color: #e8f0fb;
  color: var(--el-color-primary);
}
</style>
