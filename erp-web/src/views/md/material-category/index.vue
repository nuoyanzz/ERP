<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">物料分类</div>
        <div class="page-header__desc">以树形结构维护物料分类，分类被物料引用后不可删除</div>
      </div>
    </div>

    <div class="category-layout">
      <!-- 左侧分类树 -->
      <el-card shadow="never" class="category-tree">
        <div class="tree-toolbar">
          <el-button v-perms="'md:material-category:add'" type="primary" :icon="Plus" @click="handleCreate(0)">
            新增根分类
          </el-button>
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
        <el-input v-model="treeKeyword" placeholder="搜索分类名称/编码" clearable :prefix-icon="Search" class="tree-search" />
        <el-tree
          ref="treeRef"
          v-loading="loading"
          :data="treeData"
          :props="{ label: 'categoryName', children: 'children' }"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          :filter-node-method="filterNode"
          @node-click="handleNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="tree-node__name">{{ data.categoryName }}</span>
              <span class="tree-node__code">{{ data.categoryCode }}</span>
            </div>
          </template>
        </el-tree>
      </el-card>

      <!-- 右侧分类明细 -->
      <div class="category-detail">
        <el-card shadow="never">
          <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
            <el-form-item label="分类编码">
              <el-input v-model="query.categoryCode" placeholder="分类编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="分类名称">
              <el-input v-model="query.categoryName" placeholder="分类名称" clearable @keyup.enter="handleSearch" />
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
          <el-button v-perms="'md:material-category:add'" type="primary" :icon="Plus"
            @click="handleCreate(current ? current.id : 0)">新增分类</el-button>
          <el-button v-perms="'md:material-category:edit'" :icon="Edit" :disabled="!current"
            @click="handleEdit(current)">编辑</el-button>
          <el-button v-perms="'md:material-category:remove'" :icon="Delete" :disabled="!current"
            @click="handleDelete(current)">删除</el-button>
        </div>

        <el-card shadow="never">
          <el-table v-loading="loading" :data="pagedList" border highlight-current-row
            @current-change="handleRowSelect">
            <el-table-column type="index" label="#" width="45" />
            <el-table-column prop="categoryCode" label="分类编码" min-width="130" />
            <el-table-column prop="categoryName" label="分类名称" min-width="150" />
            <el-table-column prop="parentName" label="上级分类" min-width="130" />
            <el-table-column prop="sortNo" label="排序" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button v-perms="'md:material-category:add'" link type="primary" @click="handleCreate(row.id)">新增子级</el-button>
                <el-button v-perms="'md:material-category:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-button v-perms="'md:material-category:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="filteredList.length"
              :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
              @size-change="handleSearch" @current-change="handleSearch" />
          </div>
        </el-card>

        <!-- 选中分类明细 -->
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="detail-card__title">分类明细</span>
          </template>
          <el-descriptions v-if="current" :column="3" border>
            <el-descriptions-item label="分类编码">{{ current.categoryCode }}</el-descriptions-item>
            <el-descriptions-item label="分类名称">{{ current.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="上级分类">{{ current.parentName || '根分类' }}</el-descriptions-item>
            <el-descriptions-item label="排序号">{{ current.sortNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="current.status === 0 ? 'success' : 'danger'">
                {{ current.status === 0 ? '正常' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="请在左侧选择或点击列表中的分类" :image-size="80" />
        </el-card>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑分类' : '新增分类'" width="520px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="上级分类">
          <el-tree-select v-model="editing.parentId" :data="parentTree" node-key="id"
            :props="{ label: 'categoryName', children: 'children', value: 'id' }" check-strictly clearable
            placeholder="根分类" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="editing.categoryCode" placeholder="如 FG-01" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="editing.categoryName" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="editing.sortNo" :min="0" :precision="0" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type TreeInstance } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { materialCategoryApi } from '@/api/master'

interface CategoryNode {
  id: number
  categoryCode: string
  categoryName: string
  parentId: number
  sortNo: number
  status: number
  remark?: string
  children?: CategoryNode[]
}

/** 拉平后的分类节点，附带上级分类名称 */
interface FlatCategory extends CategoryNode {
  parentName: string
}

const loading = ref(false)
const saving = ref(false)
const treeRef = ref<TreeInstance>()
const treeData = ref<CategoryNode[]>([])
const treeKeyword = ref('')
const current = ref<FlatCategory | null>(null)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryCode: '',
  categoryName: '',
  status: undefined as number | undefined
})

const rules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

/** 拉平树结构，并补上上级分类名称，供列表与下拉使用 */
const flatten = (nodes: CategoryNode[], parentName = ''): FlatCategory[] => {
  const result: FlatCategory[] = []
  for (const node of nodes) {
    result.push({ ...node, parentName })
    if (node.children?.length) {
      result.push(...flatten(node.children, node.categoryName))
    }
  }
  return result
}

const flatList = computed(() => flatten(treeData.value))

/** 右侧列表按查询条件过滤 */
const filteredList = computed(() =>
  flatList.value.filter((item) => {
    if (query.categoryCode && !item.categoryCode.includes(query.categoryCode)) {
      return false
    }
    if (query.categoryName && !item.categoryName.includes(query.categoryName)) {
      return false
    }
    if (query.status !== undefined && item.status !== query.status) {
      return false
    }
    return true
  })
)

const pagedList = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredList.value.slice(start, start + query.pageSize)
})

/** 上级分类下拉数据：编辑时排除自身，避免把自己挂到自己下面 */
const parentTree = computed(() => {
  const filter = (nodes: CategoryNode[]): CategoryNode[] =>
    nodes
      .filter((node) => node.id !== editing.id)
      .map((node) => ({ ...node, children: node.children ? filter(node.children) : [] }))
  return filter(treeData.value)
})

const filterNode = (value: string, data: CategoryNode) => {
  if (!value) {
    return true
  }
  return data.categoryName.includes(value) || data.categoryCode.includes(value)
}

watch(treeKeyword, (value) => {
  treeRef.value?.filter(value)
})

const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await materialCategoryApi.tree()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
}

const handleReset = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, categoryCode: '', categoryName: '', status: undefined })
}

const handleNodeClick = (data: CategoryNode) => {
  current.value = flatList.value.find((item) => item.id === data.id) ?? null
}

const handleRowSelect = (row: any) => {
  current.value = row ? flatList.value.find((item) => item.id === row.id) ?? null : null
}

const handleCreate = (parentId: number) => {
  Object.assign(editing, {
    id: undefined,
    categoryCode: '',
    categoryName: '',
    parentId: parentId || 0,
    sortNo: 0,
    status: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  if (!row) {
    return
  }
  const detail = await materialCategoryApi.detail(row.id)
  Object.assign(editing, {
    id: detail.id,
    categoryCode: detail.categoryCode,
    categoryName: detail.categoryName,
    parentId: detail.parentId || 0,
    sortNo: detail.sortNo,
    status: detail.status,
    remark: detail.remark || ''
  })
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
        categoryCode: editing.categoryCode,
        categoryName: editing.categoryName,
        parentId: editing.parentId || 0,
        sortNo: editing.sortNo,
        status: editing.status,
        remark: editing.remark
      }
      if (editing.id) {
        await materialCategoryApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await materialCategoryApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      await loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: any) => {
  if (!row) {
    return
  }
  ElMessageBox.confirm(`确认删除分类「${row.categoryName}」？`, '提示', { type: 'warning' }).then(async () => {
    await materialCategoryApi.remove(row.id)
    ElMessage.success('删除成功')
    if (current.value?.id === row.id) {
      current.value = null
    }
    await loadData()
  })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.category-layout {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.category-tree {
  width: 300px;
  flex-shrink: 0;
}

.category-detail {
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

.detail-card {
  margin-top: 12px;
}

.detail-card__title {
  font-weight: 600;
}
</style>
