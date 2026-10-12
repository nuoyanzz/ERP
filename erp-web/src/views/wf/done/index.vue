<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">我的已办</div>
        <div class="page-header__desc">查看已处理的流程任务记录</div>
      </div>
      <div class="page-header__extra">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="instanceTitle" label="流程标题" min-width="180" />
        <el-table-column prop="definitionName" label="流程名称" min-width="140" />
        <el-table-column prop="nodeName" label="审批节点" min-width="120" />
        <el-table-column prop="assigneeName" label="审批人" min-width="100" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '同意' : '驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="审批意见" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operateTime" label="处理时间" min-width="160" />
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { taskApi } from '@/api/workflow'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const loadData = async () => {
  loading.value = true
  try {
    const data = await taskApi.done(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
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








