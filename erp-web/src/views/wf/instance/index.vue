<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">流程实例</div>
        <div class="page-header__desc">跟踪流程实例的运行状态</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="流程标题">
          <el-input v-model="query.title" placeholder="流程标题" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="query.bizType" placeholder="如 PA_ORDER" clearable style="width: 160px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="运行中" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
            <el-option label="已撤销" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button type="primary" :icon="Plus" @click="startVisible = true">发起流程</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="definitionName" label="流程名称" min-width="150" />
        <el-table-column prop="title" label="流程标题" min-width="180" />
        <el-table-column prop="starterName" label="发起人" min-width="100" />
        <el-table-column label="关联业务" min-width="140">
          <template #default="{ row }">
            <span v-if="row.bizType">{{ row.bizType }} / {{ row.bizId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentNodeName" label="当前节点" min-width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" min-width="160" />
        <el-table-column prop="finishTime" label="完结时间" min-width="160" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">审批记录</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="handleCancel(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="startVisible" title="发起流程" width="520px">
      <el-form ref="formRef" :model="startForm" :rules="rules" label-width="90px">
        <el-form-item label="流程" prop="definitionKey">
          <el-select v-model="startForm.definitionKey" filterable placeholder="请选择已发布的流程" style="width: 100%">
            <el-option v-for="d in publishedDefinitions" :key="d.id" :label="`${d.definitionName}（${d.definitionKey}）`"
              :value="d.definitionKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="流程标题" prop="title">
          <el-input v-model="startForm.title" placeholder="如：采购订单 PO-202610-0001 审批" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="startForm.bizType" placeholder="如 PA_ORDER" />
        </el-form-item>
        <el-form-item label="业务单据ID">
          <el-input v-model="startForm.bizId" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" :loading="starting" @click="handleStart">发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="审批记录" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="流程标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail.starterName }}</el-descriptions-item>
        <el-descriptions-item label="当前节点">{{ detail.currentNodeName || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-timeline style="margin-top: 16px">
        <el-timeline-item v-for="task in detail.tasks || []" :key="task.id" :type="taskType(task.status)"
          :timestamp="task.operateTime || task.createTime" placement="top">
          <div class="task-title">
            {{ task.nodeName }} — {{ task.assigneeName }}
          </div>
          <div class="task-desc">
            {{ taskStatusText(task.status) }}<span v-if="task.comment">：{{ task.comment }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { definitionApi, instanceApi } from '@/api/workflow'

const loading = ref(false)
const starting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const publishedDefinitions = ref<any[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  bizType: '',
  status: undefined as number | undefined
})
const startVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref<FormInstance>()
const detail = reactive<any>({})
const startForm = reactive({ definitionKey: '', title: '', bizType: '', bizId: '' })

const statusText = (status: number) =>
  ({ 0: '运行中', 1: '已通过', 2: '已驳回', 3: '已撤销' } as Record<number, string>)[status] || '未知'
const statusType = (status: number) =>
  ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' } as Record<number, string>)[status] || 'info'
const taskStatusText = (status: number) =>
  ({ 0: '待处理', 1: '已同意', 2: '已驳回', 3: '已取消' } as Record<number, string>)[status] || '未知'
const taskType = (status: number) =>
  ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' } as Record<number, string>)[status] || 'info'

const rules = {
  definitionKey: [{ required: true, message: '请选择流程', trigger: 'change' }],
  title: [{ required: true, message: '请输入流程标题', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await instanceApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, title: '', bizType: '', status: undefined })
  loadData()
}

const handleStart = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    starting.value = true
    try {
      await instanceApi.start({
        definitionKey: startForm.definitionKey,
        title: startForm.title,
        bizType: startForm.bizType || undefined,
        bizId: startForm.bizId ? Number(startForm.bizId) : undefined
      })
      ElMessage.success('流程已发起')
      startVisible.value = false
      Object.assign(startForm, { definitionKey: '', title: '', bizType: '', bizId: '' })
      loadData()
    } finally {
      starting.value = false
    }
  })
}

const handleView = async (row: any) => {
  const data = await instanceApi.detail(row.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

const handleCancel = (row: any) => {
  ElMessageBox.confirm(`确认撤销流程「${row.title}」？撤销后待办任务将全部取消。`, '撤销流程', { type: 'warning' }).then(
    async () => {
      await instanceApi.cancel(row.id)
      ElMessage.success('已撤销')
      loadData()
    }
  )
}

onMounted(async () => {
  const data = await definitionApi.page({ pageNum: 1, pageSize: 100, status: 1 })
  publishedDefinitions.value = data.records
  loadData()
})
</script>

<style scoped>
.task-title {
  font-weight: 600;
}

.task-desc {
  font-size: 12px;
  color: #606266;
}
</style>








