<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">试算平衡</div>
        <div class="page-header__desc">按会计期间生成试算平衡表，校验期初、本期发生额与期末余额是否借贷平衡</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="会计期间">
          <el-date-picker v-model="query.period" type="month" value-format="YYYY-MM" placeholder="选择期间"
            style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button v-perms="'fi:trial:generate'" type="primary" :icon="Search" @click="loadData">生成试算平衡表</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert v-if="result" :type="result.balanced ? 'success' : 'error'" :closable="false" show-icon
      class="result-alert" :title="result.balanced ? '借贷平衡' : '借贷不平衡'" :description="alertDescription" />

    <el-card shadow="never">
      <el-table v-loading="loading" :data="result?.lines || []" border show-summary :summary-method="getSummaries">
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="accountCode" label="科目编码" min-width="130" />
        <el-table-column prop="accountName" label="科目名称" min-width="160" />
        <el-table-column label="期初借方" width="120" align="right">
          <template #default="{ row }">{{ Number(row.openingDebit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="期初贷方" width="120" align="right">
          <template #default="{ row }">{{ Number(row.openingCredit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="本期借方" width="120" align="right">
          <template #default="{ row }">{{ Number(row.debitAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="本期贷方" width="120" align="right">
          <template #default="{ row }">{{ Number(row.creditAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="期末借方" width="120" align="right">
          <template #default="{ row }">{{ Number(row.closingDebit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="期末贷方" width="120" align="right">
          <template #default="{ row }">{{ Number(row.closingCredit || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>

      <div class="tip">期初 = 历史期间累计，本期 = 已记账凭证发生额，期末 = 期初 + 本期。生成试算平衡表会保存该期间的记账基线快照。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { trialBalanceApi } from '@/api/finance'

const loading = ref(false)
const result = ref<any>(null)
const query = reactive({ period: new Date().toISOString().slice(0, 7) })

const num = (value: any) => Number(value) || 0

const alertDescription = computed(() => {
  if (!result.value) {
    return ''
  }
  const r = result.value
  return [
    `期初借方 ${Number(r.openingDebitTotal || 0).toFixed(2)} / 贷方 ${Number(r.openingCreditTotal || 0).toFixed(2)}`,
    `本期借方 ${Number(r.debitTotal || 0).toFixed(2)} / 贷方 ${Number(r.creditTotal || 0).toFixed(2)}`,
    `期末借方 ${Number(r.closingDebitTotal || 0).toFixed(2)} / 贷方 ${Number(r.closingCreditTotal || 0).toFixed(2)}`,
    `差额 ${Number(r.difference || 0).toFixed(2)}`
  ].join('；')
})

const getSummaries = ({ columns, data }: any) => {
  const sums: any[] = []
  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = column.property
    if (
      prop &&
      ['openingDebit', 'openingCredit', 'debitAmount', 'creditAmount', 'closingDebit', 'closingCredit'].includes(prop)
    ) {
      sums[index] = data.reduce((sum: number, row: any) => sum + num(row[prop]), 0).toFixed(2)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

const loadData = async () => {
  loading.value = true
  try {
    result.value = await trialBalanceApi.generate(query.period)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  query.period = new Date().toISOString().slice(0, 7)
  result.value = null
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.result-alert {
  margin-bottom: 12px;
}

.tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
