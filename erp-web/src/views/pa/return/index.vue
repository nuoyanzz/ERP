<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">采购退货单</div>
        <div class="page-header__desc">处理采购退货并冲减库存</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="退货单号">
          <el-input v-model="query.returnNo" placeholder="退货单号" clearable @keyup.enter="handleSearch" />
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
      <el-button v-perms="'pa:return:add'" type="primary" :icon="Plus" @click="handleCreate">新增退货</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="returnNo" label="退货单号" min-width="150" />
        <el-table-column prop="sourceInstockNo" label="来源入库单" min-width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column prop="warehouseName" label="退货仓库" min-width="120" />
        <el-table-column prop="returnDate" label="退货日期" width="120" />
        <el-table-column prop="totalAmount" label="价税合计" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'pa:return:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'pa:return:audit'" link type="success"
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

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑采购退货' : '新增采购退货'" width="860px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="来源入库单" prop="sourceInstockId">
          <el-select v-model="editing.sourceInstockId" filterable placeholder="请选择已审核的入库单" style="width: 280px"
            @change="handleSourceChange">
            <el-option v-for="i in auditedInstocks" :key="i.id" :label="`${i.instockNo} ${i.supplierName || ''}`"
              :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="退货日期" prop="returnDate">
          <el-date-picker v-model="editing.returnDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>退货明细</span>
      </div>
      <el-table :data="editing.entries" border size="small">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column label="可退数量" width="100">
          <template #default="{ row }">{{ row.remaining }}</template>
        </el-table-column>
        <el-table-column label="退货数量" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0.0001" :precision="4" size="small" controls-position="right"
              style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="100" />
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采购退货详情" width="760px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="退货单号">{{ detail.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="来源入库单">{{ detail.sourceInstockNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="退货日期">{{ detail.returnDate }}</el-descriptions-item>
        <el-descriptions-item label="价税合计">{{ detail.totalAmount }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column prop="amount" label="价税合计" width="110" />
        <el-table-column prop="batchNo" label="批次号" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { paReturnApi, paInstockApi } from '@/api/purchase'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, returnNo: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const auditedInstocks = ref<any[]>([])

const rules = {
  sourceInstockId: [{ required: true, message: '请选择来源入库单', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  sourceInstockId: undefined as number | undefined,
  returnDate: new Date().toISOString().slice(0, 10),
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await paReturnApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, returnNo: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await paReturnApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await paReturnApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleSourceChange = async (instockId: number) => {
  if (!instockId) return
  const instock = await paInstockApi.detail(instockId)
  editing.entries = (instock.entries || []).map((entry: any) => ({
    sourceEntryId: entry.id,
    materialId: entry.materialId,
    materialCode: entry.materialCode,
    materialName: entry.materialName,
    batchNo: entry.batchNo,
    remaining: (Number(entry.qty) || 0) - (Number(entry.returnedQty) || 0),
    qty: (Number(entry.qty) || 0) - (Number(entry.returnedQty) || 0),
    remark: ''
  }))
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('退货明细为空，请先选择来源入库单')
      return
    }
    for (const row of editing.entries) {
      if (!row.qty || row.qty <= 0) {
        ElMessage.warning('退货数量必须大于0')
        return
      }
      if (row.qty > row.remaining) {
        ElMessage.warning(`退货数量超过可退数量：${row.materialName}`)
        return
      }
    }
    saving.value = true
    try {
      await paReturnApi.create({
        sourceInstockId: editing.sourceInstockId,
        returnDate: editing.returnDate,
        remark: editing.remark,
        entries: editing.entries.map((row: any) => ({
          sourceEntryId: row.sourceEntryId,
          qty: row.qty,
          remark: row.remark
        }))
      })
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(
    `确认审核退货单「${row.returnNo}」？审核后将扣减库存并生成红字凭证冲销原入库。`,
    '审核过账',
    { type: 'warning' }
  ).then(async () => {
    await paReturnApi.audit(row.id)
    ElMessage.success('审核成功，库存已扣减并生成红字凭证')
    loadData()
  })
}

onMounted(async () => {
  auditedInstocks.value = (await paInstockApi.page({ pageNum: 1, pageSize: 100, status: 1 })).records
  loadData()
})
</script>

<style scoped>
.entry-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
}
</style>








