<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">组织架构</div>
        <div class="page-header__desc">维护公司、部门等组织节点</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="组织名称">
          <el-input v-model="query.orgName" placeholder="组织名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'hr:org:add'" type="primary" :icon="Plus" @click="handleCreate">新增组织</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="orgCode" label="组织编码" min-width="140" />
        <el-table-column prop="orgName" label="组织名称" min-width="160" />
        <el-table-column label="组织类型" width="110">
          <template #default="{ row }">
            {{ row.orgType === 1 ? '公司' : row.orgType === 2 ? '部门' : '其他' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'hr:org:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'hr:org:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑组织' : '新增组织'" width="480px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="90px">
        <el-form-item label="组织编码" prop="orgCode">
          <el-input v-model="editing.orgCode" placeholder="如 ORG-001" />
        </el-form-item>
        <el-form-item label="组织名称" prop="orgName">
          <el-input v-model="editing.orgName" />
        </el-form-item>
        <el-form-item label="组织类型">
          <el-radio-group v-model="editing.orgType">
            <el-radio :value="1">公司</el-radio>
            <el-radio :value="2">部门</el-radio>
            <el-radio :value="3">其他</el-radio>
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
import { orgApi } from '@/api/hr'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, orgName: '' })
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const rules = {
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }]
}

const emptyEditing = () => ({
  id: undefined,
  orgCode: '',
  orgName: '',
  orgType: 1,
  parentId: 0,
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await orgApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, orgName: '' })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
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
        await orgApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await orgApi.create({ ...editing })
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
  ElMessageBox.confirm(`确认删除组织「${row.orgName}」？`, '提示', { type: 'warning' }).then(async () => {
    await orgApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>








