<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">会计科目</div>
        <div class="page-header__desc">维护会计科目体系、余额方向与辅助核算</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-button v-perms="'fi:account:add'" type="primary" :icon="Plus" @click="handleCreate(0)">新增根科目</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

      <el-table v-loading="loading" :data="treeData" row-key="id" border default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="subjectCode" label="科目编码" min-width="140" />
        <el-table-column prop="subjectName" label="科目名称" min-width="160" />
        <el-table-column label="类别" width="90">
          <template #default="{ row }">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column label="余额方向" width="90">
          <template #default="{ row }">{{ row.balanceDir === 1 ? '借方' : '贷方' }}</template>
        </el-table-column>
        <el-table-column label="辅助核算" min-width="180">
          <template #default="{ row }">
            <el-tag v-if="row.assistCustomer === 1" size="small" style="margin-right: 4px">客户</el-tag>
            <el-tag v-if="row.assistSupplier === 1" size="small" style="margin-right: 4px">供应商</el-tag>
            <el-tag v-if="row.assistDept === 1" size="small" style="margin-right: 4px">部门</el-tag>
            <el-tag v-if="row.assistProject === 1" size="small">项目</el-tag>
            <span v-if="!row.assistCustomer && !row.assistSupplier && !row.assistDept && !row.assistProject">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'fi:account:add'" link type="primary" @click="handleCreate(row.id)">新增子级</el-button>
            <el-button v-perms="'fi:account:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'fi:account:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑科目' : '新增科目'" width="560px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="上级科目">
          <el-tree-select v-model="editing.parentId" :data="treeData"
            :props="{ label: 'subjectName', children: 'children', value: 'id' }" check-strictly clearable
            placeholder="根科目" style="width: 100%" />
        </el-form-item>
        <el-form-item label="科目编码" prop="subjectCode">
          <el-input v-model="editing.subjectCode" placeholder="如 1002.01" />
        </el-form-item>
        <el-form-item label="科目名称" prop="subjectName">
          <el-input v-model="editing.subjectName" />
        </el-form-item>
        <el-form-item label="科目类别" prop="category">
          <el-select v-model="editing.category" style="width: 100%">
            <el-option label="资产" :value="1" />
            <el-option label="负债" :value="2" />
            <el-option label="权益" :value="3" />
            <el-option label="成本" :value="4" />
            <el-option label="损益" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额方向">
          <el-radio-group v-model="editing.balanceDir">
            <el-radio :value="1">借方</el-radio>
            <el-radio :value="2">贷方</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="辅助核算">
          <el-checkbox v-model="editing.assistCustomer" :true-value="1" :false-value="0">客户</el-checkbox>
          <el-checkbox v-model="editing.assistSupplier" :true-value="1" :false-value="0">供应商</el-checkbox>
          <el-checkbox v-model="editing.assistDept" :true-value="1" :false-value="0">部门</el-checkbox>
          <el-checkbox v-model="editing.assistProject" :true-value="1" :false-value="0">项目</el-checkbox>
        </el-form-item>
        <el-form-item label="核算币别">
          <el-input v-model="editing.currency" style="width: 140px" />
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { accountApi, type AccountTreeVO } from '@/api/finance'

const loading = ref(false)
const saving = ref(false)
const treeData = ref<AccountTreeVO[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const categoryText = (category: number) =>
  ({ 1: '资产', 2: '负债', 3: '权益', 4: '成本', 5: '损益' } as Record<number, string>)[category] || '-'

const rules = {
  subjectCode: [{ required: true, message: '请输入科目编码', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择科目类别', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await accountApi.tree()
  } finally {
    loading.value = false
  }
}

const handleCreate = (parentId: number) => {
  Object.assign(editing, {
    id: undefined,
    parentId: parentId || 0,
    subjectCode: '',
    subjectName: '',
    category: 1,
    balanceDir: 1,
    assistCustomer: 0,
    assistSupplier: 0,
    assistDept: 0,
    assistProject: 0,
    currency: 'CNY',
    status: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: AccountTreeVO) => {
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
        await accountApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await accountApi.create({ ...editing })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: AccountTreeVO) => {
  ElMessageBox.confirm(`确认删除科目「${row.subjectCode} ${row.subjectName}」？`, '提示', { type: 'warning' }).then(
    async () => {
      await accountApi.remove(row.id)
      ElMessage.success('删除成功')
      loadData()
    }
  )
}

onMounted(loadData)
</script>








