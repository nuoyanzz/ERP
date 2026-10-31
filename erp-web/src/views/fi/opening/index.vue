<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">期初余额</div>
        <div class="page-header__desc">按会计期间录入期初余额，并可通过试算平衡校验账套是否平衡</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="会计期间">
          <el-input v-model="query.period" placeholder="如 2026-01" style="width: 140px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button v-perms="'fi:trial:generate'" :icon="ScaleToOriginal" @click="handleTrialBalance">试算平衡</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'fi:opening:add'" type="primary" :icon="Plus" @click="handleCreate">录入期初余额</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border show-summary :summary-method="getSummaries">
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="accountCode" label="科目编码" min-width="130" />
        <el-table-column prop="accountName" label="科目名称" min-width="160" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="currency" label="币别" width="80" />
        <el-table-column label="借方余额" width="130" align="right">
          <template #default="{ row }">{{ Number(row.debitAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="贷方余额" width="130" align="right">
          <template #default="{ row }">{{ Number(row.creditAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'fi:opening:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 录入 / 编辑期初余额 -->
    <el-dialog v-model="dialogVisible" title="录入期初余额" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="会计期间" prop="period">
          <el-input v-model="editing.period" placeholder="如 2026-01" />
        </el-form-item>
        <el-form-item label="会计科目" prop="accountId">
          <el-tree-select v-model="editing.accountId" :data="accountTree"
            :props="{ label: 'subjectName', children: 'children', value: 'id' }" check-strictly clearable
            placeholder="请选择会计科目" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币别">
          <el-input v-model="editing.currency" placeholder="如 CNY" style="width: 140px" />
        </el-form-item>
        <el-form-item label="借方余额" prop="debitAmount">
          <el-input-number v-model="editing.debitAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="贷方余额" prop="creditAmount">
          <el-input-number v-model="editing.creditAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editing.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 试算平衡结果 -->
    <el-dialog v-model="trialVisible" title="试算平衡" width="720px">
      <el-alert
        :type="trialResult?.balanced ? 'success' : 'error'"
        :closable="false"
        show-icon
        :title="trialResult?.balanced ? '借贷平衡' : '借贷不平衡'"
        :description="trialDescription"
      />

      <el-table v-loading="trialLoading" :data="trialResult?.lines || []" border style="margin-top: 12px"
        show-summary :summary-method="getTrialSummaries">
        <el-table-column prop="accountCode" label="科目编码" min-width="120" />
        <el-table-column prop="accountName" label="科目名称" min-width="140" />
        <el-table-column label="期初借方" width="110" align="right">
          <template #default="{ row }">{{ Number(row.openingDebit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="期初贷方" width="110" align="right">
          <template #default="{ row }">{{ Number(row.openingCredit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="本期借方" width="110" align="right">
          <template #default="{ row }">{{ Number(row.debitAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="本期贷方" width="110" align="right">
          <template #default="{ row }">{{ Number(row.creditAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="期末借方" width="110" align="right">
          <template #default="{ row }">{{ Number(row.closingDebit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="期末贷方" width="110" align="right">
          <template #default="{ row }">{{ Number(row.closingCredit || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="trialVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Plus, ScaleToOriginal } from '@element-plus/icons-vue'
import { accountApi, openingApi, trialBalanceApi, type AccountTreeVO } from '@/api/finance'

const loading = ref(false)
const saving = ref(false)
const trialLoading = ref(false)
const list = ref<any[]>([])
const accountTree = ref<AccountTreeVO[]>([])
const dialogVisible = ref(false)
const trialVisible = ref(false)
const trialResult = ref<any>(null)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const query = reactive({ period: new Date().toISOString().slice(0, 7) })

const emptyEditing = () => ({
  id: undefined,
  period: query.period,
  accountId: undefined as number | undefined,
  currency: 'CNY',
  debitAmount: 0,
  creditAmount: 0,
  auxType: '',
  auxId: undefined as number | undefined,
  remark: ''
})

const rules = {
  period: [{ required: true, message: '请输入会计期间', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择会计科目', trigger: 'change' }],
  creditAmount: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        const debit = Number(editing.debitAmount) || 0
        if (Number(value) > 0 && debit > 0) {
          callback(new Error('借方余额与贷方余额不能同时大于 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const num = (value: any) => Number(value) || 0

const loadData = async () => {
  loading.value = true
  try {
    list.value = await openingApi.list(query.period)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        accountId: editing.accountId,
        period: editing.period,
        currency: editing.currency,
        debitAmount: editing.debitAmount,
        creditAmount: editing.creditAmount,
        auxType: editing.auxType,
        auxId: editing.auxId
      }
      await openingApi.save(payload)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确认删除科目「${row.accountCode} ${row.accountName}」在 ${row.period} 的期初余额？`,
    '提示',
    { type: 'warning' }
  ).then(async () => {
    await openingApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleTrialBalance = async () => {
  trialVisible.value = true
  trialLoading.value = true
  trialResult.value = null
  try {
    trialResult.value = await trialBalanceApi.generate(query.period)
  } finally {
    trialLoading.value = false
  }
}

const trialDescription = computed(() => {
  if (!trialResult.value) {
    return ''
  }
  const r = trialResult.value
  return `借方合计 ${Number(r.debitTotal || 0).toFixed(2)}，贷方合计 ${Number(r.creditTotal || 0).toFixed(2)}，差额 ${Number(r.difference || 0).toFixed(2)}`
})

const getSummaries = ({ columns, data }: any) => {
  const sums: any[] = []
  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = column.property
    if (prop === 'debitAmount' || prop === 'creditAmount') {
      sums[index] = data.reduce((sum: number, row: any) => sum + num(row[prop]), 0).toFixed(2)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

const getTrialSummaries = ({ columns, data }: any) => {
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

onMounted(async () => {
  accountTree.value = await accountApi.tree()
  loadData()
})
</script>
