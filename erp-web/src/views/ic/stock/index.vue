<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">即时库存</div>
        <div class="page-header__desc">查看各物料的现存量、锁存量与可用量</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="物料编码">
          <el-input v-model="query.materialCode" placeholder="物料编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="query.materialName" placeholder="物料名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable filterable style="width: 180px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
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
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="120" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="warehouseName" label="仓库" min-width="110" />
        <el-table-column prop="batchNo" label="批次号" width="110" />
        <el-table-column prop="qty" label="现存量" width="100" />
        <el-table-column prop="lockedQty" label="锁存量" width="100" />
        <el-table-column prop="availableQty" label="可用量" width="100" />
        <el-table-column prop="updateTime" label="更新时间" min-width="160" />
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
import { stockApi } from '@/api/inventory'
import { warehouseApi } from '@/api/master'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  materialCode: '',
  materialName: '',
  warehouseId: undefined as number | undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await stockApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, materialCode: '', materialName: '', warehouseId: undefined })
  loadData()
}

onMounted(async () => {
  warehouses.value = (await warehouseApi.page({ pageNum: 1, pageSize: 200 })).records
  loadData()
})
</script>








