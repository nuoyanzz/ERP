<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">流程定义</div>
        <div class="page-header__desc">设计审批流程节点与连线</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="流程编码">
          <el-input v-model="query.definitionKey" placeholder="流程编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="流程名称">
          <el-input v-model="query.definitionName" placeholder="流程名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="停用" :value="0" />
            <el-option label="启用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
    <el-button v-perms="'wf:definition:add'" type="primary" :icon="Plus" @click="handleCreate">新增流程</el-button>
  </div>

  <el-card shadow="never">
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="definitionKey" label="流程编码" min-width="140" />
      <el-table-column prop="definitionName" label="流程名称" min-width="160" />
      <el-table-column prop="bizType" label="业务类型" min-width="120" />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" v-perms="'wf:definition:publish'" link type="success"
            @click="handlePublish(row)">发布</el-button>
          <el-button v-if="row.status === 1" v-perms="'wf:definition:publish'" link type="warning"
            @click="handleDisable(row)">停用</el-button>
          <el-button v-perms="'wf:definition:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="handleView(row)">节点</el-button>
          <el-button v-perms="'wf:definition:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
        @size-change="loadData" @current-change="loadData" />
    </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑流程' : '新增流程'" width="920px"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="editing" :rules="rules" inline label-width="80px">
        <el-form-item label="流程编码" prop="definitionKey">
          <el-input v-model="editing.definitionKey" :disabled="!!editing.id" placeholder="如 PA_ORDER_AUDIT"
            style="width: 200px" />
        </el-form-item>
        <el-form-item label="流程名称" prop="definitionName">
          <el-input v-model="editing.definitionName" style="width: 200px" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="editing.bizType" placeholder="如 PA_ORDER" style="width: 180px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editing.description" style="width: 240px" />
        </el-form-item>
      </el-form>

      <div class="entry-toolbar">
        <span>流程节点（按排序依次审批）</span>
        <el-button size="small" :icon="Plus" @click="handleAddNode">添加节点</el-button>
      </div>
      <el-table :data="editing.nodes" border size="small">
        <el-table-column label="节点编码" width="140">
          <template #default="{ row }">
            <el-input v-model="row.nodeKey" size="small" placeholder="NODE1" />
          </template>
        </el-table-column>
        <el-table-column label="节点名称" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.nodeName" size="small" placeholder="部门主管审批" />
          </template>
        </el-table-column>
        <el-table-column label="审批人类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.approverType" size="small" style="width: 100%" @change="row.approverValue = ''">
              <el-option label="角色" :value="1" />
              <el-option label="指定用户" :value="2" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="审批人" min-width="180">
          <template #default="{ row }">
            <el-select v-if="row.approverType === 1" v-model="row.approverValue" filterable size="small"
              placeholder="请选择角色" style="width: 100%">
              <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="String(r.id)" />
            </el-select>
            <el-select v-else v-model="row.approverValue" filterable size="small" placeholder="请选择用户"
              style="width: 100%">
              <el-option v-for="u in users" :key="u.id" :label="`${u.username} ${u.nickname || ''}`"
                :value="String(u.id)" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="多人方式" width="120">
          <template #default="{ row }">
            <el-select v-model="row.multiApprove" size="small" style="width: 100%">
              <el-option label="任一通过" :value="1" />
              <el-option label="全部通过" :value="2" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.sortNo" :min="0" size="small" controls-position="right"
              style="width: 70px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editing.nodes.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="流程节点" width="760px">
      <el-steps :active="detailNodes.length" align-center finish-status="success">
        <el-step v-for="node in detailNodes" :key="node.id" :title="node.nodeName"
          :description="approverText(node)" />
      </el-steps>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { definitionApi } from '@/api/workflow'
import { roleApi, userApi } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  definitionKey: '',
  definitionName: '',
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({ nodes: [] as any[] })
const roles = ref<any[]>([])
const users = ref<any[]>([])
const detailNodes = ref<any[]>([])

const approverText = computed(() => (node: any) => {
  const approver =
    node.approverType === 1
      ? roles.value.find((r: any) => String(r.id) === String(node.approverValue))?.roleName
      : users.value.find((u: any) => String(u.id) === String(node.approverValue))?.username
  const multi = node.multiApprove === 2 ? '全部通过' : '任一通过'
  return `${node.approverType === 1 ? '角色' : '用户'}：${approver || node.approverValue || '-'}（${multi}）`
})

const rules = {
  definitionKey: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  definitionName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }]
}

const emptyEditing = () => ({
  id: undefined,
  definitionKey: '',
  definitionName: '',
  bizType: '',
  description: '',
  nodes: [] as any[]
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await definitionApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, definitionKey: '', definitionName: '', status: undefined })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const data = await definitionApi.detail(row.id)
  Object.assign(editing, emptyEditing(), data)
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  const data = await definitionApi.detail(row.id)
  detailNodes.value = data.nodes || []
  detailVisible.value = true
}

const handleAddNode = () => {
  const nextSort = editing.nodes.length + 1
  editing.nodes.push({
    nodeKey: `N${nextSort}`,
    nodeName: '',
    nodeType: 2,
    approverType: 1,
    approverValue: '',
    multiApprove: 1,
    sortNo: nextSort,
    remark: ''
  })
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!editing.nodes.length) {
      ElMessage.warning('请添加流程节点')
      return
    }
    for (const node of editing.nodes) {
      if (!node.nodeKey || !node.nodeName) {
        ElMessage.warning('请完善节点编码与名称')
        return
      }
      if (!node.approverValue) {
        ElMessage.warning(`请配置节点「${node.nodeName}」的审批人`)
        return
      }
    }
    saving.value = true
    try {
      if (editing.id) {
        await definitionApi.update({ ...editing })
        ElMessage.success('修改成功')
      } else {
        await definitionApi.create({ ...editing })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handlePublish = (row: any) => {
  ElMessageBox.confirm(`确认发布流程「${row.definitionName}」？`, '发布流程', { type: 'warning' }).then(async () => {
    await definitionApi.publish(row.id)
    ElMessage.success('已发布')
    loadData()
  })
}

const handleDisable = (row: any) => {
  ElMessageBox.confirm(`确认停用流程「${row.definitionName}」？`, '停用流程', { type: 'warning' }).then(async () => {
    await definitionApi.disable(row.id)
    ElMessage.success('已停用')
    loadData()
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除流程「${row.definitionName}」？`, '提示', { type: 'warning' }).then(async () => {
    await definitionApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  roles.value = await roleApi.list()
  users.value = (await userApi.page({ pageNum: 1, pageSize: 200 })).records
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








