<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">库存流水</div>
        <div class="page-header__desc">追踪物料出入库的流水记录</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="单据类型">
          <el-select v-model="query.tranType" placeholder="全部" clearable style="width: 180px">
            <el-option label="采购入库" value="PA_INSTOCK" />
            <el-option label="采购退货" value="PA_RETURN" />
            <el-option label="销售出库" value="SA_OUTSTOCK" />
            <el-option label="销售退货" value="SA_RETURN" />
            <el-option label="调拨出库" value="IC_TRANSFER_OUT" />
            <el-option label="调拨入库" value="IC_TRANSFER_IN" />
            <el-option label="盘点盘盈" value="IC_CHECK_GAIN" />
            <el-option label="盘点盘亏" value="IC_CHECK_LOSS" />
            <el-option label="生产领料" value="PM_PICK" />
            <el-option label="完工入库" value="PM_FINISH" />
          </el-select>
        </el-form-item>
        <el-form-item label="单据编号">
          <el-input v-model="query.bizNo" placeholder="单据编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="方向">
          <el-select v-model="query.direction" placeholder="全部" clearable style="width: 110px">
            <el-option label="入库" :value="1" />
            <el-option label="出库" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="bizDate" label="业务日期" width="110" />
        <el-table-column prop="tranType" label="单据类型" min-width="130" />
        <el-table-column prop="bizNo" label="单据编号" min-width="140" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="warehouseName" label="仓库" min-width="110" />
        <el-table-column prop="batchNo" label="批次号" width="100" />
        <el-table-column label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.direction === 1 ? 'success' : 'danger'">
              {{ row.direction === 1 ? '入库' : '出库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" width="100" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="记录时间" min-width="160" />
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
import { transactionApi } from '@/api/inventory'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  tranType: '',
  bizNo: '',
  direction: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await transactionApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  if (dateRange.value) {
    query.startDate = dateRange.value[0]
    query.endDate = dateRange.value[1]
  } else {
    query.startDate = ''
    query.endDate = ''
  }
  loadData()
}

const handleReset = () => {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    tranType: '',
    bizNo: '',
    direction: undefined,
    startDate: '',
    endDate: ''
  })
  dateRange.value = null
  loadData()
}

onMounted(loadData)
</script>








