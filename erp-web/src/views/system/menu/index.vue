<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">菜单管理</div>
        <div class="page-header__desc">维护目录、菜单与按钮级权限标识</div>
      </div>
    </div>

    <!-- 操作区 + 树形表格 -->
    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-button v-perms="'sys:menu:add'" type="primary" :icon="Plus" @click="handleCreate(0)">新增根菜单</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

      <el-table v-loading="loading" class="tree-table" :data="treeData" row-key="id" border default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 1 ? 'info' : row.menuType === 2 ? 'success' : 'warning'">
              {{ row.menuType === 1 ? '目录' : row.menuType === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" min-width="160" />
        <el-table-column prop="path" label="路由地址" min-width="140" />
        <el-table-column prop="component" label="组件路径" min-width="180" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="sortNo" label="排序" width="70" />
        <el-table-column label="可见" width="70">
          <template #default="{ row }">{{ row.visible === 0 ? '显示' : '隐藏' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 3" v-perms="'sys:menu:add'" link type="primary"
              @click="handleCreate(row.id)">新增子级</el-button>
            <el-button v-perms="'sys:menu:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'sys:menu:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑菜单' : '新增菜单'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <div class="form-grid">
          <el-form-item label="上级菜单" class="form-grid__full">
            <el-tree-select v-model="editing.parentId" :data="treeData" :props="{ label: 'menuName', children: 'children' }"
              check-strictly :value="editing.parentId" placeholder="根菜单" clearable style="width: 100%" />
          </el-form-item>
          <el-form-item label="菜单类型" prop="menuType">
            <el-radio-group v-model="editing.menuType">
              <el-radio :value="1">目录</el-radio>
              <el-radio :value="2">菜单</el-radio>
              <el-radio :value="3">按钮</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="菜单名称" prop="menuName">
            <el-input v-model="editing.menuName" />
          </el-form-item>
          <el-form-item label="权限标识">
            <el-input v-model="editing.perms" placeholder="如 sys:user:add" />
          </el-form-item>
          <el-form-item v-if="editing.menuType !== 3" label="路由地址">
            <el-input v-model="editing.path" placeholder="如 /sys/user" />
          </el-form-item>
          <el-form-item v-if="editing.menuType === 2" label="组件路径">
            <el-input v-model="editing.component" placeholder="如 sys/user/index" />
          </el-form-item>
          <el-form-item v-if="editing.menuType !== 3" label="图标">
            <el-input v-model="editing.icon" placeholder="Element Plus 图标名" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="editing.sortNo" :min="0" />
          </el-form-item>
          <el-form-item v-if="editing.menuType !== 3" label="显示状态">
            <el-radio-group v-model="editing.visible">
              <el-radio :value="0">显示</el-radio>
              <el-radio :value="1">隐藏</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { menuApi, type MenuTreeVO } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const treeData = ref<MenuTreeVO[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await menuApi.tree()
  } finally {
    loading.value = false
  }
}

const handleCreate = (parentId: number) => {
  Object.assign(editing, {
    id: undefined,
    parentId: parentId || 0,
    menuName: '',
    menuType: parentId ? 2 : 1,
    perms: '',
    path: '',
    component: '',
    icon: '',
    sortNo: 0,
    visible: 0
  })
  dialogVisible.value = true
}

const handleEdit = (row: MenuTreeVO) => {
  Object.assign(editing, { ...row, children: undefined })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { ...editing }
      if (payload.id) {
        await menuApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await menuApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: MenuTreeVO) => {
  ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' }).then(async () => {
    await menuApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

  onMounted(loadData)
</script>

<style scoped>
/* 对话框两列表单：上级菜单通栏 */
.form-grid__full {
  grid-column: 1 / -1;
}

/* 树形表格最小高度 */
.tree-table {
  min-height: 320px;
}
</style>








