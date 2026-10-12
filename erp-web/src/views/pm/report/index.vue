<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">生产报工</div>
        <div class="page-header__desc">记录工序产出与工时，归集人工成本到生产订单</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="报工单号">
          <el-input v-model="query.reportNo" placeholder="报工单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="报工日期">
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
      <el-button v-perms="'pm:report:add'" type="primary" :icon="Plus" @click="handleCreate">新增报工</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="reportNo" label="报工单号" min-width="150" />
        <el-table-column prop="moNo" label="生产订单" min-width="150" />
        <el-table-column prop="operationName" label="工序" min-width="120" />
        <el-table-column prop="employeeName" label="报工人" width="100" />
        <el-table-column prop="reportDate" label="报工日期" width="120" />
        <el-table-column prop="qty" label="产出数量" width="100" />
        <el-table-column prop="qualifiedQty" label="合格数" width="100" />
        <el-table-column prop="scrapQty" label="报废数" width="100" />
        <el-table-column prop="workHours" label="工时" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'pm:report:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'pm:report:audit'" link type="success"
              @click="handleAudit(row)">审核</el-button>
            <el-button v-perms="'pm:report:remove'" link type="danger" @click="handleRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-page="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑报工单' : '新增报工单'" width="700px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="90px">
        <el-form-item label="生产订单" prop="moId">
          <el-select v-model="editing.moId" filterable placeholder="请选择生产订单" style="width: 240px">
            <el-option v-for="m in moList" :key="m.id" :label="m.moNo" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报工人" prop="employeeId">
          <el-select v-model="editing.employeeId" filterable placeholder="请选择" style="width: 180px">
            <el-option v-for="e in employees" :key="e.id" :label="e.employeeName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报工日期" prop="reportDate">
          <el-date-picker v-model="editing.reportDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="产出数量" prop="qty">
          <el-input-number v-model="editing.qty" :min="0.0001" :precision="4" controls-position="right"
            style="width: 130px" />
        </el-form-item>
        <el-form-item label="合格数">
          <el-input-number v-model="editing.qualifiedQty" :min="0" :precision="4" controls-position="right"
            style="width: 130px" />
        </el-form-item>
        <el-form-item label="报废数">
          <el-input-number v-model="editing.scrapQty" :min="0" :precision="4" controls-position="right"
            style="width: 130px" />
        </el-form-item>
        <el-form-item label="工时">
          <el-input-number v-model="editing.workHours" :min="0" :precision="2" controls-position="right"
            style="width: 130px" />
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
import { moApi, reportWorkApi } from '@/api/production'
import { employeeApi } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  reportNo: '',
  startDate: '',
  endDate: ''
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})
const moList = ref<any[]>([])
const employees = ref<any[]>([])

const rules = {
  moId: [{ required: true, message: '请选择生产订单', trigger: 'change' }],
  reportDate: [{ required: true, message: '请选择报工日期', trigger: 'change' }],
  qty: [{ required: true, message: '请填写产出数量', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await reportWorkApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, reportNo: '', startDate: '', endDate: '' })
  dateRange.value = null
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, {
    id: undefined,
    moId: undefined,
    operationId: undefined,
    employeeId: undefined,
    reportDate: new Date().toISOString().slice(0, 10),
    qty: 1,
    qualifiedQty: 1,
    scrapQty: 0,
    workHours: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await reportWorkApi.detail(row.id)
  Object.assign(editing, JSON.parse(JSON.stringify(data)))
  dialogVisible.value = true
}

const handleRemove = (row: any) => {
  ElMessageBox.confirm(`确认删除报工单「${row.reportNo}」？`, '删除报工', { type: 'warning' }).then(async () => {
    await reportWorkApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(
    `确认审核报工单「${row.reportNo}」？审核后将归集人工成本到生产订单并生成凭证。`,
    '审核报工',
    { type: 'warning' }
  ).then(async () => {
    await reportWorkApi.audit(row.id)
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
      await reportWorkApi.update(editing)
    } else {
      await reportWorkApi.create(editing)
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
  const [moRes, empRes] = await Promise.all([
    moApi.page({ pageNum: 1, pageSize: 200 }),
    employeeApi.page({ pageNum: 1, pageSize: 200 })
  ])
  moList.value = moRes?.records || []
  employees.value = empRes?.records || []
})
</script>








