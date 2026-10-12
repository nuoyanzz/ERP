<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">部门管理</div>
        <div class="page-header__desc">维护部门档案、层级结构与负责人</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-button v-perms="'hr:dept:add'" type="primary" :icon="Plus" @click="handleCreate(0)">新增根部门</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

      <el-table v-loading="loading" :data="treeData" row-key="id" border default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="deptCode" label="部门编码" min-width="140" />
        <el-table-column prop="deptName" label="部门名称" min-width="160" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'hr:dept:add'" link type="primary" @click="handleCreate(row.id)">新增子级</el-button>
            <el-button v-perms="'hr:dept:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'hr:dept:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑部门' : '新增部门'" width="480px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="editing.parentId" :data="treeData"
            :props="{ label: 'deptName', children: 'children', value: 'id' }" check-strictly clearable
            placeholder="根部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门编码" prop="deptCode">
          <el-input v-model="editing.deptCode" />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="editing.deptName" />
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { deptApi } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const treeData = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const rules = {
  deptCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await deptApi.tree()
  } finally {
    loading.value = false
  }
}

const handleCreate = (parentId: number) => {
  Object.assign(editing, {
    id: undefined,
    parentId: parentId || 0,
    deptCode: '',
    deptName: '',
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  Object.assign(editing, { ...row, children: undefined })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editing.id) {
        await deptApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await deptApi.create({ ...editing })
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
  ElMessageBox.confirm(`确认删除部门「${row.deptName}」？`, '提示', { type: 'warning' }).then(async () => {
    await deptApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>








