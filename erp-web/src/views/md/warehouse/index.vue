<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">仓库管理</div>
        <div class="page-header__desc">维护仓库档案、仓库类型与库存控制策略</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="仓库编码">
          <el-input v-model="query.warehouseCode" placeholder="仓库编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="仓库名称">
          <el-input v-model="query.warehouseName" placeholder="仓库名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="仓库类型">
          <el-select v-model="query.whType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in whTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
      <el-button v-perms="'md:warehouse:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="warehouseCode" label="仓库编码" min-width="120" />
        <el-table-column prop="warehouseName" label="仓库名称" min-width="150" />
        <el-table-column label="仓库类型" width="110">
          <template #default="{ row }">{{ whTypeText(row.whType) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="库位" min-width="140" show-overflow-tooltip />
        <el-table-column label="允许负库存" width="100">
          <template #default="{ row }">
            <el-tag :type="row.allowNegative === 1 ? 'warning' : 'info'">{{ row.allowNegative === 1 ? '允许' : '不允许' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="冻结库存" width="100">
          <template #default="{ row }">
            <el-tag :type="row.freezeStock === 1 ? 'danger' : 'info'">{{ row.freezeStock === 1 ? '已冻结' : '未冻结' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'md:warehouse:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'md:warehouse:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑仓库' : '新增仓库'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="editing.warehouseCode" placeholder="如 WH01" />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="editing.warehouseName" />
        </el-form-item>
        <el-form-item label="仓库类型" prop="whType">
          <el-select v-model="editing.whType" style="width: 100%">
            <el-option v-for="item in whTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位">
          <el-input v-model="editing.location" placeholder="仓库所在位置" />
        </el-form-item>
        <el-form-item label="允许负库存">
          <el-radio-group v-model="editing.allowNegative">
            <el-radio :value="0">不允许</el-radio>
            <el-radio :value="1">允许</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="冻结库存">
          <el-radio-group v-model="editing.freezeStock">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
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
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { warehouseApi, type WarehouseVO } from '@/api/master'

/** 仓库类型：1 实仓 2 不良品仓 3 在制品仓 4 待检仓 */
const whTypeOptions = [
  { label: '实仓', value: 1 },
  { label: '不良品仓', value: 2 },
  { label: '在制品仓', value: 3 },
  { label: '待检仓', value: 4 }
]

const whTypeText = (type: number) => whTypeOptions.find((item) => item.value === type)?.label ?? '-'

const loading = ref(false)
const saving = ref(false)
const list = ref<WarehouseVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  warehouseCode: '',
  warehouseName: '',
  whType: undefined as number | undefined,
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const emptyEditing = () => ({
  id: undefined,
  warehouseCode: '',
  warehouseName: '',
  whType: 1,
  location: '',
  allowNegative: 0,
  freezeStock: 0,
  status: 0,
  remark: ''
})

const rules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  whType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await warehouseApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, warehouseCode: '', warehouseName: '', whType: undefined, status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: WarehouseVO) => {
  Object.assign(editing, emptyEditing(), row)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editing.id) {
        await warehouseApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await warehouseApi.create({ ...editing })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: WarehouseVO) => {
  ElMessageBox.confirm(`确认删除仓库「${row.warehouseName}」？`, '提示', { type: 'warning' }).then(async () => {
    await warehouseApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>
