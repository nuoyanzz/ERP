<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">期末处理</div>
        <div class="page-header__desc">执行损益结转，将损益类科目余额转入本年利润</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-alert type="info" :closable="false" show-icon
        title="损益结转：将本期损益类科目余额全部结转至「本年利润（4103）」，结转后生成一张待审核凭证。每个期间只能执行一次。" />
      <div class="toolbar-actions">
      <el-date-picker v-model="period" type="month" value-format="YYYY-MM" placeholder="如 2026-10"
        style="width: 160px" />
      <el-button type="primary" :loading="running" @click="handleCarry">执行损益结转</el-button>
      <el-button v-if="resultVoucherId" @click="goVoucher">查看生成的凭证</el-button>
    </div>
      <el-alert v-if="resultVoucherId" type="success" :closable="false" show-icon
        :title="`结转完成，已生成凭证 ID：${resultVoucherId}（状态：已提交，待财务审核记账）`" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { carryApi } from '@/api/finance'

const router = useRouter()
const period = ref(new Date().toISOString().slice(0, 7))
const running = ref(false)
const resultVoucherId = ref<number | null>(null)

const handleCarry = async () => {
  await ElMessageBox.confirm(
    `确认对 ${period.value} 期间执行损益结转？执行后将生成本期损益结转凭证（每个期间仅可执行一次）。`,
    '损益结转',
    { type: 'warning' }
  )
  running.value = true
  try {
    const voucherId = await carryApi.profitAndLoss(period.value)
    resultVoucherId.value = voucherId
    ElMessage.success('结转成功')
  } finally {
    running.value = false
  }
}

const goVoucher = () => {
  router.push('/fi/voucher')
}
</script>

<style scoped>
.action-bar {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.result {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9eb;
  border: 1px solid #67c23a;
  border-radius: 4px;
  color: #67c23a;
}
</style>








