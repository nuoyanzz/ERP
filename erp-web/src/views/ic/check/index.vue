<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">库存盘点</div>
        <div class="page-header__desc">创建盘点单并记录盘点盈亏</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="盘点单号">
          <el-input v-model="query.checkNo" placeholder="盘点单号" clearable @keyup.enter="handleSearch" />
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
      <el-button v-perms="'ic:check:add'" type="primary" :icon="Plus" @click="handleCreate">新增盘点</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="checkNo" label="盘点单号" min-width="150" />
        <el-table-column prop="warehouseName" label="盘点仓库" min-width="120" />
        <el-table-column prop="checkDate" label="盘点日期" width="120" />
        <el-table-column prop="gainQty" label="盘盈数量" width="100" />
        <el-table-column prop="lossQty" label="盘亏数量" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已审核' : '暂存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="voucherId" label="凭证号" width="90" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perms="'ic:check:edit'" link type="primary"
              @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perms="'ic:check:audit'" link type="success"
              @click="handleAudit(row)">审核</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑盘点单' : '新增盘点单'" width="860px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="盘点仓库" prop="warehouseId">
          <el-select v-model="editing.warehouseId" filterable placeholder="盘点仓库" style="width: 200px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="editing.checkDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>盘点明细（按实盘数量录入，系统按实时账面数量计算盈亏）</span>
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
        <el-table-column label="批次号" width="120">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" size="small" placeholder="批次" />
          </template>
        </el-table-column>
        <el-table-column label="实盘数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.countQty" :min="0" :precision="4" size="small" controls-position="right"
              style="width: 120px" />
          </template>
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

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="盘点单详情" width="800px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="盘点单号">{{ detail.checkNo }}</el-descriptions-item>
        <el-descriptions-item label="盘点仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ detail.checkDate }}</el-descriptions-item>
        <el-descriptions-item label="盘盈数量">{{ detail.gainQty }}</el-descriptions-item>
        <el-descriptions-item label="盘亏数量">{{ detail.lossQty }}</el-descriptions-item>
        <el-descriptions-item label="凭证号">{{ detail.voucherId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="batchNo" label="批次号" width="100" />
        <el-table-column prop="bookQty" label="账面数量" width="100" />
        <el-table-column prop="countQty" label="实盘数量" width="100" />
        <el-table-column prop="gainQty" label="盘盈" width="90" />
        <el-table-column prop="lossQty" label="盘亏" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus } from '@element-plus/icons-vue'
import { checkApi } from '@/api/inventory'
import { materialApi, warehouseApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  checkNo: '',
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const warehouses = ref<any[]>([])
const materials = ref<any[]>([])

const rules = {
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  warehouseId: undefined as number | undefined,
  checkDate: new Date().toISOString().slice(0, 10),
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await checkApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, checkNo: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await checkApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await checkApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddEntry = () => {
  editing.entries.push({ materialId: undefined, countQty: 0, batchNo: '', remark: '' })
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('请添加盘点明细')
      return
    }
    for (const row of editing.entries) {
      if (!row.materialId) {
        ElMessage.warning('请选择物料')
        return
      }
    }
    saving.value = true
    try {
      const payload = {
        id: editing.id,
        warehouseId: editing.warehouseId,
        checkDate: editing.checkDate,
        remark: editing.remark,
        entries: editing.entries
      }
      if (editing.id) {
        await checkApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await checkApi.create(payload)
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
    `确认审核盘点单「${row.checkNo}」？审核后将按实时账面数量计算盘盈亏，调整库存并生成凭证。`,
    '审核过账',
    { type: 'warning' }
  ).then(async () => {
    await checkApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  })
}

onMounted(async () => {
  warehouses.value = (await warehouseApi.page({ pageNum: 1, pageSize: 200 })).records
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
</style>








