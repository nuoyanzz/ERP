<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">坏账计提</div>
        <div class="page-header__desc">按期计提应收坏账，审核后生成减值准备凭证</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="记录编号">
          <el-input v-model="query.recordNo" placeholder="记录编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="期间">
          <el-input v-model="query.period" placeholder="如 2026-10" clearable style="width: 130px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'ar:baddebt:add'" type="primary" :icon="Plus" @click="handleCreate">新增计提</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="recordNo" label="记录编号" min-width="150" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="amount" label="计提金额" width="130" />
        <el-table-column prop="reason" label="计提原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditTime" label="审核时间" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'ar:baddebt:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'ar:baddebt:audit'" link type="success"
              @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 0" v-perms="'ar:baddebt:remove'" link type="danger"
              @click="handleRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-page="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑坏账计提' : '新增坏账计提'" width="640px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="90px">
        <el-form-item label="记录编号" prop="recordNo">
          <el-input v-model="editing.recordNo" placeholder="如 BDT-202610-001" style="width: 200px" />
        </el-form-item>
        <el-form-item label="期间" prop="period">
          <el-input v-model="editing.period" placeholder="如 2026-10" style="width: 140px" />
        </el-form-item>
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="editing.customerId" filterable placeholder="请选择客户" style="width: 220px">
            <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计提金额" prop="amount">
          <el-input-number v-model="editing.amount" :min="0" :precision="2" controls-position="right"
            style="width: 160px" />
        </el-form-item>
        <el-form-item label="计提原因">
          <el-input v-model="editing.reason" placeholder="如 客户经营困难" style="width: 260px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editing.remark" placeholder="备注" style="width: 260px" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { badDebtApi } from '@/api/ar-ap'
import { customerApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const customers = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  recordNo: '',
  period: ''
})

const rules = {
  recordNo: [{ required: true, message: '请填写记录编号', trigger: 'blur' }],
  period: [{ required: true, message: '请填写期间', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  amount: [{ required: true, message: '请填写计提金额', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await badDebtApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, recordNo: '', period: '' })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, {
    id: undefined,
    recordNo: '',
    period: new Date().toISOString().slice(0, 7),
    customerId: undefined,
    amount: 0,
    reason: '',
    remark: '',
    status: 0
  })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await badDebtApi.detail(row.id)
  Object.assign(editing, JSON.parse(JSON.stringify(data)))
  dialogVisible.value = true
}

const handleRemove = (row: any) => {
  ElMessageBox.confirm(`确认删除坏账计提「${row.recordNo}」？`, '删除计提', { type: 'warning' }).then(async () => {
    await badDebtApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(
    `确认审核坏账计提「${row.recordNo}」？审核后将生成坏账准备凭证并计入应收单。`,
    '审核计提',
    { type: 'warning' }
  ).then(async () => {
    await badDebtApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.id) {
      await badDebtApi.update(editing)
    } else {
      await badDebtApi.create(editing)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loadData()
  const res = await customerApi.list()
  customers.value = res || []
})
</script>
