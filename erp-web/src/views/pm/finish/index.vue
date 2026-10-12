<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">完工入库单</div>
        <div class="page-header__desc">登记完工产品入库</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="完工单号">
          <el-input v-model="query.finishNo" placeholder="完工单号" clearable @keyup.enter="handleSearch" />
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
      <el-button v-perms="'pm:finish:add'" type="primary" :icon="Plus" @click="handleCreate">新增完工入库</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="finishNo" label="完工单号" min-width="150" />
        <el-table-column prop="moNo" label="生产订单" min-width="150" />
        <el-table-column prop="warehouseName" label="成品仓库" min-width="120" />
        <el-table-column prop="finishDate" label="完工日期" width="120" />
        <el-table-column prop="qty" label="完工数量" width="100" />
        <el-table-column prop="costAmount" label="成本额" width="110" />
        <el-table-column prop="voucherId" label="凭证ID" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'pm:finish:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'pm:finish:audit'" link type="success"
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

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑完工入库' : '新增完工入库'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="生产订单" prop="moId">
          <el-select v-model="editing.moId" filterable placeholder="请选择生产订单" style="width: 100%">
            <el-option v-for="m in productionOrders" :key="m.id"
              :label="`${m.moNo} ${m.materialName}（剩余 ${remainingQty(m)}）`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成品仓库" prop="warehouseId">
          <el-select v-model="editing.warehouseId" filterable placeholder="请选择成品仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="完工日期" prop="finishDate">
          <el-date-picker v-model="editing.finishDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="完工数量" prop="qty">
          <el-input-number v-model="editing.qty" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editing.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div class="tip">审核时按累计领料成本 ÷ 完工数量计算单位成本，回写物料成本价，并生成凭证：借：库存商品 / 贷：生产成本。</div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="完工入库详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="完工单号">{{ detail.finishNo }}</el-descriptions-item>
        <el-descriptions-item label="生产订单">{{ detail.moNo }}</el-descriptions-item>
        <el-descriptions-item label="成品仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="完工日期">{{ detail.finishDate }}</el-descriptions-item>
        <el-descriptions-item label="完工数量">{{ detail.qty }}</el-descriptions-item>
        <el-descriptions-item label="成本额">{{ detail.costAmount }}</el-descriptions-item>
        <el-descriptions-item label="凭证ID">{{ detail.voucherId || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { finishApi, moApi } from '@/api/production'
import { warehouseApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, finishNo: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})
const detail = reactive<any>({})
const warehouses = ref<any[]>([])
const productionOrders = ref<any[]>([])

const remainingQty = (order: any) => {
  const remaining = (Number(order.qty) || 0) - (Number(order.producedQty) || 0)
  return remaining > 0 ? remaining : 0
}

const rules = {
  moId: [{ required: true, message: '请选择生产订单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择成品仓库', trigger: 'change' }],
  finishDate: [{ required: true, message: '请选择完工日期', trigger: 'change' }],
  qty: [{ required: true, message: '请输入完工数量', trigger: 'blur' }]
}

const emptyEditing = () => ({
  id: undefined,
  moId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  finishDate: new Date().toISOString().slice(0, 10),
  qty: 1,
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await finishApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, finishNo: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await finishApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await finishApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const order = productionOrders.value.find((item: any) => item.id === editing.moId)
    if (order && editing.qty > remainingQty(order)) {
      ElMessage.warning('完工数量不能超过订单剩余数量')
      return
    }
    saving.value = true
    try {
      if (editing.id) {
        await finishApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await finishApi.create({ ...editing })
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
    `确认审核完工入库「${row.finishNo}」？审核后将增加成品库存、按领料成本结转并更新物料成本价。`,
    '审核过账',
    { type: 'warning' }
  ).then(async () => {
    await finishApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

onMounted(async () => {
  warehouses.value = (await warehouseApi.page({ pageNum: 1, pageSize: 200 })).records
  productionOrders.value = (await moApi.page({ pageNum: 1, pageSize: 200 })).records.filter(
    (order: any) => order.status === 1 || order.status === 2
  )
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








