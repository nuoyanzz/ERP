<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">应付单</div>
        <div class="page-header__desc">管理采购应付单与核销状态</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="应付单号">
          <el-input v-model="query.payableNo" placeholder="应付单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="query.supplierId" filterable clearable placeholder="全部供应商" style="width: 200px">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
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
        <el-table-column prop="payableNo" label="应付单号" min-width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column prop="sourceNo" label="来源单据" min-width="150" />
        <el-table-column prop="amount" label="应付金额" width="110" />
        <el-table-column prop="paidAmount" label="已付金额" width="110" />
        <el-table-column prop="unpaidAmount" label="未付金额" width="110" />
        <el-table-column prop="dueDate" label="到期日" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已结清' : '未结清' }}
            </el-tag>
          </template>
        </el-table-column>
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
import { Search, Refresh } from '@element-plus/icons-vue'
import { apPayableApi } from '@/api/ar-ap'
import { supplierApi } from '@/api/master'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const suppliers = ref<any[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  payableNo: '',
  supplierId: undefined as number | undefined,
  status: undefined as number | undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await apPayableApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, payableNo: '', supplierId: undefined, status: undefined })
  loadData()
}

onMounted(async () => {
  suppliers.value = (await supplierApi.page({ pageNum: 1, pageSize: 200 })).records
  loadData()
})
</script>








