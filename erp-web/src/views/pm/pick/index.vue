<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">生产领料单</div>
        <div class="page-header__desc">登记生产领料出库</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="领料单号">
          <el-input v-model="query.pickNo" placeholder="领料单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
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
      <el-button v-perms="'pm:pick:add'" type="primary" :icon="Plus" @click="handleCreate">新增领料</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="pickNo" label="领料单号" min-width="150" />
        <el-table-column prop="moNo" label="生产订单" min-width="150" />
        <el-table-column prop="warehouseName" label="领料仓库" min-width="120" />
        <el-table-column prop="pickDate" label="领料日期" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'pm:pick:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'pm:pick:audit'" link type="success"
              @click="handleAudit(row)">审核过账</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑领料单' : '新增领料单'" width="880px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="生产订单" prop="moId">
          <el-select v-model="editing.moId" filterable placeholder="请选择已审核的生产订单" style="width: 300px"
            @change="handleMoChange">
            <el-option v-for="m in auditedOrders" :key="m.id" :label="`${m.moNo} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="领料仓库" prop="warehouseId">
          <el-select v-model="editing.warehouseId" filterable placeholder="请选择仓库" style="width: 200px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="领料日期" prop="pickDate">
          <el-date-picker v-model="editing.pickDate" type="date" value-format="YYYY-MM-DD" style="width: 140px" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>领料明细（按订单子项需求，不可超过未领数量）</span>
      </div>
      <el-table :data="editing.entries" border size="small">
        <el-table-column prop="materialCode" label="子项编码" min-width="120" />
        <el-table-column prop="materialName" label="子项物料" min-width="140" />
        <el-table-column label="未领数量" width="110">
          <template #default="{ row }">{{ row.remaining }}</template>
        </el-table-column>
        <el-table-column label="领料数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0.0001" :precision="4" size="small" controls-position="right"
              style="width: 120px" />
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="120">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" placeholder="批次" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.entries.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="领料单详情" width="820px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="领料单号">{{ detail.pickNo }}</el-descriptions-item>
        <el-descriptions-item label="生产订单">{{ detail.moNo }}</el-descriptions-item>
        <el-descriptions-item label="领料仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="领料日期">{{ detail.pickDate }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="materialCode" label="子项编码" min-width="120" />
        <el-table-column prop="materialName" label="子项物料" min-width="140" />
        <el-table-column prop="qty" label="领料数量" width="110" />
        <el-table-column prop="costPrice" label="成本单价" width="110" />
        <el-table-column prop="costAmount" label="成本额" width="110" />
        <el-table-column prop="batchNo" label="批次号" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { moApi, pickApi } from '@/api/production'
import { warehouseApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, pickNo: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const warehouses = ref<any[]>([])
const auditedOrders = ref<any[]>([])

const rules = {
  moId: [{ required: true, message: '请选择生产订单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择领料仓库', trigger: 'change' }],
  pickDate: [{ required: true, message: '请选择领料日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  moId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  pickDate: new Date().toISOString().slice(0, 10),
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await pickApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, pickNo: '', status: undefined })
  loadData()
}

const handleMoChange = async (moId: number) => {
  if (!moId) return
  const order = await moApi.detail(moId)
  editing.entries = (order.entries || [])
    .map((entry: any) => {
      const remaining = (Number(entry.requireQty) || 0) - (Number(entry.pickedQty) || 0)
      return {
        moEntryId: entry.id,
        materialId: entry.materialId,
        materialCode: entry.materialCode,
        materialName: entry.materialName,
        batchNo: '',
        remaining,
        qty: remaining > 0 ? remaining : 0,
        remark: ''
      }
    })
    .filter((entry: any) => entry.remaining > 0)
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await pickApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await pickApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('领料明细为空，请先选择生产订单')
      return
    }
    for (const row of editing.entries) {
      if (!row.qty || row.qty <= 0) {
        ElMessage.warning('领料数量必须大于0')
        return
      }
      if (row.qty > row.remaining) {
        ElMessage.warning(`领料数量超过未领数量：${row.materialName}`)
        return
      }
    }
    saving.value = true
    try {
      const payload = {
        id: editing.id,
        moId: editing.moId,
        warehouseId: editing.warehouseId,
        pickDate: editing.pickDate,
        remark: editing.remark,
        entries: editing.entries.map((row: any) => ({
          moEntryId: row.moEntryId,
          qty: row.qty,
          batchNo: row.batchNo,
          remark: row.remark
        }))
      }
      if (editing.id) {
        await pickApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await pickApi.create(payload)
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
    `确认审核领料单「${row.pickNo}」？审核后将扣减库存并生成凭证（借：生产成本 / 贷：原材料）。`,
    '审核过账',
    { type: 'warning' }
  ).then(async () => {
    await pickApi.audit(row.id)
    ElMessage.success('审核成功，库存已扣减')
    loadData()
  })
}

onMounted(async () => {
  warehouses.value = (await warehouseApi.page({ pageNum: 1, pageSize: 200 })).records
  auditedOrders.value = (await moApi.page({ pageNum: 1, pageSize: 100 })).records.filter(
    (order: any) => order.status === 1 || order.status === 2
  )
  loadData()
})
</script>

<style scoped>
.entry-toolbar {
  margin-bottom: 8px;
  font-weight: 600;
}
</style>








