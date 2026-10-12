<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">员工档案</div>
        <div class="page-header__desc">维护员工基本信息与在职状态</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="工号">
          <el-input v-model="query.employeeNo" placeholder="工号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.employeeName" placeholder="姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="query.deptId" :data="deptTree"
            :props="{ label: 'deptName', children: 'children', value: 'id' }" check-strictly clearable
            placeholder="全部部门" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="在职" :value="0" />
            <el-option label="离职" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'hr:employee:add'" type="primary" :icon="Plus" @click="handleCreate">新增员工</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="employeeNo" label="工号" min-width="130" />
        <el-table-column prop="employeeName" label="姓名" min-width="100" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" min-width="130" />
        <el-table-column prop="position" label="岗位" min-width="120" />
        <el-table-column prop="entryDate" label="入职日期" width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">
              {{ row.status === 0 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'hr:employee:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'hr:employee:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑员工' : '新增员工'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="所属部门" prop="deptId">
          <el-tree-select v-model="editing.deptId" :data="deptTree"
            :props="{ label: 'deptName', children: 'children', value: 'id' }" check-strictly placeholder="请选择部门"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="姓名" prop="employeeName">
          <el-input v-model="editing.employeeName" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editing.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="岗位">
          <el-input v-model="editing.position" />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="editing.entryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editing.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editing.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editing.status">
            <el-radio :value="0">在职</el-radio>
            <el-radio :value="1">离职</el-radio>
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
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { deptApi, employeeApi } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const deptTree = ref<any[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  employeeNo: '',
  employeeName: '',
  deptId: undefined as number | undefined,
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const rules = {
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  employeeName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const emptyEditing = () => ({
  id: undefined,
  deptId: undefined as number | undefined,
  employeeName: '',
  gender: 1,
  position: '',
  entryDate: new Date().toISOString().slice(0, 10),
  phone: '',
  email: '',
  status: 0,
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await employeeApi.page(query)
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
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    employeeNo: '',
    employeeName: '',
    deptId: undefined,
    status: undefined
  })
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
      if (editing.id) {
        await employeeApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await employeeApi.create({ ...editing })
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
  ElMessageBox.confirm(`确认删除员工「${row.employeeName}」？`, '提示', { type: 'warning' }).then(async () => {
    await employeeApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  deptTree.value = await deptApi.tree()
  loadData()
})
</script>








