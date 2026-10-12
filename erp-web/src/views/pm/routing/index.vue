<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">工艺路线</div>
        <div class="page-header__desc">维护产品的工序路线、标准工时与费率</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="路线编号">
          <el-input v-model="query.routingNo" placeholder="路线编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="生产物料">
          <el-select v-model="query.materialId" filterable placeholder="全部" clearable style="width: 220px">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`"
              :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'pm:routing:add'" type="primary" :icon="Plus" @click="handleCreate">新增路线</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="routingNo" label="路线编号" min-width="150" />
        <el-table-column prop="materialCode" label="物料编码" width="130" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="effectiveDate" label="生效日期" width="120" />
        <el-table-column prop="operationCount" label="工序数" width="90" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'pm:routing:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'pm:routing:remove'" link type="danger" @click="handleRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-page="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑工艺路线' : '新增工艺路线'" width="960px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="90px">
        <el-form-item label="生产物料" prop="materialId">
          <el-select v-model="editing.materialId" filterable placeholder="请选择物料" style="width: 240px">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`"
              :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="editing.version" placeholder="V1" style="width: 100px" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="editing.effectiveDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择日期" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>工序明细</span>
        <el-button size="small" :icon="Plus" @click="handleAddOperation">添加工序</el-button>
      </div>
      <el-table :data="editing.operations" border size="small">
        <el-table-column label="工序号" width="110">
          <template #default="{ row }">
            <el-input v-model="row.operationNo" size="small" placeholder="OP10" />
          </template>
        </el-table-column>
        <el-table-column label="工序名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.operationName" size="small" placeholder="装配" />
          </template>
        </el-table-column>
        <el-table-column label="工作中心" width="160">
          <template #default="{ row }">
            <el-select v-model="row.workCenterId" filterable placeholder="请选择" size="small" style="width: 100%">
              <el-option v-for="w in workCenters" :key="w.id" :label="w.centerName" :value="w.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="标准工时" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.standardHours" :min="0" :precision="4" size="small"
              controls-position="right" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column label="人工费率" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.laborRate" :min="0" :precision="4" size="small"
              controls-position="right" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column label="制造费率" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.overheadRate" :min="0" :precision="4" size="small"
              controls-position="right" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.operations.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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
import { routingApi } from '@/api/production'
import { materialApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  routingNo: '',
  materialId: undefined as number | undefined
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ operations: [] as any[] })
const materials = ref<any[]>([])
const workCenters = ref<any[]>([])

const rules = {
  materialId: [{ required: true, message: '请选择生产物料', trigger: 'change' }],
  version: [{ required: true, message: '请填写版本号', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await routingApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, routingNo: '', materialId: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, {
    id: undefined,
    materialId: undefined,
    version: 'V1',
    effectiveDate: new Date().toISOString().slice(0, 10),
    remark: '',
    operations: [] as any[]
  })
  handleAddOperation()
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await routingApi.detail(row.id)
  Object.assign(editing, JSON.parse(JSON.stringify(data)))
  if (!editing.operations) {
    editing.operations = []
  }
  dialogVisible.value = true
}

const handleRemove = (row: any) => {
  ElMessageBox.confirm(`确认删除工艺路线「${row.routingNo}」？`, '删除路线', { type: 'warning' }).then(async () => {
    await routingApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleAddOperation = () => {
  editing.operations.push({
    operationNo: '',
    operationName: '',
    workCenterId: undefined,
    standardHours: 0,
    standardQty: 1,
    laborRate: 0,
    overheadRate: 0
  })
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!editing.operations.length) {
    ElMessage.warning('请至少添加工序')
    return
  }
  saving.value = true
  try {
    if (editing.id) {
      await routingApi.update(editing)
    } else {
      await routingApi.create(editing)
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
  const [materialRes, centerRes] = await Promise.all([
    materialApi.list(),
    routingApi.workCenters()
  ])
  materials.value = materialRes || []
  workCenters.value = centerRes || []
})
</script>








