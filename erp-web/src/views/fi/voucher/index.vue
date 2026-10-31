<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">记账凭证</div>
        <div class="page-header__desc">手工录入与审核记账凭证，凭证状态机受控流转</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="凭证号">
          <el-input v-model="query.voucherNo" placeholder="凭证号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="期间">
          <el-input v-model="query.period" placeholder="如 2026-10" clearable style="width: 120px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="query.status" dict-type="fi_voucher_status" placeholder="全部" clearable
            style="width: 120px" />
        </el-form-item>
        <el-form-item label="凭证日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'fi:voucher:add'" type="primary" :icon="Plus" @click="handleCreate">新增凭证</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="voucherNo" label="凭证号" min-width="140" />
        <el-table-column prop="voucherDate" label="凭证日期" width="120" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="totalDebit" label="借方合计" width="120" />
        <el-table-column prop="totalCredit" label="贷方合计" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <DictTag dict-type="fi_voucher_status" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'fi:voucher:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'fi:voucher:submit'" link type="success"
              @click="handleSubmit(row)">提交</el-button>
            <el-button v-if="row.status === 1" v-perms="'fi:voucher:audit'" link type="warning"
              @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 1" v-perms="'fi:voucher:unaudit'" link type="warning"
              @click="handleUnaudit(row)">反审核</el-button>
            <el-button v-if="row.status === 2" v-perms="'fi:voucher:post'" link type="success"
              @click="handlePost(row)">记账</el-button>
            <el-button v-if="row.status === 3" v-perms="'fi:voucher:unpost'" link type="warning"
              @click="handleUnpost(row)">反记账</el-button>
            <el-button v-if="row.status === 3" v-perms="'fi:voucher:redFlush'" link type="danger"
              @click="handleRedFlush(row)">红冲</el-button>
            <el-button v-if="row.status === 0 || row.status === 1 || row.status === 2" v-perms="'fi:voucher:void'"
              link type="danger" @click="handleVoid(row)">作废</el-button>
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-page="loadData" />
      </div>
    </el-card>

    <!-- 凭证录入 -->
    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑记账凭证' : '新增记账凭证'" width="960px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="凭证字" prop="word">
          <el-input v-model="editing.word" placeholder="记" style="width: 90px" />
        </el-form-item>
        <el-form-item label="凭证日期" prop="voucherDate">
          <el-date-picker v-model="editing.voucherDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="附件数">
          <el-input-number v-model="editing.attachCount" :min="0" :precision="0" controls-position="right"
            style="width: 120px" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>会计分录</span>
        <el-button size="small" :icon="Plus" @click="handleAddEntry">添加分录</el-button>
      </div>
      <el-table :data="editing.entries" border size="small">
        <el-table-column label="摘要" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.summary" size="small" placeholder="摘要" />
          </template>
        </el-table-column>
        <el-table-column label="会计科目" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.accountId" filterable placeholder="请选择科目" size="small" style="width: 100%"
              @change="handleAccountChange(row)">
              <el-option v-for="a in flatAccounts" :key="a.id" :label="`${a.subjectCode} ${a.subjectName}`"
                :value="a.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="借方" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.debitAmount" :min="0" :precision="4" size="small" controls-position="right"
              style="width: 110px" :disabled="row.direction === 2" @change="syncAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="贷方" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.creditAmount" :min="0" :precision="4" size="small" controls-position="right"
              style="width: 110px" :disabled="row.direction === 1" @change="syncAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="辅助核算" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.assist" size="small" placeholder="客户/供应商/部门" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.entries.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="balance-line" :class="{ 'balance-line--ok': balanced, 'balance-line--bad': !balanced }">
        借方合计：{{ debitTotal }} &nbsp;|&nbsp; 贷方合计：{{ creditTotal }}
        <span v-if="!balanced" class="balance-line__tip">借贷不平，无法保存</span>
        <span v-else class="balance-line__tip">借贷平衡</span>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!balanced" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 凭证详情 -->
    <el-dialog v-model="detailVisible" title="记账凭证详情" width="860px">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="凭证号">{{ detail.voucherNo }}</el-descriptions-item>
        <el-descriptions-item label="凭证字">{{ detail.word }}</el-descriptions-item>
        <el-descriptions-item label="凭证日期">{{ detail.voucherDate }}</el-descriptions-item>
        <el-descriptions-item label="期间">{{ detail.period }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag dict-type="fi_voucher_status" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="借方合计">{{ detail.totalDebit }}</el-descriptions-item>
        <el-descriptions-item label="贷方合计">{{ detail.totalCredit }}</el-descriptions-item>
        <el-descriptions-item label="附件数">{{ detail.attachCount }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="accountCode" label="科目编码" width="110" />
        <el-table-column prop="accountName" label="科目名称" min-width="140" />
        <el-table-column prop="summary" label="摘要" min-width="140" show-overflow-tooltip />
        <el-table-column label="借方" width="110">
          <template #default="{ row }">{{ row.direction === 1 ? row.amount : '' }}</template>
        </el-table-column>
        <el-table-column label="贷方" width="110">
          <template #default="{ row }">{{ row.direction === 2 ? row.amount : '' }}</template>
        </el-table-column>
        <el-table-column prop="assist" label="辅助核算" min-width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { accountApi, voucherApi, type AccountTreeVO } from '@/api/finance'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  voucherNo: '',
  period: '',
  status: undefined as number | undefined,
  startDate: '',
  endDate: ''
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const accountTree = ref<AccountTreeVO[]>([])

/**
 * 扁平化科目树，供下拉选择使用。
 */
const flatAccounts = computed(() => {
  const result: AccountTreeVO[] = []
  const walk = (nodes: AccountTreeVO[]) => {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        walk(node.children)
      } else {
        result.push(node)
      }
    }
  }
  walk(accountTree.value)
  return result
})

const debitTotal = computed(() =>
  editing.entries
    .reduce((sum: number, row: any) => sum + (Number(row.debitAmount) || 0), 0)
    .toFixed(2)
)

const creditTotal = computed(() =>
  editing.entries
    .reduce((sum: number, row: any) => sum + (Number(row.creditAmount) || 0), 0)
    .toFixed(2)
)

const balanced = computed(() => debitTotal.value === creditTotal.value)

const rules = {
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }],
  word: [{ required: true, message: '请填写凭证字', trigger: 'blur' }]
}

const emptyEditing = () => ({
  id: undefined,
  word: '记',
  voucherDate: new Date().toISOString().slice(0, 10),
  attachCount: 0,
  summary: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await voucherApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  if (dateRange.value) {
    query.startDate = dateRange.value[0]
    query.endDate = dateRange.value[1]
  } else {
    query.startDate = ''
    query.endDate = ''
  }
  loadData()
}

const handleReset = () => {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    voucherNo: '',
    period: '',
    status: undefined,
    startDate: '',
    endDate: ''
  })
  dateRange.value = null
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  handleAddEntry()
  handleAddEntry()
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await voucherApi.detail(row.id)
  Object.assign(editing, {
    id: data.id,
    word: data.word,
    voucherDate: data.voucherDate,
    attachCount: data.attachCount,
    summary: data.summary,
    entries: (data.entries || []).map((e: any) => ({
      accountId: e.accountId,
      direction: e.direction,
      amount: e.amount,
      summary: e.summary,
      assist: e.assist,
      auxType: e.auxType,
      auxId: e.auxId,
      debitAmount: e.direction === 1 ? e.amount : 0,
      creditAmount: e.direction === 2 ? e.amount : 0
    }))
  })
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await voucherApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddEntry = () => {
  editing.entries.push({
    accountId: undefined,
    direction: 1,
    amount: 0,
    summary: '',
    assist: '',
    debitAmount: 0,
    creditAmount: 0
  })
}

/**
 * 选中科目后自动判断借贷方向：资产类与费用类记借方，其余记贷方。
 */
const handleAccountChange = (row: any) => {
  const account = flatAccounts.value.find((a) => a.id === row.accountId)
  if (!account) return
  // 1 资产 2 负债 3 权益 4 成本 5 损益
  const debitSide = account.category === 1 || account.category === 4 || account.category === 5
    ? (account.balanceDir === 1)
    : false
  row.direction = debitSide ? 1 : 2
  syncAmount(row)
}

/**
 * 借方与贷方输入同步到统一的 amount 字段。
 */
const syncAmount = (row: any) => {
  if (row.direction === 1) {
    row.amount = Number(row.debitAmount || 0)
  } else {
    row.amount = Number(row.creditAmount || 0)
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!editing.entries.length) {
    ElMessage.warning('请至少添加一行分录')
    return
  }
  if (!balanced.value) {
    ElMessage.warning('借贷不平，无法保存')
    return
  }
  editing.entries.forEach(syncAmount)
  const payload = {
    id: editing.id,
    word: editing.word,
    voucherDate: editing.voucherDate,
    attachCount: editing.attachCount,
    summary: editing.summary,
    entries: editing.entries.map((e: any) => ({
      accountId: e.accountId,
      direction: e.direction,
      amount: e.amount,
      summary: e.summary,
      assist: e.assist,
      auxType: e.auxType,
      auxId: e.auxId
    }))
  }
  saving.value = true
  try {
    if (editing.id) {
      await voucherApi.update(payload)
    } else {
      await voucherApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleSubmit = (row: any) => {
  ElMessageBox.confirm(`确认提交凭证「${row.voucherNo}」？提交后将进入审核流程。`, '提交凭证', {
    type: 'warning'
  }).then(async () => {
    await voucherApi.submit(row.id)
    ElMessage.success('提交成功')
    loadData()
  })
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(`确认审核凭证「${row.voucherNo}」？`, '审核凭证', { type: 'warning' }).then(async () => {
    await voucherApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

const handleUnaudit = (row: any) => {
  ElMessageBox.confirm(
    `确认反审核凭证「${row.voucherNo}」？反审核后凭证退回已提交状态，可重新编辑。`,
    '反审核凭证',
    { type: 'warning' }
  ).then(async () => {
    await voucherApi.unaudit(row.id)
    ElMessage.success('反审核成功')
    loadData()
  })
}

const handlePost = (row: any) => {
  ElMessageBox.confirm(
    `确认记账凭证「${row.voucherNo}」？记账后凭证生效并参与余额表与报表统计。`,
    '凭证记账',
    { type: 'warning' }
  ).then(async () => {
    await voucherApi.post(row.id)
    ElMessage.success('记账成功')
    loadData()
  })
}

const handleUnpost = (row: any) => {
  ElMessageBox.confirm(
    `确认反记账凭证「${row.voucherNo}」？反记账后该凭证将退出账簿，不再参与余额与报表统计。`,
    '反记账',
    { type: 'warning' }
  ).then(async () => {
    await voucherApi.unpost(row.id)
    ElMessage.success('反记账成功')
    loadData()
  })
}

const handleRedFlush = (row: any) => {
  ElMessageBox.confirm(
    `确认对凭证「${row.voucherNo}」执行红字冲销？系统将自动生成一张负数红字凭证。`,
    '红字冲销',
    { type: 'warning' }
  ).then(async () => {
    await voucherApi.redFlush(row.id)
    ElMessage.success('红字凭证已生成')
    loadData()
  })
}

const handleVoid = (row: any) => {
  ElMessageBox.prompt(
    `作废凭证「${row.voucherNo}」需填写作废原因，作废后凭证不再参与账簿计算。`,
    '作废凭证',
    {
      inputPlaceholder: '请输入作废原因',
      inputValidator: (v: string) => (v && v.trim() ? true : '作废原因不能为空')
    }
  ).then(async ({ value }: any) => {
    await voucherApi.voidVoucher(row.id, value)
    ElMessage.success('作废成功')
    loadData()
  })
}

onMounted(async () => {
  loadData()
  accountTree.value = (await accountApi.tree()) || []
})
</script>

<style scoped lang="scss">
.balance-line {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  font-size: 13px;

  &--ok {
    background: rgb(5 150 105 / 10%);
    color: #059669;
  }

  &--bad {
    background: rgb(220 38 38 / 10%);
    color: #dc2626;
  }

  &__tip {
    margin-left: 12px;
    font-size: 12px;
  }
}
</style>








