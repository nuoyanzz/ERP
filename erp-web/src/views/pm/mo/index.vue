<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">生产订单</div>
        <div class="page-header__desc">管理生产订单的下达、领料与完工</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="订单编号">
          <el-input v-model="query.moNo" placeholder="订单编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="暂存" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="进行中" :value="2" />
            <el-option label="已完工" :value="3" />
            <el-option label="已关闭" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'pm:mo:add'" type="primary" :icon="Plus" @click="handleCreate">新增生产订单</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="moNo" label="订单编号" min-width="150" />
        <el-table-column prop="materialName" label="生产物料" min-width="140" />
        <el-table-column prop="bomNo" label="BOM" min-width="140" />
        <el-table-column prop="qty" label="生产数量" width="100" />
        <el-table-column prop="producedQty" label="已完工" width="100" />
        <el-table-column prop="warehouseName" label="成品仓库" min-width="110" />
        <el-table-column prop="planStartDate" label="计划开工" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'pm:mo:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'pm:mo:audit'" link type="success"
              @click="handleAudit(row)">审核展开</el-button>
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 0" v-perms="'pm:mo:remove'" link type="danger"
              @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑生产订单' : '新增生产订单'" width="580px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="生产物料" prop="materialId">
          <el-select v-model="editing.materialId" filterable placeholder="请选择生产物料" style="width: 100%"
            @change="handleMaterialChange">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`"
              :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="BOM" prop="bomId">
          <el-select v-model="editing.bomId" filterable placeholder="请选择 BOM" style="width: 100%">
            <el-option v-for="b in bomOptions" :key="b.id" :label="`${b.bomNo} ${b.version}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="生产数量" prop="qty">
          <el-input-number v-model="editing.qty" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成品仓库" prop="warehouseId">
          <el-select v-model="editing.warehouseId" filterable placeholder="请选择成品仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开工">
          <el-date-picker v-model="editing.planStartDate" type="date" value-format="YYYY-MM-DD"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划完工">
          <el-date-picker v-model="editing.planEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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

    <el-dialog v-model="detailVisible" title="生产订单详情" width="820px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单编号">{{ detail.moNo }}</el-descriptions-item>
        <el-descriptions-item label="生产物料">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="BOM">{{ detail.bomNo }}</el-descriptions-item>
        <el-descriptions-item label="生产数量">{{ detail.qty }}</el-descriptions-item>
        <el-descriptions-item label="成品仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
      </el-descriptions>
      <div class="sub-title">子项需求（审核后由 BOM 自动展开）</div>
      <el-table :data="detail.entries || []" border size="small">
        <el-table-column prop="materialCode" label="子项编码" min-width="120" />
        <el-table-column prop="materialName" label="子项物料" min-width="140" />
        <el-table-column prop="requireQty" label="需求数量" width="110" />
        <el-table-column prop="pickedQty" label="已领数量" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { bomApi, moApi } from '@/api/production'
import { materialApi, warehouseApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, moNo: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})
const detail = reactive<any>({})
const materials = ref<any[]>([])
const warehouses = ref<any[]>([])
const bomOptions = ref<any[]>([])

const statusText = (status: number) =>
  ({ 0: '暂存', 1: '已审核', 2: '进行中', 3: '已完工', 4: '已关闭' } as Record<number, string>)[status] || '未知'
const statusType = (status: number) =>
  ({ 0: 'info', 1: 'success', 2: 'warning', 3: 'primary', 4: 'danger' } as Record<number, string>)[status] || 'info'

const rules = {
  materialId: [{ required: true, message: '请选择生产物料', trigger: 'change' }],
  bomId: [{ required: true, message: '请选择 BOM', trigger: 'change' }],
  qty: [{ required: true, message: '请输入生产数量', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择成品仓库', trigger: 'change' }],
  planStartDate: [{ required: true, message: '请选择计划开工日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  materialId: undefined as number | undefined,
  bomId: undefined as number | undefined,
  qty: 1,
  warehouseId: undefined as number | undefined,
  planStartDate: new Date().toISOString().slice(0, 10),
  planEndDate: '',
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await moApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, moNo: '', status: undefined })
  loadData()
}

const handleMaterialChange = async (materialId: number) => {
  if (!materialId) {
    bomOptions.value = []
    editing.bomId = undefined
    return
  }
  const data = await bomApi.page({ pageNum: 1, pageSize: 100, materialId, status: 1 })
  bomOptions.value = data.records
  if (bomOptions.value.length === 1) {
    editing.bomId = bomOptions.value[0].id
  }
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  bomOptions.value = []
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await moApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  const bomData = await bomApi.page({ pageNum: 1, pageSize: 100, materialId: data.materialId, status: 1 })
  bomOptions.value = bomData.records
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await moApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editing.id) {
        await moApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await moApi.create({ ...editing })
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
    `确认审核生产订单「${row.moNo}」？审核后将按 BOM 自动展开子项需求（需求量 = 生产数量 × 单位用量 × (1 + 损耗率)）。`,
    '审核生产订单',
    { type: 'warning' }
  ).then(async () => {
    await moApi.audit(row.id)
    ElMessage.success('审核成功，子项需求已展开')
    loadData()
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除生产订单「${row.moNo}」？`, '提示', { type: 'warning' }).then(async () => {
    await moApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  materials.value = (await materialApi.page({ pageNum: 1, pageSize: 500 })).records
  warehouses.value = (await warehouseApi.page({ pageNum: 1, pageSize: 200 })).records
  loadData()
})
</script>

<style scoped>
.sub-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>








