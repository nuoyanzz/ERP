<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">销售出库单</div>
        <div class="page-header__desc">登记销售出库并扣减库存</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="出库单号">
          <el-input v-model="query.outstockNo" placeholder="出库单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="暂存" :value="0" />
            <el-option label="已审核" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="凭证状态">
          <el-select v-model="query.voucherStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="未生成" :value="0" />
            <el-option label="已生成" :value="1" />
            <el-option label="生成失败" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'sa:outstock:add'" type="primary" :icon="Plus" @click="handleCreate">新增出库</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="outstockNo" label="出库单号" min-width="150" />
        <el-table-column prop="sourceOrderNo" label="来源订单" min-width="150" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="warehouseName" label="出库仓库" min-width="120" />
        <el-table-column prop="outstockDate" label="出库日期" width="120" />
        <el-table-column prop="totalAmount" label="售价合计" width="110" />
        <el-table-column prop="totalCost" label="成本合计" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="凭证" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.voucherStatus === 1" type="success">已生成</el-tag>
            <el-tag v-else-if="row.voucherStatus === 2" type="danger">失败</el-tag>
            <el-tag v-else type="info">未生成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'sa:outstock:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'sa:outstock:audit'" link type="success"
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

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑销售出库' : '新增销售出库'" width="900px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="editing.customerId" filterable placeholder="请选择客户" style="width: 220px">
            <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库仓库" prop="warehouseId">
          <el-select v-model="editing.warehouseId" filterable placeholder="请选择仓库" style="width: 220px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库日期" prop="outstockDate">
          <el-date-picker v-model="editing.outstockDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="来源订单">
          <el-select v-model="editing.orderId" filterable clearable placeholder="可选，按订单出库" style="width: 260px"
            @change="handleOrderChange">
            <el-option v-for="o in auditedOrders" :key="o.id" :label="`${o.orderNo} ${o.customerName}`" :value="o.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>出库明细</span>
        <el-button size="small" :icon="Plus" @click="handleAddEntry">添加行</el-button>
      </div>
      <el-table :data="editing.entries" border size="small">
        <el-table-column label="物料" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.materialId" filterable placeholder="请选择物料" size="small" style="width: 100%">
              <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`"
                :value="m.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0.0001" :precision="4" size="small" controls-position="right"
              style="width: 110px" @change="calcAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="4" size="small" controls-position="right"
              style="width: 110px" @change="calcAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="税率%" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.taxRate" :min="0" :precision="2" size="small" controls-position="right"
              style="width: 90px" @change="calcAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="120">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" placeholder="批次" />
          </template>
        </el-table-column>
        <el-table-column label="价税合计" width="110">
          <template #default="{ row }">{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.entries.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="total-line">售价合计：{{ totalAmount }}</div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="销售出库详情" width="860px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="出库单号">{{ detail.outstockNo }}</el-descriptions-item>
        <el-descriptions-item label="来源订单">{{ detail.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="出库日期">{{ detail.outstockDate }}</el-descriptions-item>
        <el-descriptions-item label="凭证号">{{ detail.voucherId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column prop="price" label="单价" width="90" />
        <el-table-column prop="amount" label="售价" width="100" />
        <el-table-column prop="costPrice" label="成本单价" width="100" />
        <el-table-column prop="costAmount" label="成本额" width="100" />
        <el-table-column prop="batchNo" label="批次号" width="110" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { saOrderApi, saOutstockApi } from '@/api/sales'
import { customerApi, materialApi, warehouseApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  outstockNo: '',
  status: undefined as number | undefined,
  voucherStatus: undefined as number | undefined
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const customers = ref<any[]>([])
const warehouses = ref<any[]>([])
const materials = ref<any[]>([])
const auditedOrders = ref<any[]>([])

const totalAmount = computed(() =>
  editing.entries.reduce((sum: number, row: any) => sum + (Number(row.amount) || 0), 0).toFixed(2)
)

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  outstockDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  customerId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  outstockDate: new Date().toISOString().slice(0, 10),
  orderId: undefined as number | undefined,
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await saOutstockApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, outstockNo: '', status: undefined, voucherStatus: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await saOutstockApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await saOutstockApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddEntry = () => {
  editing.entries.push({
    materialId: undefined,
    qty: 1,
    price: 0,
    taxRate: 13,
    batchNo: '',
    amount: 0,
    remark: ''
  })
}

const calcAmount = (row: any) => {
  const qty = Number(row.qty) || 0
  const price = Number(row.price) || 0
  const taxRate = Number(row.taxRate) || 0
  row.amount = (qty * price * (1 + taxRate / 100)).toFixed(2)
}

const handleOrderChange = async (orderId: number) => {
  if (!orderId) return
  const order = await saOrderApi.detail(orderId)
  editing.customerId = order.customerId
  editing.entries = (order.entries || []).map((entry: any) => {
    const remaining = (Number(entry.qty) || 0) - (Number(entry.deliveredQty) || 0)
    const row = {
      orderEntryId: entry.id,
      materialId: entry.materialId,
      qty: remaining > 0 ? remaining : 0,
      price: entry.price,
      taxRate: entry.taxRate,
      batchNo: '',
      amount: 0,
      remark: ''
    }
    calcAmount(row)
    return row
  })
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('请添加出库明细')
      return
    }
    for (const row of editing.entries) {
      if (!row.materialId) {
        ElMessage.warning('请选择物料')
        return
      }
      calcAmount(row)
    }
    saving.value = true
    try {
      const payload = {
        id: editing.id,
        customerId: editing.customerId,
        warehouseId: editing.warehouseId,
        outstockDate: editing.outstockDate,
        orderId: editing.orderId,
        remark: editing.remark,
        entries: editing.entries
      }
      if (editing.id) {
        await saOutstockApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await saOutstockApi.create(payload)
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
    `确认审核出库单「${row.outstockNo}」？审核后将扣减库存、结转销售成本并自动生成凭证与应收单。`,
    '审核过账',
    { type: 'warning' }
  ).then(async () => {
    await saOutstockApi.audit(row.id)
    ElMessage.success('审核成功，库存已扣减并生成凭证与应收')
    loadData()
  })
}

onMounted(async () => {
  customers.value = (await customerApi.page({ pageNum: 1, pageSize: 200 })).records
  warehouses.value = (await warehouseApi.page({ pageNum: 1, pageSize: 200 })).records
  materials.value = (await materialApi.page({ pageNum: 1, pageSize: 500 })).records
  auditedOrders.value = (await saOrderApi.page({ pageNum: 1, pageSize: 100, status: 1 })).records
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

.total-line {
  text-align: right;
  margin-top: 8px;
  font-weight: 600;
  color: #f56c6c;
}
</style>








