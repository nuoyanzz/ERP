<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">角色管理</div>
        <div class="page-header__desc">配置角色编码、数据范围与菜单权限</div>
      </div>
    </div>

    <!-- 查询表单 -->
    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="角色编码">
          <el-input v-model="query.roleCode" placeholder="角色编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作区 + 表格 + 分页 -->
    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-button v-perms="'sys:role:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
      <el-button v-perms="'sys:role:remove'" type="danger" :icon="Delete" :disabled="!selected.length"
        @click="handleBatchDelete">批量删除</el-button>
    </div>

      <el-table v-loading="loading" :data="list" border @selection-change="(rows: any[]) => (selected = rows)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column label="数据范围" width="140">
          <template #default="{ row }">
            {{ dataScopeText(row.dataScope) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'sys:role:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'sys:role:assign'" link type="primary" @click="handleAssignMenus(row)">分配菜单</el-button>
            <el-button v-perms="'sys:role:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="editing.roleCode" :disabled="!!editing.id" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="editing.roleName" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="editing.dataScope" style="width: 100%">
            <el-option v-for="(label, value) in scopeOptions" :key="value" :label="label" :value="Number(value)" />
          </el-select>
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

    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="360px">
      <el-tree ref="menuTreeRef" class="assign-tree" :data="menuTree" :props="{ label: 'menuName', children: 'children' }" show-checkbox
        node-key="id" :default-checked-keys="checkedMenuIds" :default-expanded-keys="expandedKeys" />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type TreeInstance } from 'element-plus'
import { Search, Reset, Plus, Delete } from '@element-plus/icons-vue'
import { menuApi, roleApi, type MenuTreeVO, type RoleVO } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref<RoleVO[]>([])
const total = ref(0)
const selected = ref<RoleVO[]>([])
const query = reactive({ pageNum: 1, pageSize: 10, roleCode: '', roleName: '' })
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const menuTreeRef = ref<TreeInstance>()
const editing = reactive<any>({})
const menuTree = ref<MenuTreeVO[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRoleId = ref(0)

const scopeOptions: Record<string, string> = {
  1: '全部数据',
  2: '本部门及以下',
  3: '本部门',
  4: '仅本人',
  5: '自定义'
}

const dataScopeText = (scope: number) => scopeOptions[scope] || '未知'

const expandedKeys = computed(() => {
  const keys: number[] = []
  const walk = (nodes: MenuTreeVO[]) => {
    nodes.forEach((node) => {
      keys.push(node.id)
      if (node.children) walk(node.children)
    })
  }
  walk(menuTree.value)
  return keys
})

const emptyEditing = () => ({
  id: undefined,
  roleCode: '',
  roleName: '',
  dataScope: 1,
  status: 0,
  remark: ''
})

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await roleApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, roleCode: '', roleName: '' })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: RoleVO) => {
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
        await roleApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await roleApi.create({ ...editing })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: RoleVO) => {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' }).then(async () => {
    await roleApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleBatchDelete = () => {
  const ids = selected.value.map((item) => item.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个角色？`, '提示', { type: 'warning' }).then(async () => {
    await roleApi.batchRemove(ids)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleAssignMenus = async (row: RoleVO) => {
  currentRoleId.value = row.id
  if (!menuTree.value.length) {
    menuTree.value = await menuApi.tree()
  }
  const detail = await roleApi.detail(row.id)
  checkedMenuIds.value = detail.menuIds || []
  menuDialogVisible.value = true
}

const handleSaveMenus = async () => {
  saving.value = true
  try {
    const checked = menuTreeRef.value?.getCheckedKeys() || []
    const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
    const menuIds = [...checked, ...halfChecked].map((id) => Number(id))
    await roleApi.assignMenus(currentRoleId.value, menuIds)
    ElMessage.success('保存成功')
    menuDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

  onMounted(loadData)
</script>

<style scoped>
/* 分配菜单树：默认全部展开，限制高度超出滚动 */
.assign-tree {
  max-height: 46vh;
  overflow: auto;
  padding: 2px 4px;
}
</style>








