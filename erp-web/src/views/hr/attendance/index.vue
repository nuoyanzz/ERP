<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">考勤管理</div>
        <div class="page-header__desc">记录员工上下班打卡、工时、加班与请假</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="员工">
          <el-select v-model="query.employeeId" filterable clearable placeholder="全部" style="width: 200px">
            <el-option v-for="item in employees" :key="item.id" :label="`${item.employeeNo} ${item.employeeName}`"
              :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" @change="handleDateChange" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="停用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'hr:attendance:add'" type="primary" :icon="Plus" @click="handleCreate">录入考勤</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="employeeNo" label="工号" min-width="100" />
        <el-table-column prop="employeeName" label="员工" min-width="100" />
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column prop="workDate" label="考勤日期" width="110" />
        <el-table-column prop="shiftCode" label="班次" width="90" />
        <el-table-column prop="clockIn" label="上班打卡" width="160">
          <template #default="{ row }">{{ row.clockIn || '-' }}</template>
        </el-table-column>
        <el-table-column prop="clockOut" label="下班打卡" width="160">
          <template #default="{ row }">{{ row.clockOut || '-' }}</template>
        </el-table-column>
        <el-table-column label="工时" width="90" align="right">
          <template #default="{ row }">{{ Number(row.workHours || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="加班" width="90" align="right">
          <template #default="{ row }">{{ Number(row.overtimeHours || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="leaveType" label="请假类型" width="100">
          <template #default="{ row }">{{ leaveTypeText(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column label="请假时长" width="100" align="right">
          <template #default="{ row }">{{ Number(row.leaveHours || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'hr:attendance:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'hr:attendance:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑考勤' : '录入考勤'" width="580px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="员工" prop="employeeId">
          <el-select v-model="editing.employeeId" filterable placeholder="请选择员工" style="width: 100%">
            <el-option v-for="item in employees" :key="item.id" :label="`${item.employeeNo} ${item.employeeName}`"
              :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤日期" prop="workDate">
          <el-date-picker v-model="editing.workDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="班次">
          <el-input v-model="editing.shiftCode" placeholder="如 DAY / NIGHT" />
        </el-form-item>
        <el-form-item label="上班打卡">
          <el-date-picker v-model="editing.clockIn" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下班打卡">
          <el-date-picker v-model="editing.clockOut" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工时">
          <el-input-number v-model="editing.workHours" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加班时长">
          <el-input-number v-model="editing.overtimeHours" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请假类型">
          <el-select v-model="editing.leaveType" clearable placeholder="无请假" style="width: 100%">
            <el-option v-for="item in leaveTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假时长">
          <el-input-number v-model="editing.leaveHours" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editing.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { attendanceApi, employeeApi } from '@/api/hr'

const leaveTypeOptions = [
  { label: '事假', value: 'PERSONAL' },
  { label: '病假', value: 'SICK' },
  { label: '年假', value: 'ANNUAL' },
  { label: '婚假', value: 'MARRIAGE' },
  { label: '产假', value: 'MATERNITY' },
  { label: '丧假', value: 'FUNERAL' },
  { label: '调休', value: 'TIME_OFF' }
]

const leaveTypeText = (value: string) => leaveTypeOptions.find((item) => item.value === value)?.label || '-'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const employees = ref<any[]>([])
const dateRange = ref<string[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  employeeId: undefined as number | undefined,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
  status: undefined as number | undefined
})

const emptyEditing = () => ({
  id: undefined,
  employeeId: undefined as number | undefined,
  workDate: new Date().toISOString().slice(0, 10),
  shiftCode: 'DAY',
  clockIn: '',
  clockOut: '',
  workHours: 8,
  overtimeHours: 0,
  leaveType: '',
  leaveHours: 0,
  status: 0,
  remark: ''
})

const rules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择考勤日期', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await attendanceApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleDateChange = (value: string[] | null) => {
  query.startDate = value?.[0]
  query.endDate = value?.[1]
  query.pageNum = 1
  loadData()
}

const handleSearch = () => {
  query.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    employeeId: undefined,
    startDate: undefined,
    endDate: undefined,
    status: undefined
  })
  dateRange.value = []
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(editing, emptyEditing(), row)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        id: editing.id,
        employeeId: editing.employeeId,
        workDate: editing.workDate,
        shiftCode: editing.shiftCode,
        clockIn: editing.clockIn,
        clockOut: editing.clockOut,
        workHours: editing.workHours,
        overtimeHours: editing.overtimeHours,
        leaveType: editing.leaveType,
        leaveHours: editing.leaveHours,
        status: editing.status,
        remark: editing.remark
      }
      if (editing.id) {
        await attendanceApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await attendanceApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确认删除「${row.employeeName}」${row.workDate} 的考勤记录？`,
    '提示',
    { type: 'warning' }
  ).then(async () => {
    await attendanceApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  employees.value = (await employeeApi.page({ pageNum: 1, pageSize: 300, status: 0 })).records
  loadData()
})
</script>
