<template>
  <div class="page-container report-page">
    <el-card shadow="never" class="report-card">
      <template #header>
        <div class="header">
          <div class="header-left">
            <el-radio-group v-model="activeTab" @change="loadData">
              <el-radio-button value="balance-sheet">资产负债表</el-radio-button>
              <el-radio-button value="income-statement">利润表</el-radio-button>
            </el-radio-group>
            <span class="unit-tip">单位：元</span>
          </div>
          <div class="header-right">
            <el-date-picker
              v-model="period"
              type="month"
              value-format="YYYY-MM"
              placeholder="选择月份"
              style="width: 140px"
              @change="loadData"
            />
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <!-- 资产负债表 -->
        <template v-if="activeTab === 'balance-sheet'">
          <el-row :gutter="16" class="stat-row">
            <el-col :xs="24" :sm="8">
              <div class="stat-card stat-card--asset">
                <div class="stat-card__icon"><el-icon><Wallet /></el-icon></div>
                <div class="stat-card__body">
                  <span class="stat-card__label">资产总计</span>
                  <span class="stat-card__value">{{ sheet?.assetTotal ?? '0.00' }}</span>
                </div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="stat-card stat-card--liability">
                <div class="stat-card__icon"><el-icon><TrendCharts /></el-icon></div>
                <div class="stat-card__body">
                  <span class="stat-card__label">负债及所有者权益总计</span>
                  <span class="stat-card__value">{{ liabilityEquityTotal }}</span>
                </div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="stat-card" :class="sheet?.balanced ? 'stat-card--ok' : 'stat-card--bad'">
                <div class="stat-card__icon">
                  <el-icon><CircleCheck v-if="sheet?.balanced" /><CircleClose v-else /></el-icon>
                </div>
                <div class="stat-card__body">
                  <span class="stat-card__label">平衡校验</span>
                  <span class="stat-card__value stat-card__value--text">
                    {{ sheet?.balanced ? '报表平衡' : '报表不平衡' }}
                  </span>
                </div>
              </div>
            </el-col>
          </el-row>

          <div v-if="sheet" class="report-title">
            <span class="report-title__main">资产负债表</span>
            <span class="report-title__sub">{{ sheet.period }}　单位：元</span>
          </div>
          <el-empty v-else description="暂无报表数据" />

          <el-row v-if="sheet" :gutter="20">
            <el-col :xs="24" :md="12">
              <div v-for="section in assetSections" :key="section.title" class="report-table-wrap">
                <table class="report-table">
                  <thead>
                    <tr>
                      <th class="col-item">资产</th>
                      <th class="col-num">期末数</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr class="row-group">
                      <td class="cell-group" colspan="2">{{ section.title }}</td>
                    </tr>
                    <tr v-for="line in section.lines" :key="line.accountCode">
                      <td>
                        <span class="account-code">{{ line.accountCode }}</span>{{ line.accountName }}
                      </td>
                      <td class="num">{{ line.balance }}</td>
                    </tr>
                    <tr class="row-total">
                      <td>{{ section.title }}合计</td>
                      <td class="num">{{ section.subtotal }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="grand-total">
                资产总计：<span class="grand-total__num">{{ sheet.assetTotal }}</span>
              </div>
            </el-col>

            <el-col :xs="24" :md="12">
              <div v-for="section in liabEquitySections" :key="section.title" class="report-table-wrap">
                <table class="report-table">
                  <thead>
                    <tr>
                      <th class="col-item">负债和所有者权益</th>
                      <th class="col-num">期末数</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr class="row-group">
                      <td class="cell-group" colspan="2">{{ section.title }}</td>
                    </tr>
                    <tr v-for="line in section.lines" :key="line.accountCode">
                      <td>
                        <span class="account-code">{{ line.accountCode }}</span>{{ line.accountName }}
                      </td>
                      <td class="num">{{ line.balance }}</td>
                    </tr>
                    <tr class="row-total">
                      <td>{{ section.title }}合计</td>
                      <td class="num">{{ section.subtotal }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="grand-total">
                负债及所有者权益总计：<span class="grand-total__num">{{ liabilityEquityTotal }}</span>
              </div>
            </el-col>
          </el-row>

          <el-alert
            v-if="sheet"
            class="balance-alert"
            :type="sheet.balanced ? 'success' : 'error'"
            :closable="false"
            show-icon
            :title="
              sheet.balanced
                ? '资产 = 负债 + 所有者权益，报表平衡'
                : '报表不平衡，请检查凭证与期末结转'
            "
          />
        </template>

        <!-- 利润表 -->
        <template v-else>
          <el-row :gutter="16" class="stat-row">
            <el-col :xs="24" :sm="8">
              <div class="stat-card stat-card--asset">
                <div class="stat-card__icon"><el-icon><Money /></el-icon></div>
                <div class="stat-card__body">
                  <span class="stat-card__label">营业利润</span>
                  <span class="stat-card__value">{{ income?.operatingProfit ?? '0.00' }}</span>
                </div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="stat-card stat-card--liability">
                <div class="stat-card__icon"><el-icon><TrendCharts /></el-icon></div>
                <div class="stat-card__body">
                  <span class="stat-card__label">利润总额</span>
                  <span class="stat-card__value">{{ income?.totalProfit ?? '0.00' }}</span>
                </div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="stat-card" :class="income && Number(income.netProfit) >= 0 ? 'stat-card--ok' : 'stat-card--bad'">
                <div class="stat-card__icon"><el-icon><Coin /></el-icon></div>
                <div class="stat-card__body">
                  <span class="stat-card__label">净利润</span>
                  <span class="stat-card__value">{{ income?.netProfit ?? '0.00' }}</span>
                </div>
              </div>
            </el-col>
          </el-row>

          <div v-if="income" class="report-title">
            <span class="report-title__main">利润表</span>
            <span class="report-title__sub">{{ income.period }}　单位：元</span>
          </div>
          <el-empty v-else description="暂无报表数据" />

          <div v-if="income" class="report-table-wrap report-table-wrap--wide">
            <table class="report-table">
              <thead>
                <tr>
                  <th class="col-item">项目</th>
                  <th class="col-num">本期金额</th>
                  <th class="col-num">本年累计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in income.items" :key="item.name">
                  <td>
                    <span class="account-code">{{ item.code || '' }}</span>{{ item.name }}
                  </td>
                  <td class="num">{{ item.currentAmount }}</td>
                  <td class="num">{{ item.ytdAmount }}</td>
                </tr>
                <tr class="row-total">
                  <td>三、营业利润</td>
                  <td class="num">{{ income.operatingProfit }}</td>
                  <td class="num">{{ income.ytdOperatingProfit }}</td>
                </tr>
                <tr class="row-total">
                  <td>四、利润总额</td>
                  <td class="num">{{ income.totalProfit }}</td>
                  <td class="num">{{ income.ytdTotalProfit }}</td>
                </tr>
                <tr class="row-grand">
                  <td>五、净利润</td>
                  <td class="num">{{ income.netProfit }}</td>
                  <td class="num">{{ income.ytdNetProfit }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck, CircleClose, Coin, Money, Search, TrendCharts, Wallet } from '@element-plus/icons-vue'
import { reportApi } from '@/api/finance'

const route = useRoute()
const loading = ref(false)
const activeTab = ref((route.query.tab as string) || 'balance-sheet')
const period = ref(new Date().toISOString().slice(0, 7))
const sheet = ref<any>(null)
const income = ref<any>(null)

const assetSections = computed(() => (sheet.value?.sections || []).slice(0, 2))
const liabEquitySections = computed(() => (sheet.value?.sections || []).slice(2))
const liabilityEquityTotal = computed(() =>
  (Number(sheet.value?.liabilityTotal || 0) + Number(sheet.value?.equityTotal || 0)).toFixed(2)
)

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'balance-sheet') {
      sheet.value = await reportApi.balanceSheet(period.value)
    } else {
      income.value = await reportApi.incomeStatement(period.value)
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.report-card {
  border-radius: 8px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit-tip {
  font-size: 12px;
  color: #909399;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fff;
  transition: box-shadow 0.2s, transform 0.2s;
}

.stat-card:hover {
  box-shadow: 0 6px 16px rgba(31, 45, 61, 0.08);
  transform: translateY(-2px);
}

.stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  font-size: 22px;
}

.stat-card--asset .stat-card__icon {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.stat-card--liability .stat-card__icon {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
}

.stat-card--ok .stat-card__icon {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.12);
}

.stat-card--bad .stat-card__icon {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.12);
}

.stat-card__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-card__label {
  font-size: 12px;
  color: #909399;
}

.stat-card__value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.stat-card__value--text {
  font-size: 16px;
}

.stat-card--ok .stat-card__value--text {
  color: #67c23a;
}

.stat-card--bad .stat-card__value--text {
  color: #f56c6c;
}

.report-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 16px;
}

.report-title__main {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 1px;
}

.report-title__sub {
  font-size: 12px;
  color: #909399;
}

.report-table-wrap {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
}

.report-table th {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
}

.report-table td {
  padding: 9px 12px;
  font-size: 13px;
  color: #303133;
  border-bottom: 1px solid #f2f4f7;
}

.report-table tbody tr:last-child td {
  border-bottom: none;
}

.report-table tbody tr:hover td {
  background: #f8fafc;
}

.col-item {
  min-width: 160px;
}

.col-num {
  width: 130px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.report-table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  width: 130px;
}

.account-code {
  display: inline-block;
  margin-right: 6px;
  color: #909399;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  font-size: 12px;
}

.cell-group {
  font-weight: 600;
  color: #1f2d3d;
  background: #f0f4f9;
}

.row-group td {
  background: #f0f4f9;
}

.row-total td {
  font-weight: 600;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}

.row-grand td {
  font-weight: 700;
  color: #303133;
  background: #ecf5ff;
  border-top: 1px solid #a2c8f5;
}

.grand-total {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin: 4px 0 16px;
  font-weight: 700;
  color: #1f2d3d;
}

.grand-total__num {
  color: #409eff;
  font-variant-numeric: tabular-nums;
}

.balance-alert {
  margin-top: 4px;
}
</style>








