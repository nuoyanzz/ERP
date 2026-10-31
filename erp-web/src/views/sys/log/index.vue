<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">操作日志</div>
        <div class="page-header__desc">记录业务写操作，供审计追溯与问题定位</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部" clearable filterable allow-create style="width: 150px">
            <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.operType" placeholder="全部" clearable filterable allow-create style="width: 150px">
            <el-option v-for="item in operTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operUsername" placeholder="操作人账号" clearable style="width: 150px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="是否成功">
          <el-select v-model="query.success" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">{{ moduleText(row.module) }}</template>
        </el-table-column>
        <el-table-column prop="operType" label="操作类型" width="120">
          <template #default="{ row }">{{ operTypeText(row.operType) }}</template>
        </el-table-column>
        <el-table-column prop="operDesc" label="操作描述" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.operDesc || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operUsername" label="操作人" width="110">
          <template #default="{ row }">{{ row.operUsername || '-' }}</template>
        </el-table-column>
        <el-table-column prop="method" label="请求方法" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.method" size="small" effect="plain">{{ row.method }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求 URL" min-width="220" show-overflow-tooltip />
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">{{ row.costTime ?? 0 }} ms</template>
        </el-table-column>
        <el-table-column label="是否成功" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" min-width="170" />
        <el-table-column prop="errorMsg" label="错误信息" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMsg || '-' }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { operLogApi } from '@/api/system'

/** 操作模块：与后端 @OperLog(module = ...) 一致 */
const moduleOptions = [
  { label: '采购 pa', value: 'pa' },
  { label: '销售 sa', value: 'sa' },
  { label: '库存 ic', value: 'ic' },
  { label: '财务 fi', value: 'fi' },
  { label: '生产 pm', value: 'pm' },
  { label: '应收 ar', value: 'ar' },
  { label: '应付 ap', value: 'ap' },
  { label: '人事 hr', value: 'hr' },
  { label: '系统 system', value: 'system' },
  { label: '流程 wf', value: 'wf' }
]

/** 操作类型：与后端 @OperLog(type = ...) 一致 */
const operTypeOptions = [
  { label: '新增 CREATE', value: 'CREATE' },
  { label: '修改 UPDATE', value: 'UPDATE' },
  { label: '删除 DELETE', value: 'DELETE' },
  { label: '导入 IMPORT', value: 'IMPORT' },
  { label: '审核 AUDIT', value: 'AUDIT' },
  { label: '反审核 UNAUDIT', value: 'UNAUDIT' },
  { label: '记账 POST', value: 'POST' },
  { label: '反记账 UNPOST', value: 'UNPOST' },
  { label: '作废 VOID', value: 'VOID' },
  { label: '红冲 RED_FLUSH', value: 'RED_FLUSH' },
  { label: '结账 CLOSE', value: 'CLOSE' },
  { label: '反结账 OPEN', value: 'OPEN' },
  { label: '试算平衡 TRIAL_BALANCE', value: 'TRIAL_BALANCE' },
  { label: '刷新缓存 REFRESH', value: 'REFRESH' }
]

const moduleText = (value: string) => moduleOptions.find((item) => item.value === value)?.label ?? value ?? '-'
const operTypeText = (value: string) => operTypeOptions.find((item) => item.value === value)?.label ?? value ?? '-'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  module: '',
  operType: '',
  operUsername: '',
  success: undefined as number | undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await operLogApi.page(query)
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
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    module: '',
    operType: '',
    operUsername: '',
    success: undefined
  })
  loadData()
}

onMounted(loadData)
</script>
