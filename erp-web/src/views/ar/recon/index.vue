<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">往来对账</div>
        <div class="page-header__desc">应收台账与总账应收科目余额核对，定位未达账项</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar-actions">
        <el-select v-model="customerId" filterable placeholder="选择客户" style="width: 240px" @change="loadData">
          <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="loadData">对账</el-button>
      </div>

      <el-alert
        v-if="summary"
        :type="summary.difference === 0 ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        <template v-if="summary.difference === 0">
          应收台账与总账余额一致，差异为 0.00
        </template>
        <template v-else>
          存在差异 {{ formatAmount(summary.difference) }}，请检查未达账项或辅助核算设置
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column label="应收台账余额" width="140">
          <template #default="{ row }">{{ formatAmount(row.receivableBalance) }}</template>
        </el-table-column>
        <el-table-column label="总账余额" width="140">
          <template #default="{ row }">{{ formatAmount(row.ledgerBalance) }}</template>
        </el-table-column>
        <el-table-column label="差异" width="140">
          <template #default="{ row }">
            <span :class="Number(row.difference) === 0 ? 'ok' : 'danger'">
              {{ formatAmount(row.difference) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="对账结果" width="120">
          <template #default="{ row }">
            <el-tag :type="Number(row.difference) === 0 ? 'success' : 'warning'">
              {{ Number(row.difference) === 0 ? '已对平' : '有差异' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="tip">
        总账余额取自已记账凭证中「1122 应收账款」科目的辅助核算项（按客户）汇总。
        差异通常来自未生成凭证的单据或辅助核算未设置客户。
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { reconApi } from '@/api/ar-ap'
import { customerApi } from '@/api/master'

const loading = ref(false)
const list = ref<any[]>([])
const customers = ref<any[]>([])
const customerId = ref<number | undefined>(undefined)

const summary = computed(() => {
  if (!list.value.length) {
    return null
  }
  return list.value.reduce(
    (acc, row) => ({
      receivableBalance: acc.receivableBalance + (Number(row.receivableBalance) || 0),
      ledgerBalance: acc.ledgerBalance + (Number(row.ledgerBalance) || 0),
      difference: acc.difference + (Number(row.difference) || 0)
    }),
    { receivableBalance: 0, ledgerBalance: 0, difference: 0 }
  )
})

const formatAmount = (value: any) => (Number(value) || 0).toFixed(2)

const loadData = async () => {
  if (!customerId.value) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await reconApi.recon(customerId.value)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await customerApi.list()
  customers.value = res || []
  if (customers.value.length) {
    customerId.value = customers.value[0].id
    loadData()
  }
})
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ok {
  color: var(--el-color-success);
}

.danger {
  color: var(--el-color-danger);
}
</style>
