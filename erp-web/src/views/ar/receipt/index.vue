<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">收款单</div>
        <div class="page-header__desc">登记收款单并核销应收单</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="收款单号">
          <el-input v-model="query.receiptNo" placeholder="收款单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="query.customerId" filterable clearable placeholder="全部客户" style="width: 200px">
            <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
          </el-select>
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
      <el-button v-perms="'ar:receipt:add'" type="primary" :icon="Plus" @click="handleCreate">新增收款</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="receiptNo" label="收款单号" min-width="150" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="receiptDate" label="收款日期" width="120" />
        <el-table-column prop="amount" label="收款金额" width="120" />
        <el-table-column prop="voucherId" label="凭证ID" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'ar:receipt:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'ar:receipt:audit'" link type="success"
              @click="handleAudit(row)">审核核销</el-button>
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑收款单' : '新增收款单'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="editing.customerId" filterable placeholder="请选择客户" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款日期" prop="receiptDate">
          <el-date-picker v-model="editing.receiptDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款金额" prop="amount">
          <el-input-number v-model="editing.amount" :min="0.0001" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editing.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div class="tip">审核时按「先到期先核销」自动分配至该客户未结清应收单，并生成凭证：借：银行存款 / 贷：应收账款。</div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="收款单详情" width="700px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="收款单号">{{ detail.receiptNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="收款日期">{{ detail.receiptDate }}</el-descriptions-item>
        <el-descriptions-item label="收款金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="凭证ID">{{ detail.voucherId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '已审核' : '暂存' }}</el-descriptions-item>
      </el-descriptions>
      <div class="sub-title">核销记录</div>
      <el-table :data="detail.verifies || []" border size="small">
        <el-table-column prop="receivableNo" label="应收单号" min-width="160" />
        <el-table-column prop="amount" label="核销金额" width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { arReceiptApi } from '@/api/ar-ap'
import { customerApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const customers = ref<any[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  receiptNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})
const detail = reactive<any>({})

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }]
}

const emptyEditing = () => ({
  id: undefined,
  customerId: undefined as number | undefined,
  receiptDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await arReceiptApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, receiptNo: '', customerId: undefined, status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await arReceiptApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await arReceiptApi.detail(row.id)
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
        await arReceiptApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await arReceiptApi.create({ ...editing })
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
  ElMessageBox.confirm(`确认删除收款单「${row.receiptNo}」？`, '提示', { type: 'warning' }).then(async () => {
    await arReceiptApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleAudit = (row: any) => {
  ElMessageBox.confirm(
    `确认审核收款单「${row.receiptNo}」？系统将按先到期先核销自动分配，并生成记账凭证。`,
    '审核核销',
    { type: 'warning' }
  ).then(async () => {
    await arReceiptApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

onMounted(async () => {
  customers.value = (await customerApi.page({ pageNum: 1, pageSize: 200 })).records
  loadData()
})
</script>

<style scoped>
.tip {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}
</style>








