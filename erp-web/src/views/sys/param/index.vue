<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">系统参数</div>
        <div class="page-header__desc">维护系统级可配置参数，供业务代码按参数键读取</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="参数键">
          <el-input v-model="query.paramKey" placeholder="参数键" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="参数名称">
          <el-input v-model="query.paramName" placeholder="参数名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="参数类型">
          <el-select v-model="query.paramType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in paramTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
      <el-button v-perms="'sys:param:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="paramKey" label="参数键" min-width="200" />
        <el-table-column prop="paramValue" label="参数值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="paramName" label="参数名称" min-width="160" />
        <el-table-column label="参数类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ paramTypeText(row.paramType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'sys:param:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'sys:param:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑参数' : '新增参数'" width="520px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="参数键" prop="paramKey">
          <el-input v-model="editing.paramKey" placeholder="如 erp.default_tax_rate" />
        </el-form-item>
        <el-form-item label="参数值" prop="paramValue">
          <el-input v-model="editing.paramValue" type="textarea" :rows="3" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="参数名称">
          <el-input v-model="editing.paramName" />
        </el-form-item>
        <el-form-item label="参数类型" prop="paramType">
          <el-select v-model="editing.paramType" style="width: 100%">
            <el-option v-for="item in paramTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
import { paramApi, type ParamVO } from '@/api/system'

const paramTypeOptions = [
  { label: '字符串 STRING', value: 'STRING' },
  { label: '数字 NUMBER', value: 'NUMBER' },
  { label: '布尔 BOOLEAN', value: 'BOOLEAN' },
  { label: 'JSON', value: 'JSON' }
]

const paramTypeText = (type: string) =>
  paramTypeOptions.find((item) => item.value === type)?.label.split(' ')[0] ?? type ?? '-'

const loading = ref(false)
const saving = ref(false)
const list = ref<ParamVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  paramKey: '',
  paramName: '',
  paramType: '',
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const emptyEditing = () => ({
  id: undefined,
  paramKey: '',
  paramValue: '',
  paramName: '',
  paramType: 'STRING',
  status: 0,
  remark: ''
})

const rules = {
  paramKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  paramType: [{ required: true, message: '请选择参数类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await paramApi.page(query)
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
    paramKey: '',
    paramName: '',
    paramType: '',
    status: undefined
  })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: ParamVO) => {
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
        paramKey: editing.paramKey,
        paramValue: editing.paramValue,
        paramName: editing.paramName,
        paramType: editing.paramType,
        status: editing.status,
        remark: editing.remark
      }
      if (editing.id) {
        await paramApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await paramApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: ParamVO) => {
  ElMessageBox.confirm(`确认删除参数「${row.paramKey}」？`, '提示', { type: 'warning' }).then(async () => {
    await paramApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>
