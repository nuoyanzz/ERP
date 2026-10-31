<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">社保个税政策</div>
        <div class="page-header__desc">维护社保公积金缴纳比例、个税起征点与 7 档税率速算扣除数</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="政策名称">
          <el-input v-model="query.policyName" placeholder="政策名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'hr:policy:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="policyName" label="政策名称" min-width="160" />
        <el-table-column prop="effectiveDate" label="生效日期" width="120" />
        <el-table-column label="社保比例%" width="110" align="right">
          <template #default="{ row }">{{ Number(row.socialRate || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="公积金比例%" width="120" align="right">
          <template #default="{ row }">{{ Number(row.fundRate || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="个税起征点" width="120" align="right">
          <template #default="{ row }">{{ Number(row.taxThreshold || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="税率" min-width="180">
          <template #default="{ row }">
            <span class="rate-text">{{ rateSummary(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'hr:policy:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'hr:policy:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑政策' : '新增政策'" width="760px" top="6vh">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="110px">
        <el-form-item label="政策名称" prop="policyName">
          <el-input v-model="editing.policyName" placeholder="如 2026 年标准" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="editing.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="社保比例%">
          <el-input-number v-model="editing.socialRate" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="公积金比例%">
          <el-input-number v-model="editing.fundRate" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="个税起征点">
          <el-input-number v-model="editing.taxThreshold" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>

        <el-divider content-position="left">个税税率与速算扣除数</el-divider>

        <el-table :data="taxRows" border size="small" class="tax-table">
          <el-table-column label="档位" width="70">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="税率%" min-width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.taxRate" :min="0" :max="100" :precision="2" size="small"
                controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="速算扣除数" min-width="180">
            <template #default="{ row }">
              <el-input-number v-model="row.taxDeduction" :min="0" :precision="2" size="small" controls-position="right"
                style="width: 100%" />
            </template>
          </el-table-column>
        </el-table>

        <el-form-item label="状态" style="margin-top: 16px">
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
import { socialPolicyApi } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

/** 7 档个税税率与速算扣除数，编辑时作为表单的可编辑副本 */
const taxRows = ref<{ taxRate: number; taxDeduction: number }[]>([])

const query = reactive({ policyName: '' })

const rules = {
  policyName: [{ required: true, message: '请输入政策名称', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  policyName: '',
  effectiveDate: '',
  socialRate: 0,
  fundRate: 0,
  taxThreshold: 0,
  status: 0,
  remark: ''
})

const rateSummary = (row: any) => {
  const parts: string[] = []
  for (let i = 1; i <= 7; i++) {
    const rate = row[`taxRate${i}`]
    if (rate !== undefined && rate !== null) {
      parts.push(`${Number(rate).toFixed(2)}%`)
    }
  }
  return parts.length ? parts.join(' / ') : '-'
}

const loadData = async () => {
  loading.value = true
  try {
    list.value = await socialPolicyApi.list()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  const keyword = query.policyName.trim()
  list.value = keyword
    ? list.value.filter((item) => String(item.policyName || '').includes(keyword))
    : list.value
}

const handleReset = () => {
  query.policyName = ''
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  taxRows.value = Array.from({ length: 7 }, () => ({ taxRate: 0, taxDeduction: 0 }))
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(editing, emptyEditing(), row)
  taxRows.value = Array.from({ length: 7 }, (_, index) => ({
    taxRate: Number(row[`taxRate${index + 1}`] || 0),
    taxDeduction: Number(row[`taxDeduction${index + 1}`] || 0)
  }))
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload: any = {
        id: editing.id,
        policyName: editing.policyName,
        effectiveDate: editing.effectiveDate,
        socialRate: editing.socialRate,
        fundRate: editing.fundRate,
        taxThreshold: editing.taxThreshold,
        status: editing.status,
        remark: editing.remark
      }
      taxRows.value.forEach((row, index) => {
        payload[`taxRate${index + 1}`] = row.taxRate
        payload[`taxDeduction${index + 1}`] = row.taxDeduction
      })
      if (editing.id) {
        await socialPolicyApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await socialPolicyApi.create(payload)
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
  ElMessageBox.confirm(`确认删除政策「${row.policyName}」？`, '提示', { type: 'warning' }).then(async () => {
    await socialPolicyApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.rate-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.tax-table {
  width: 100%;
}
</style>
