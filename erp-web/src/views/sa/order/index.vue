<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">销售订单</div>
        <div class="page-header__desc">管理销售订单的创建、审核与发货</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="订单编号">
          <el-input v-model="query.orderNo" placeholder="订单编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="暂存" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'sa:order:add'" type="primary" :icon="Plus" @click="handleCreate">新增订单</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="orderNo" label="订单编号" min-width="150" />
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column prop="orderDate" label="订单日期" width="120" />
        <el-table-column prop="totalAmount" label="价税合计" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'sa:order:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'sa:order:audit'" link type="success"
              @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 1" v-perms="'sa:order:close'" link type="warning"
              @click="handleClose(row)">关闭</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑销售订单' : '新增销售订单'" width="900px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="editing.customerId" filterable placeholder="请选择客户" style="width: 220px">
            <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单日期" prop="orderDate">
          <el-date-picker v-model="editing.orderDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="交货日期">
          <el-date-picker v-model="editing.deliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>订单明细</span>
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
        <el-table-column label="价税合计" width="120">
          <template #default="{ row }">{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.entries.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="total-line">订单合计：{{ totalAmount }}</div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="销售订单详情" width="820px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="订单日期">{{ detail.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="交货日期">{{ detail.deliveryDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="价税合计">{{ detail.totalAmount }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="120" show-overflow-tooltip />
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column prop="price" label="单价" width="90" />
        <el-table-column prop="taxRate" label="税率%" width="80" />
        <el-table-column prop="amount" label="价税合计" width="110" />
        <el-table-column prop="deliveredQty" label="已发货" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { saOrderApi } from '@/api/sales'
import { customerApi, materialApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  status: undefined as number | undefined,
  startDate: '',
  endDate: ''
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const customers = ref<any[]>([])
const materials = ref<any[]>([])

const statusText = (status: number) =>
  ({ 0: '暂存', 1: '已审核', 2: '已完成', 3: '已关闭' } as Record<number, string>)[status] || '未知'
const statusType = (status: number) =>
  ({ 0: 'info', 1: 'success', 2: 'primary', 3: 'danger' } as Record<number, string>)[status] || 'info'

const totalAmount = computed(() =>
  editing.entries.reduce((sum: number, row: any) => sum + (Number(row.amount) || 0), 0).toFixed(2)
)

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  customerId: undefined as number | undefined,
  orderDate: new Date().toISOString().slice(0, 10),
  deliveryDate: '',
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await saOrderApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  if (dateRange.value) {
    query.startDate = dateRange.value[0]
    query.endDate = dateRange.value[1]
  } else {
    query.startDate = ''
    query.endDate = ''
  }
  loadData()
}

const handleReset = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, orderNo: '', status: undefined, startDate: '', endDate: '' })
  dateRange.value = null
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await saOrderApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await saOrderApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddEntry = () => {
  editing.entries.push({ materialId: undefined, qty: 1, price: 0, taxRate: 13, amount: 0, remark: '' })
}

const calcAmount = (row: any) => {
  const qty = Number(row.qty) || 0
  const price = Number(row.price) || 0
  const taxRate = Number(row.taxRate) || 0
  row.amount = (qty * price * (1 + taxRate / 100)).toFixed(2)
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('请添加订单明细')
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
        orderDate: editing.orderDate,
        deliveryDate: editing.deliveryDate || undefined,
        remark: editing.remark,
        entries: editing.entries
      }
      if (editing.id) {
        await saOrderApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await saOrderApi.create(payload)
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
  ElMessageBox.confirm(`确认审核订单「${row.orderNo}」？`, '提示', { type: 'warning' }).then(async () => {
    await saOrderApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

const handleClose = (row: any) => {
  ElMessageBox.prompt('请输入关闭原因', '关闭订单', { inputPlaceholder: '关闭原因' }).then(async ({ value }) => {
    await saOrderApi.close(row.id, value || '')
    ElMessage.success('已关闭')
    loadData()
  })
}

onMounted(async () => {
  customers.value = (await customerApi.page({ pageNum: 1, pageSize: 200 })).records
  materials.value = (await materialApi.page({ pageNum: 1, pageSize: 500 })).records
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








