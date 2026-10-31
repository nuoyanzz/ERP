<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">汇率维护</div>
        <div class="page-header__desc">维护各币别按期间的折算汇率，用于外币业务折算与期末调汇</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="币别">
          <el-input v-model="query.currency" placeholder="如 USD" clearable style="width: 120px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="期间">
          <el-input v-model="query.period" placeholder="如 2026-01" clearable style="width: 140px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'fi:rate:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="currency" label="币别" width="100" />
        <el-table-column prop="period" label="期间" width="120" />
        <el-table-column label="汇率" width="130" align="right">
          <template #default="{ row }">{{ Number(row.rate || 0).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="汇率类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.rateType === 2 ? 'warning' : 'primary'">{{ rateTypeText(row.rateType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'fi:rate:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'fi:rate:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑汇率' : '新增汇率'" width="480px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="币别" prop="currency">
          <el-input v-model="editing.currency" placeholder="如 USD" />
        </el-form-item>
        <el-form-item label="期间" prop="period">
          <el-date-picker v-model="editing.period" type="month" value-format="YYYY-MM" placeholder="选择期间"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="汇率" prop="rate">
          <el-input-number v-model="editing.rate" :min="0" :precision="4" :step="0.01" style="width: 100%" />
        </el-form-item>
        <el-form-item label="汇率类型" prop="rateType">
          <el-radio-group v-model="editing.rateType">
            <el-radio :value="1">期间汇率</el-radio>
            <el-radio :value="2">调整汇率</el-radio>
          </el-radio-group>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { rateApi, type ExchangeRateVO } from '@/api/finance'

const rateTypeText = (type: number) => (type === 2 ? '调整汇率' : '期间汇率')

const loading = ref(false)
const saving = ref(false)
const list = ref<ExchangeRateVO[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, currency: '', period: '' })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const emptyEditing = () => ({
  id: undefined,
  currency: '',
  period: new Date().toISOString().slice(0, 7),
  rate: 1,
  rateType: 1
})

const rules: FormRules = {
  currency: [{ required: true, message: '请输入币别', trigger: 'blur' }],
  period: [{ required: true, message: '请选择期间', trigger: 'change' }],
  rate: [
    { required: true, message: '请输入汇率', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || value === null || Number(value) <= 0) {
          callback(new Error('汇率必须大于 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await rateApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, currency: '', period: '' })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: ExchangeRateVO) => {
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
        currency: editing.currency,
        period: editing.period,
        rate: editing.rate,
        rateType: editing.rateType
      }
      if (editing.id) {
        await rateApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await rateApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: ExchangeRateVO) => {
  ElMessageBox.confirm(`确认删除 ${row.currency} 在 ${row.period} 的汇率？`, '提示', { type: 'warning' }).then(async () => {
    await rateApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>
