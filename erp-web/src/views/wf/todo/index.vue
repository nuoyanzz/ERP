<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">我的待办</div>
        <div class="page-header__desc">处理待审批的流程任务</div>
      </div>
      <div class="page-header__extra">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="instanceTitle" label="流程标题" min-width="180" />
        <el-table-column prop="definitionName" label="流程名称" min-width="140" />
        <el-table-column prop="nodeName" label="当前节点" min-width="120" />
        <el-table-column label="关联业务" min-width="150">
          <template #default="{ row }">
            <span v-if="row.bizType">{{ row.bizType }} / {{ row.bizId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="到达时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handleApprove(row)">同意</el-button>
            <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="approveVisible" title="审批同意" width="480px">
      <el-form label-width="0">
        <el-form-item>
          <el-input v-model="comment" type="textarea" :rows="3" placeholder="审批意见（可选）" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmApprove">同意</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="审批驳回" width="480px">
      <el-form label-width="0">
        <el-form-item>
          <el-input v-model="comment" type="textarea" :rows="3" placeholder="驳回原因（可选）" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="confirmReject">驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { taskApi } from '@/api/workflow'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const approveVisible = ref(false)
const rejectVisible = ref(false)
const comment = ref('')
const currentTaskId = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const data = await taskApi.todo(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleApprove = (row: any) => {
  currentTaskId.value = row.id
  comment.value = ''
  approveVisible.value = true
}

const handleReject = (row: any) => {
  currentTaskId.value = row.id
  comment.value = ''
  rejectVisible.value = true
}

const confirmApprove = async () => {
  submitting.value = true
  try {
    await taskApi.approve(currentTaskId.value, comment.value)
    ElMessage.success('已同意')
    approveVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const confirmReject = async () => {
  submitting.value = true
  try {
    await taskApi.reject(currentTaskId.value, comment.value)
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>








