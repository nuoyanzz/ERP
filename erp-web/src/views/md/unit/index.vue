<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">计量单位</div>
        <div class="page-header__desc">维护物料计量单位档案</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="单位编码">
          <el-input v-model="query.unitCode" placeholder="单位编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="单位名称">
          <el-input v-model="query.unitName" placeholder="单位名称" clearable @keyup.enter="handleSearch" />
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
      <el-button v-perms="'md:unit:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="unitCode" label="单位编码" min-width="120" />
        <el-table-column prop="unitName" label="单位名称" min-width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'md:unit:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'md:unit:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑计量单位' : '新增计量单位'" width="480px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="单位编码" prop="unitCode">
          <el-input v-model="editing.unitCode" placeholder="如 PCS / kg / m" />
        </el-form-item>
        <el-form-item label="单位名称" prop="unitName">
          <el-input v-model="editing.unitName" placeholder="如 个 / 千克 / 米" />
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
import { unitApi, type UnitVO } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<UnitVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  unitCode: '',
  unitName: '',
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const emptyEditing = () => ({
  id: undefined,
  unitCode: '',
  unitName: '',
  status: 0,
  remark: ''
})

const rules = {
  unitCode: [{ required: true, message: '请输入单位编码', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await unitApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, unitCode: '', unitName: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: UnitVO) => {
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
        await unitApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await unitApi.create({ ...editing })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: UnitVO) => {
  ElMessageBox.confirm(`确认删除计量单位「${row.unitName}」？`, '提示', { type: 'warning' }).then(async () => {
    await unitApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>
