<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">数据字典</div>
        <div class="page-header__desc">维护字典类型与字典项，业务页面通过字典编码读取可配置选项</div>
      </div>
    </div>

    <div class="dict-layout">
      <!-- 左侧字典类型 -->
      <el-card shadow="never" class="dict-type-card">
        <div class="tree-toolbar">
          <el-button v-perms="'sys:dict:add'" type="primary" :icon="Plus" @click="handleTypeCreate">新增类型</el-button>
          <el-button :icon="Refresh" @click="handleRefreshCache">刷新缓存</el-button>
        </div>
        <el-input v-model="typeKeyword" placeholder="搜索字典名称/编码" clearable :prefix-icon="Search" class="tree-search" />
        <el-tree
          v-loading="typeLoading"
          :data="typeTree"
          :props="{ label: 'label', children: 'children' }"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handleTypeSelect"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="tree-node__name">{{ data.dictName }}</span>
              <span class="tree-node__code">{{ data.dictType }}</span>
            </div>
          </template>
        </el-tree>
      </el-card>

      <!-- 右侧字典项 -->
      <div class="dict-data-card">
        <el-card shadow="never">
          <div class="toolbar-actions">
            <el-button v-perms="'sys:dict:add'" type="primary" :icon="Plus" :disabled="!currentType"
              @click="handleDataCreate">新增字典项</el-button>
            <el-button v-perms="'sys:dict:edit'" :icon="Edit" :disabled="!currentType" @click="handleTypeEdit">
              编辑类型</el-button>
            <el-button v-perms="'sys:dict:remove'" :icon="Delete" :disabled="!currentType" @click="handleTypeDelete">
              删除类型</el-button>
          </div>

          <div class="current-type" v-if="currentType">
            当前字典类型：<strong>{{ currentType.dictName }}</strong>
            <span class="code-text">{{ currentType.dictType }}</span>
          </div>
          <div class="current-type" v-else>请在左侧选择字典类型</div>

          <el-table v-loading="dataLoading" :data="dataList" border>
            <el-table-column type="index" label="#" width="45" />
            <el-table-column prop="dictLabel" label="字典标签" min-width="140" />
            <el-table-column prop="dictValue" label="字典键值" min-width="140" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="样式" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.listClass" :type="tagType(row.listClass)" size="small" effect="light">
                  {{ row.listClass }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="默认" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-perms="'sys:dict:edit'" link type="primary" @click="handleDataEdit(row)">编辑</el-button>
                <el-button v-perms="'sys:dict:remove'" link type="danger" @click="handleDataDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>

    <!-- 字典类型表单 -->
    <el-dialog v-model="typeDialogVisible" :title="typeEditing.id ? '编辑字典类型' : '新增字典类型'" width="480px">
      <el-form ref="typeFormRef" :model="typeEditing" :rules="typeRules" label-width="90px">
        <el-form-item label="字典编码" prop="dictType">
          <el-input v-model="typeEditing.dictType" placeholder="如 pa_order_status" :disabled="!!typeEditing.id" />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeEditing.dictName" placeholder="如 采购订单状态" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeEditing.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeEditing.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleTypeSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典项表单 -->
    <el-dialog v-model="dataDialogVisible" :title="dataEditing.id ? '编辑字典项' : '新增字典项'" width="480px">
      <el-form ref="dataFormRef" :model="dataEditing" :rules="dataRules" label-width="90px">
        <el-form-item label="所属类型">
          <el-input v-model="dataEditing.dictType" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataEditing.dictLabel" placeholder="如 已审核" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataEditing.dictValue" placeholder="如 1" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataEditing.sortNo" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="标签样式">
          <el-select v-model="dataEditing.listClass" clearable placeholder="无" style="width: 100%">
            <el-option v-for="item in listClassOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认选中">
          <el-radio-group v-model="dataEditing.isDefault">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataEditing.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataEditing.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleDataSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { dictApi } from '@/api/system'
import { dictApi as localDictApi } from '@/api/dict'
import request from '@/utils/request'

interface DictTypeNode {
  id: number
  dictType: string
  dictName: string
  status: number
  remark?: string
  label: string
  children?: DictTypeNode[]
}

const listClassOptions = [
  { label: '成功 success', value: 'success' },
  { label: '警告 warning', value: 'warning' },
  { label: '危险 danger', value: 'danger' },
  { label: '主要 primary', value: 'primary' },
  { label: '信息 info', value: 'info' }
]

const tagType = (listClass: string) => {
  const hit = listClassOptions.find((item) => item.value === listClass)
  return (hit?.value as any) || 'info'
}

const typeLoading = ref(false)
const dataLoading = ref(false)
const saving = ref(false)
const typeList = ref<any[]>([])
const typeKeyword = ref('')
const currentType = ref<any>(null)
const dataList = ref<any[]>([])
const typeDialogVisible = ref(false)
const dataDialogVisible = ref(false)
const typeFormRef = ref<FormInstance>()
const dataFormRef = ref<FormInstance>()
const typeEditing = reactive<any>({})
const dataEditing = reactive<any>({})

const typeRules = {
  dictType: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }]
}

const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }]
}

/** 左侧树：字典类型平铺一层，label 统一成「名称（编码）」 */
const typeTree = computed<DictTypeNode[]>(() =>
  typeList.value
    .filter((item) => {
      if (!typeKeyword.value) {
        return true
      }
      return (
        String(item.dictName || '').includes(typeKeyword.value) ||
        String(item.dictType || '').includes(typeKeyword.value)
      )
    })
    .map((item) => ({
      id: item.id,
      dictType: item.dictType,
      dictName: item.dictName,
      status: item.status,
      remark: item.remark,
      label: item.dictName
    }))
)

watch(typeKeyword, () => {
  // 左侧列表已通过 typeTree 计算属性过滤，无需额外处理
})

const loadTypes = async () => {
  typeLoading.value = true
  try {
    typeList.value = await dictApi.types()
  } finally {
    typeLoading.value = false
  }
}

const loadData = async () => {
  if (!currentType.value) {
    dataList.value = []
    return
  }
  dataLoading.value = true
  try {
    // 维护页需要看到停用的字典项，这里读取全量接口
    dataList.value = await request.get<any, any[]>(`/system/dict/data/all/${currentType.value.dictType}`)
  } finally {
    dataLoading.value = false
  }
}

const handleTypeSelect = (data: DictTypeNode) => {
  currentType.value = typeList.value.find((item) => item.id === data.id) ?? null
  loadData()
}

const handleTypeCreate = () => {
  Object.assign(typeEditing, { id: undefined, dictType: '', dictName: '', status: 0, remark: '' })
  typeDialogVisible.value = true
}

const handleTypeEdit = () => {
  if (!currentType.value) {
    return
  }
  Object.assign(typeEditing, {
    id: currentType.value.id,
    dictType: currentType.value.dictType,
    dictName: currentType.value.dictName,
    status: currentType.value.status ?? 0,
    remark: currentType.value.remark || ''
  })
  typeDialogVisible.value = true
}

const handleTypeSave = async () => {
  if (!typeFormRef.value) return
  await typeFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        id: typeEditing.id,
        dictType: typeEditing.dictType,
        dictName: typeEditing.dictName,
        status: typeEditing.status,
        remark: typeEditing.remark
      }
      if (typeEditing.id) {
        await request.put<any, void>('/system/dict/type', payload)
        ElMessage.success('修改成功')
      } else {
        await request.post<any, number>('/system/dict/type', payload)
        ElMessage.success('新增成功')
      }
      typeDialogVisible.value = false
      await loadTypes()
      localDictApi.clearCache()
    } finally {
      saving.value = false
    }
  })
}

const handleTypeDelete = () => {
  if (!currentType.value) {
    return
  }
  ElMessageBox.confirm(`确认删除字典类型「${currentType.value.dictName}」？`, '提示', { type: 'warning' }).then(async () => {
    await request.delete<any, void>(`/system/dict/type/${currentType.value.id}`)
    ElMessage.success('删除成功')
    currentType.value = null
    dataList.value = []
    await loadTypes()
    localDictApi.clearCache()
  })
}

const handleDataCreate = () => {
  if (!currentType.value) {
    return
  }
  Object.assign(dataEditing, {
    id: undefined,
    dictType: currentType.value.dictType,
    dictLabel: '',
    dictValue: '',
    sortNo: 0,
    cssClass: '',
    listClass: '',
    isDefault: 0,
    status: 0,
    remark: ''
  })
  dataDialogVisible.value = true
}

const handleDataEdit = (row: any) => {
  Object.assign(dataEditing, {
    id: row.id,
    dictType: row.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    sortNo: row.sortNo ?? 0,
    cssClass: row.cssClass || '',
    listClass: row.listClass || '',
    isDefault: row.isDefault ?? 0,
    status: row.status ?? 0,
    remark: row.remark || ''
  })
  dataDialogVisible.value = true
}

const handleDataSave = async () => {
  if (!dataFormRef.value) return
  await dataFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        id: dataEditing.id,
        dictType: dataEditing.dictType,
        dictLabel: dataEditing.dictLabel,
        dictValue: dataEditing.dictValue,
        sortNo: dataEditing.sortNo,
        cssClass: dataEditing.cssClass,
        listClass: dataEditing.listClass,
        isDefault: dataEditing.isDefault,
        status: dataEditing.status,
        remark: dataEditing.remark
      }
      if (dataEditing.id) {
        await request.put<any, void>('/system/dict/data', payload)
        ElMessage.success('修改成功')
      } else {
        await request.post<any, number>('/system/dict/data', payload)
        ElMessage.success('新增成功')
      }
      dataDialogVisible.value = false
      localDictApi.clearCache()
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDataDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除字典项「${row.dictLabel}」？`, '提示', { type: 'warning' }).then(async () => {
    await request.delete<any, void>(`/system/dict/data/${row.id}`)
    ElMessage.success('删除成功')
    localDictApi.clearCache()
    loadData()
  })
}

const handleRefreshCache = async () => {
  await dictApi.refresh()
  localDictApi.clearCache()
  ElMessage.success('字典缓存已刷新')
}

onMounted(loadTypes)
</script>

<style scoped lang="scss">
.dict-layout {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.dict-type-card {
  width: 300px;
  flex-shrink: 0;
}

.dict-data-card {
  flex: 1;
  min-width: 0;
}

.tree-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.tree-search {
  margin-bottom: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.tree-node__code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.current-type {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.code-text {
  margin-left: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
