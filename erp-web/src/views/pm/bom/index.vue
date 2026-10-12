<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">BOM 物料清单</div>
        <div class="page-header__desc">维护产品结构与物料层级、版本</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-button v-perms="'pm:bom:add'" type="primary" :icon="Plus" @click="handleCreate">新增 BOM</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="bomNo" label="BOM 编号" min-width="150" />
        <el-table-column prop="materialCode" label="父项编码" min-width="120" />
        <el-table-column prop="materialName" label="父项物料" min-width="140" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-button v-perms="'pm:bom:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'pm:bom:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑 BOM' : '新增 BOM'" width="860px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="父项物料" prop="materialId">
          <el-select v-model="editing.materialId" filterable placeholder="请选择父项物料（成品）" style="width: 260px">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`"
              :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="editing.version" style="width: 120px" placeholder="V1" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>BOM 子项明细</span>
        <el-button size="small" :icon="Plus" @click="handleAddEntry">添加子项</el-button>
      </div>
      <el-table :data="editing.entries" border size="small">
        <el-table-column label="子项物料" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.materialId" filterable placeholder="请选择子项物料" size="small" style="width: 100%">
              <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`"
                :value="m.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="单位用量" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0.0001" :precision="4" size="small" controls-position="right"
              style="width: 110px" />
          </template>
        </el-table-column>
        <el-table-column label="损耗率%" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.lossRate" :min="0" :precision="2" size="small" controls-position="right"
              style="width: 100px" />
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

    <el-dialog v-model="detailVisible" title="BOM 详情" width="760px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="BOM 编号">{{ detail.bomNo }}</el-descriptions-item>
        <el-descriptions-item label="父项物料">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.entries || []" border size="small" style="margin-top: 12px">
        <el-table-column prop="materialCode" label="子项编码" min-width="120" />
        <el-table-column prop="materialName" label="子项物料" min-width="140" />
        <el-table-column prop="qty" label="单位用量" width="110" />
        <el-table-column prop="lossRate" label="损耗率%" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { bomApi } from '@/api/production'
import { materialApi } from '@/api/master'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ entries: [] as any[] })
const detail = reactive<any>({})
const materials = ref<any[]>([])

const rules = {
  materialId: [{ required: true, message: '请选择父项物料', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  materialId: undefined as number | undefined,
  version: 'V1',
  remark: '',
  entries: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await bomApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await bomApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await bomApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleAddEntry = () => {
  editing.entries.push({ materialId: undefined, qty: 1, lossRate: 0, remark: '' })
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.entries.length) {
      ElMessage.warning('请添加 BOM 子项明细')
      return
    }
    for (const row of editing.entries) {
      if (!row.materialId) {
        ElMessage.warning('请选择子项物料')
        return
      }
      if (row.materialId === editing.materialId) {
        ElMessage.warning('子项物料不能与父项物料相同')
        return
      }
    }
    const ids = editing.entries.map((row: any) => row.materialId)
    if (new Set(ids).size !== ids.length) {
      ElMessage.warning('同一子项物料只能添加一行')
      return
    }
    saving.value = true
    try {
      if (editing.id) {
        await bomApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await bomApi.create({ ...editing })
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
  ElMessageBox.confirm(`确认删除 BOM「${row.bomNo}」？`, '提示', { type: 'warning' }).then(async () => {
    await bomApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
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








