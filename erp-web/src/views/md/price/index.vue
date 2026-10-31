<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">价格表</div>
        <div class="page-header__desc">维护客户 / 供应商的物料价格与生效期间</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="价格编号">
          <el-input v-model="query.priceNo" placeholder="价格编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.priceName" placeholder="名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="停用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'md:price:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="priceNo" label="价格编号" min-width="120" />
        <el-table-column prop="priceName" label="名称" min-width="140" />
        <el-table-column prop="customerName" label="客户" min-width="130">
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="130">
          <template #default="{ row }">{{ row.supplierName || '-' }}</template>
        </el-table-column>
        <el-table-column label="物料" min-width="180">
          <template #default="{ row }">
            {{ row.materialCode ? `${row.materialCode} ${row.materialName || ''}` : (row.materialName || '-') }}
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">{{ Number(row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="税率%" width="90" align="right">
          <template #default="{ row }">{{ Number(row.taxRate || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="currency" label="币别" width="80" />
        <el-table-column prop="effectiveDate" label="生效日期" width="110" />
        <el-table-column prop="expiryDate" label="失效日期" width="110" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'md:price:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'md:price:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑价格' : '新增价格'" width="600px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="价格编号" prop="priceNo">
          <el-input v-model="editing.priceNo" placeholder="如 PR-0001" />
        </el-form-item>
        <el-form-item label="名称" prop="priceName">
          <el-input v-model="editing.priceName" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="editing.customerId" filterable clearable placeholder="请选择客户" style="width: 100%">
            <el-option v-for="item in customers" :key="item.id" :label="`${item.customerCode} ${item.customerName}`"
              :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="editing.supplierId" filterable clearable placeholder="请选择供应商" style="width: 100%">
            <el-option v-for="item in suppliers" :key="item.id" :label="`${item.supplierCode} ${item.supplierName}`"
              :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="物料" prop="materialId">
          <el-select v-model="editing.materialId" filterable placeholder="请选择物料" style="width: 100%">
            <el-option v-for="item in materials" :key="item.id" :label="`${item.materialCode} ${item.materialName}`"
              :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价" prop="price">
          <el-input-number v-model="editing.price" :min="0" :precision="2" :step="0.01" style="width: 100%" />
        </el-form-item>
        <el-form-item label="税率%">
          <el-input-number v-model="editing.taxRate" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币别">
          <el-input v-model="editing.currency" placeholder="如 CNY" style="width: 140px" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="editing.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效日期" prop="expiryDate">
          <el-date-picker v-model="editing.expiryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { customerApi, materialApi, priceApi, supplierApi, type PriceVO } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<PriceVO[]>([])
const total = ref(0)
const customers = ref<any[]>([])
const suppliers = ref<any[]>([])
const materials = ref<any[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  priceNo: '',
  priceName: '',
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const emptyEditing = () => ({
  id: undefined,
  priceNo: '',
  priceName: '',
  customerId: undefined as number | undefined,
  supplierId: undefined as number | undefined,
  materialId: undefined as number | undefined,
  price: 0,
  taxRate: 0,
  currency: 'CNY',
  effectiveDate: '',
  expiryDate: '',
  status: 0,
  remark: ''
})

const rules: FormRules = {
  priceNo: [{ required: true, message: '请输入价格编号', trigger: 'blur' }],
  priceName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
  price: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || value === null || Number(value) <= 0) {
          callback(new Error('单价必须大于 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  expiryDate: [
    {
      validator: (_rule, value, callback) => {
        if (value && editing.effectiveDate && value < editing.effectiveDate) {
          callback(new Error('失效日期不能早于生效日期'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await priceApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, priceNo: '', priceName: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: PriceVO) => {
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
        priceNo: editing.priceNo,
        priceName: editing.priceName,
        customerId: editing.customerId,
        supplierId: editing.supplierId,
        materialId: editing.materialId,
        price: editing.price,
        taxRate: editing.taxRate,
        currency: editing.currency,
        effectiveDate: editing.effectiveDate,
        expiryDate: editing.expiryDate,
        status: editing.status,
        remark: editing.remark
      }
      if (editing.id) {
        await priceApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await priceApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: PriceVO) => {
  ElMessageBox.confirm(`确认删除价格「${row.priceNo}」？`, '提示', { type: 'warning' }).then(async () => {
    await priceApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  const [customerList, supplierList, materialList] = await Promise.all([
    customerApi.list(),
    supplierApi.list(),
    materialApi.list()
  ])
  customers.value = customerList
  suppliers.value = supplierList
  materials.value = materialList
  loadData()
})
</script>
