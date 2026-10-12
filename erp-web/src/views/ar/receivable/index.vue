<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">应收单</div>
        <div class="page-header__desc">管理销售应收单与核销状态</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="应收单号">
          <el-input v-model="query.receivableNo" placeholder="应收单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="query.customerId" filterable clearable placeholder="全部客户" style="width: 200px">
            <el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="未结清" :value="0" />
            <el-option label="已结清" :value="1" />
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
        <el-table-column prop="receivableNo" label="应收单号" min-width="150" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="sourceType" label="来源类型" width="110">
          <template #default="{ row }">
            {{ row.sourceType === 'SA_OUTSTOCK' ? '销售出库' : row.sourceType || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sourceNo" label="来源单据" min-width="150" />
        <el-table-column prop="amount" label="应收金额" width="110" />
        <el-table-column prop="receivedAmount" label="已收金额" width="110" />
        <el-table-column prop="unreceivedAmount" label="未收金额" width="110" />
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已结清' : '未结清' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="应收单详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="应收单号">{{ detail.receivableNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="来源单据">{{ detail.sourceNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期日">{{ detail.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="应收金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="未收金额">{{ detail.unreceivedAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail.status === 1 ? '已结清' : '未结清' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { arReceivableApi } from '@/api/ar-ap'
import { customerApi } from '@/api/master'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const customers = ref<any[]>([])
const detailVisible = ref(false)
const detail = reactive<any>({})
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  receivableNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await arReceivableApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, receivableNo: '', customerId: undefined, status: undefined })
  loadData()
}

const handleView = (row: any) => {
  Object.assign(detail, row)
  detailVisible.value = true
}

onMounted(async () => {
  customers.value = (await customerApi.page({ pageNum: 1, pageSize: 200 })).records
  loadData()
})
</script>








