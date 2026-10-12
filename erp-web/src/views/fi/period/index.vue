<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">会计期间</div>
        <div class="page-header__desc">按年度维护会计期间与结账状态</div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar-actions">
      <el-select v-model="year" placeholder="年度" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y} 年`" :value="y" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="handleGenerate">生成年度期间</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="periodMonth" label="期间" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">
              {{ row.status === 0 ? '未开放' : row.status === 1 ? '开放' : '已结账' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" v-perms="'fi:period:close'" link type="warning"
              @click="handleClose(row)">结账</el-button>
            <el-button v-if="row.status === 2" v-perms="'fi:period:open'" link type="success"
              @click="handleOpen(row)">反结账</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="tip">提示：期间内存在未记账凭证时不能结账；结账后该期间不能再录入/过账凭证。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { periodApi } from '@/api/finance'

const loading = ref(false)
const list = ref<any[]>([])
const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = Array.from({ length: 5 }, (_, index) => currentYear - 2 + index)

const loadData = async () => {
  loading.value = true
  try {
    list.value = await periodApi.list(year.value)
  } finally {
    loading.value = false
  }
}

const handleGenerate = async () => {
  await ElMessageBox.confirm(`确认生成 ${year.value} 年度 12 个会计期间？`, '生成期间', { type: 'warning' })
  const created = await periodApi.generate(year.value)
  ElMessage.success(`生成成功，新增 ${created} 个期间`)
  loadData()
}

const handleClose = (row: any) => {
  ElMessageBox.confirm(`确认对 ${row.periodMonth} 执行结账？`, '期间结账', { type: 'warning' }).then(async () => {
    await periodApi.close(row.id)
    ElMessage.success('结账成功')
    loadData()
  })
}

const handleOpen = (row: any) => {
  ElMessageBox.confirm(`确认对 ${row.periodMonth} 执行反结账？`, '期间反结账', { type: 'warning' }).then(async () => {
    await periodApi.open(row.id)
    ElMessage.success('反结账成功')
    loadData()
  })
}

onMounted(loadData)
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 12px;
  color: #e6a23c;
}
</style>








