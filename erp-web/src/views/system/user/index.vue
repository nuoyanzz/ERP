<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">用户管理</div>
        <div class="page-header__desc">维护系统账号、启停状态与角色分配</div>
      </div>
    </div>

    <!-- 查询表单 -->
    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="query.nickname" placeholder="昵称" clearable @keyup.enter="handleSearch" />
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

    <!-- 操作区 + 表格 + 分页 -->
    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-button v-perms="'sys:user:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
      <el-button v-perms="'sys:user:remove'" type="danger" :icon="Delete" :disabled="!selected.length"
        @click="handleBatchDelete">批量删除</el-button>
    </div>

      <el-table v-loading="loading" :data="list" border @selection-change="(rows: any[]) => (selected = rows)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'sys:user:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'sys:user:assign'" link type="primary" @click="handleAssignRoles(row)">分配角色</el-button>
            <el-button v-perms="'sys:user:resetPwd'" link type="warning"
              @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-perms="'sys:user:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editing.username" :disabled="!!editing.id" placeholder="字母/数字/下划线" />
        </el-form-item>
        <el-form-item v-if="!editing.id" label="密码" prop="password">
          <el-input v-model="editing.password" placeholder="留空则使用默认密码 123456" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editing.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editing.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editing.email" />
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

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <el-checkbox-group v-model="assignedRoleIds">
        <el-checkbox v-for="role in allRoles" :key="role.id" :value="role.id" :label="role.roleName" border
          style="margin-bottom: 8px; width: 100%" />
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Reset, Plus, Delete } from '@element-plus/icons-vue'
import { userApi, type RoleVO, type UserVO } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref<UserVO[]>([])
const total = ref(0)
const selected = ref<UserVO[]>([])
const query = reactive({ pageNum: 1, pageSize: 10, username: '', nickname: '', status: undefined as number | undefined })
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})
const allRoles = ref<RoleVO[]>([])
const assignedRoleIds = ref<number[]>([])
const currentUserId = ref(0)

const emptyEditing = () => ({
  id: undefined,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  status: 0,
  remark: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度 3-50 个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await userApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, username: '', nickname: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: UserVO) => {
  Object.assign(editing, emptyEditing(), row)
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
        delete payload.password
        await userApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await userApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: UserVO) => {
  ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' }).then(async () => {
    await userApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleBatchDelete = () => {
  const ids = selected.value.map((item) => item.id)
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个用户？`, '提示', { type: 'warning' }).then(async () => {
    await userApi.batchRemove(ids)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleResetPassword = (row: UserVO) => {
  ElMessageBox.confirm(`确认将用户「${row.username}」的密码重置为 123456？`, '提示', { type: 'warning' }).then(
    async () => {
      await userApi.resetPassword(row.id)
      ElMessage.success('密码已重置')
    }
  )
}

const handleAssignRoles = async (row: UserVO) => {
  currentUserId.value = row.id
  if (!allRoles.value.length) {
    allRoles.value = await userApi.roles()
  }
  const detail = await userApi.detail(row.id)
  assignedRoleIds.value = detail.roleIds || []
  roleDialogVisible.value = true
}

const handleSaveRoles = async () => {
  saving.value = true
  try {
    await userApi.assignRoles(currentUserId.value, assignedRoleIds.value)
    ElMessage.success('保存成功')
    roleDialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

  onMounted(loadData)
</script>

<style scoped>
/* 表格最小高度：空数据/少量数据时保持页面稳定 */
.el-table {
  min-height: 220px;
}
</style>








