<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">薪资核算</div>
        <div class="page-header__desc">按月核算员工薪资并生成记录</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="薪资期间">
          <el-input v-model="query.period" placeholder="如 2026-10" style="width: 130px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="员工">
          <el-input v-model="query.employeeId" placeholder="员工ID" clearable style="width: 100px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="暂存" :value="0" />
            <el-option label="已审核" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'hr:salary:add'" type="primary" :icon="Plus" @click="handleCreate">新增薪资单</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="salaryNo" label="薪资单号" min-width="150" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="employeeNo" label="工号" min-width="120" />
        <el-table-column prop="employeeName" label="员工" min-width="100" />
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column prop="baseAmount" label="基本工资" width="100" />
        <el-table-column prop="meritAmount" label="绩效" width="90" />
        <el-table-column prop="allowanceAmount" label="津贴" width="90" />
        <el-table-column prop="deductAmount" label="扣款" width="90" />
        <el-table-column prop="socialAmount" label="社保" width="90" />
        <el-table-column prop="netAmount" label="实发" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'hr:salary:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'hr:salary:audit'" link type="success"
              @click="handleAudit(row)">审核</el-button>
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑薪资单' : '新增薪资单'" width="760px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="薪资期间" prop="period">
          <el-input v-model="editing.period" placeholder="如 2026-10" style="width: 140px" />
        </el-form-item>
        <el-form-item label="员工" prop="employeeId">
          <el-select v-model="editing.employeeId" filterable placeholder="请选择员工" style="width: 220px">
            <el-option v-for="e in employees" :key="e.id" :label="`${e.employeeNo} ${e.employeeName}`" :value="e.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>薪资项目（实发 = 基本 + 绩效 + 津贴 − 扣款 − 社保）</span>
        <el-button size="small" :icon="Plus" @click="handleAddItem">添加项目</el-button>
      </div>
      <el-table :data="editing.entries" border size="small">
        <el-table-column label="项目类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.itemType" size="small" style="width: 100%" @change="handleItemTypeChange(row)">
              <el-option label="基本工资" :value="1" />
              <el-option label="绩效" :value="2" />
              <el-option label="津贴" :value="3" />
              <el-option label="扣款" :value="4" />
              <el-option label="社保" :value="5" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.itemName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.amount" :min="0" :precision="2" size="small" controls-position="right"
              style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.entries.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="total-line">预计实发：{{ netPreview }}</div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="薪资单详情" width="680px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="薪资单号">{{ detail.salaryNo }}</el-descriptions-item>
        <el-descriptions-item label="期间">{{ detail.period }}</el-descriptions-item>
        <el-descriptions-item label="员工">{{ detail.employeeName }}</el-descriptions-item>
        <el-descriptions-item label="实发金额">{{ detail.netAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '已审核' : '暂存' }}</el-descriptions-item>
        <el-descriptions-item label="凭证">{{ detail.voucherId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column label="项目类型" width="120">
          <template #default="{ row }">{{ itemTypeText(row.itemType) }}</template>
        </el-table-column>
        <el-table-column prop="itemName" label="项目名称" min-width="140" />
        <el-table-column prop="amount" label="金额" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { employeeApi, salaryApi } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  period: '',
  employeeId: '',
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const employees = ref<any[]>([])

const itemTypeText = (type: number) =>
  ({ 1: '基本工资', 2: '绩效', 3: '津贴', 4: '扣款', 5: '社保' } as Record<number, string>)[type] || '-'

const netPreview = computed(() => {
  let net = 0
  for (const row of editing.entries) {
    const amount = Number(row.amount) || 0
    if (row.itemType === 4 || row.itemType === 5) net -= amount
    else net += amount
  }
  return net.toFixed(2)
})

const rules = {
  period: [{ required: true, message: '请输入薪资期间', trigger: 'blur' }],
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  period: new Date().toISOString().slice(0, 7),
  employeeId: undefined as number | undefined,
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await salaryApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, period: '', employeeId: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  editing.entries.push({ itemType: 1, itemName: '基本工资', amount: 0 })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await salaryApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await salaryApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddItem = () => {
  editing.entries.push({ itemType: 1, itemName: itemTypeText(1), amount: 0 })
}

// 选择项目类型时自动带出类型名称（已手动填写过名称则保留用户输入）
const handleItemTypeChange = (row: any) => {
  if (!row.itemName) row.itemName = itemTypeText(row.itemType)
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('请添加薪资项目')
      return
    }
    const types = editing.entries.map((row: any) => row.itemType)
    if (new Set(types).size !== types.length) {
      ElMessage.warning('同一项目类型只能添加一行')
      return
    }
    saving.value = true
    try {
      const payload = {
        id: editing.id,
        period: editing.period,
        employeeId: editing.employeeId,
        remark: editing.remark,
        entries: editing.entries
      }
      if (editing.id) {
        await salaryApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await salaryApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(
    `确认审核薪资单「${row.salaryNo}」？审核后将自动生成凭证：借：管理费用-工资 / 贷：应付职工薪酬。`,
    '审核薪资单',
    { type: 'warning' }
  ).then(async () => {
    await salaryApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

onMounted(async () => {
  employees.value = (await employeeApi.page({ pageNum: 1, pageSize: 300, status: 0 })).records
  loadData()
})
</script>

<style scoped>
.entry-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
}

.total-line {
  text-align: right;
  margin-top: 8px;
  font-weight: 600;
  color: #f56c6c;
}
</style>








