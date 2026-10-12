<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">科目余额表</div>
        <div class="page-header__desc">查询科目发生额与期末余额</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="会计期间">
          <el-input v-model="query.period" placeholder="如 2026-10" style="width: 140px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border show-summary
        :summary-method="getSummaries">
        <el-table-column prop="subjectCode" label="科目编码" min-width="130" />
        <el-table-column prop="subjectName" label="科目名称" min-width="150" />
        <el-table-column prop="openingDebit" label="期初借方" width="120" />
        <el-table-column prop="openingCredit" label="期初贷方" width="120" />
        <el-table-column prop="periodDebit" label="本期借方" width="120" />
        <el-table-column prop="periodCredit" label="本期贷方" width="120" />
        <el-table-column prop="closingDebit" label="期末借方" width="120" />
        <el-table-column prop="closingCredit" label="期末贷方" width="120" />
      </el-table>
      <div class="tip">仅统计已记账凭证；期初 = 历史期间累计，期末 = 期初 + 本期发生。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { balanceApi } from '@/api/finance'

const loading = ref(false)
const list = ref<any[]>([])
const query = reactive({ period: new Date().toISOString().slice(0, 7) })

const num = (value: any) => Number(value) || 0

const getSummaries = ({ columns, data }: any) => {
  const sums: any[] = []
  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = column.property
    if (prop && ['openingDebit', 'openingCredit', 'periodDebit', 'periodCredit', 'closingDebit', 'closingCredit'].includes(prop)) {
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
    list.value = await balanceApi.account(query.period)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>








