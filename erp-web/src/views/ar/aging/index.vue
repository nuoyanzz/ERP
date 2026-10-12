<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">账龄分析</div>
        <div class="page-header__desc">按账龄区间统计客户未收金额，识别逾期风险</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-date-picker v-model="asOfDate" type="date" value-format="YYYY-MM-DD" placeholder="截止日期（默认今天）"
        @change="loadData" />
      <el-button type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column prop="total" label="未收总额" width="130" />
        <el-table-column prop="notDue" label="未到期" width="120" />
        <el-table-column prop="within30" label="逾期1-30天" width="120">
          <template #default="{ row }">
            <span :class="{ warning: Number(row.within30) > 0 }">{{ row.within30 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="within60" label="逾期31-60天" width="130">
          <template #default="{ row }">
            <span :class="{ danger: Number(row.within60) > 0 }">{{ row.within60 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="within90" label="逾期61-90天" width="130">
          <template #default="{ row }">
            <span :class="{ danger: Number(row.within90) > 0 }">{{ row.within90 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="over90" label="逾期90天以上" width="140">
          <template #default="{ row }">
            <span :class="{ danger: Number(row.over90) > 0 }">{{ row.over90 }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="tip">账龄按截止日期与应收单到期日之差计算，仅统计未结清应收单。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { arReceivableApi } from '@/api/ar-ap'

const loading = ref(false)
const list = ref<any[]>([])
const asOfDate = ref('')

const loadData = async () => {
  loading.value = true
  try {
    list.value = await arReceivableApi.aging(asOfDate.value || undefined)
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

.warning {
  color: #e6a23c;
  font-weight: 600;
}

.danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>








