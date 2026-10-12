<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">物料管理</div>
        <div class="page-header__desc">维护物料主数据、分类、批次与序列号管理方式</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="物料编码">
          <el-input v-model="query.materialCode" placeholder="物料编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="query.materialName" placeholder="物料名称" clearable @keyup.enter="handleSearch" />
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
    <el-button v-perms="'md:material:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
  </div>

  <el-card shadow="never">
    <el-table v-loading="loading" :data="list" border>
      <el-table-column type="index" label="#" width="45" />
      <el-table-column prop="materialCode" label="物料编码" min-width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="spec" label="规格型号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="批次/序列号" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.trackBatch === 1" size="small" type="warning">批次</el-tag>
          <el-tag v-if="row.trackSn === 1" size="small" type="danger" style="margin-left: 4px">序列号</el-tag>
          <span v-if="!row.trackBatch && !row.trackSn">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="costPrice" label="成本价" width="100" />
      <el-table-column prop="safeStock" label="安全库存" width="90" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-perms="'md:material:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-perms="'md:material:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
        @size-change="loadData" @current-change="loadData" />
    </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑物料' : '新增物料'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px" class="form-grid">
        <el-form-item label="物料编码" prop="materialCode">
          <el-input v-model="editing.materialCode" placeholder="如 RM-0001" />
        </el-form-item>
        <el-form-item label="物料名称" prop="materialName">
          <el-input v-model="editing.materialName" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="editing.spec" />
        </el-form-item>
        <el-form-item label="物料分类">
          <el-tree-select v-model="editing.categoryId" :data="categoryTree"
            :props="{ label: 'categoryName', children: 'children', value: 'id' }" check-strictly clearable
            placeholder="请选择分类" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="editing.unit" placeholder="如 个 / kg / 米" />
        </el-form-item>
        <el-form-item label="管理方式">
          <el-checkbox v-model="editing.trackBatch" :true-value="1" :false-value="0">批次管理</el-checkbox>
          <el-checkbox v-model="editing.trackSn" :true-value="1" :false-value="0">序列号管理</el-checkbox>
        </el-form-item>
        <el-form-item label="安全库存">
          <el-input-number v-model="editing.safeStock" :min="0" :precision="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editing.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" class="form-grid__full">
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
import { materialApi, type CategoryTreeVO, type MaterialVO } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<MaterialVO[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, materialCode: '', materialName: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const categoryTree = ref<CategoryTreeVO[]>([])
const editing = reactive<any>({})

const emptyEditing = () => ({
  id: undefined,
  materialCode: '',
  materialName: '',
  spec: '',
  categoryId: undefined as number | undefined,
  unit: '',
  trackBatch: 0,
  trackSn: 0,
  safeStock: 0,
  status: 0,
  remark: ''
})

const rules = {
  materialCode: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await materialApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, materialCode: '', materialName: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: MaterialVO) => {
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
        await materialApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await materialApi.create({ ...editing })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: MaterialVO) => {
  ElMessageBox.confirm(`确认删除物料「${row.materialName}」？`, '提示', { type: 'warning' }).then(async () => {
    await materialApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  categoryTree.value = await materialApi.categoryTree()
  loadData()
})
</script>

<style scoped>
/* 对话框表单两列布局：通栏项 */
.form-grid__full {
  grid-column: 1 / -1;
}
</style>








