<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">采购订单</div>
        <div class="page-header__desc">管理采购订单的创建、审核与到货</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="订单编号">
          <el-input v-model="query.orderNo" placeholder="订单编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="query.status" dict-type="pa_order_status" placeholder="全部" clearable
            style="width: 130px" />
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
      <el-button v-perms="'pa:order:add'" type="primary" :icon="Plus" @click="handleCreate">新增订单</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="orderNo" label="订单编号" min-width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="160" />
        <el-table-column prop="orderDate" label="订单日期" width="120" />
        <el-table-column prop="totalAmount" label="价税合计" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <DictTag dict-type="pa_order_status" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'pa:order:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'pa:order:audit'" link type="success"
              @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 1" v-perms="'pa:order:close'" link type="warning"
              @click="handleClose(row)">关闭</el-button>
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-page="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑采购订单' : '新增采购订单'" width="900px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="editing.supplierId" filterable placeholder="请选择供应商" style="width: 220px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
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

    <el-dialog v-model="detailVisible" title="采购订单详情" width="820px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="订单日期">{{ detail.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="交货日期">{{ detail.deliveryDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag dict-type="pa_order_status" :value="detail.status" />
        </el-descriptions-item>
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
        <el-table-column prop="receivedQty" label="已收货" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { paOrderApi } from '@/api/purchase'
import { materialApi, supplierApi } from '@/api/master'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'

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
const suppliers = ref<any[]>([])
const materials = ref<any[]>([])

const totalAmount = computed(() =>
  editing.entries.reduce((sum: number, row: any) => sum + (Number(row.amount) || 0), 0).toFixed(2)
)

const rules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  supplierId: undefined as number | undefined,
  orderDate: new Date().toISOString().slice(0, 10),
  deliveryDate: '',
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await paOrderApi.page(query)
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
  editing.entries.push({ materialId: undefined, qty: 1, price: 0, taxRate: 13, remark: '' })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await paOrderApi.detail(row.id)
  Object.assign(editing, JSON.parse(JSON.stringify(data)))
  if (!editing.entries || editing.entries.length === 0) {
    editing.entries = []
  }
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await paOrderApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddEntry = () => {
  editing.entries.push({ materialId: undefined, qty: 1, price: 0, taxRate: 13, remark: '' })
}

const calcAmount = (row: any) => {
  const qty = Number(row.qty || 0)
  const price = Number(row.price || 0)
  const taxRate = Number(row.taxRate || 0)
  row.amount = Number((qty * price * (1 + taxRate / 100)).toFixed(2))
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!editing.entries.length) {
    ElMessage.warning('请至少添加一行明细')
    return
  }
  editing.entries.forEach(calcAmount)
  saving.value = true
  try {
    if (editing.id) {
      await paOrderApi.update(editing)
    } else {
      await paOrderApi.create(editing)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(`确认审核采购订单「${row.orderNo}」？`, '审核订单', { type: 'warning' }).then(async () => {
    await paOrderApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

const handleClose = (row: any) => {
  ElMessageBox.confirm(`确认关闭采购订单「${row.orderNo}」？`, '关闭订单', { type: 'warning' }).then(async () => {
    await paOrderApi.close(row.id, { closeReason: '手工关闭' })
    ElMessage.success('关闭成功')
    loadData()
  })
}

onMounted(async () => {
  loadData()
  const [supplierRes, materialRes] = await Promise.all([
    supplierApi.list(),
    materialApi.list()
  ])
  suppliers.value = supplierRes || []
  materials.value = materialRes || []
})
</script>








