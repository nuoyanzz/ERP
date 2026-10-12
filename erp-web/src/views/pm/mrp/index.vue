<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">MRP 运算</div>
        <div class="page-header__desc">按销售需求计算净需求，可投放为采购订单或生产订单</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="运算批次">
          <el-input v-model="query.runNo" placeholder="如 MRP-20261031-001" clearable style="width: 200px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="建议类型">
          <el-select v-model="query.suggestType" placeholder="全部" clearable style="width: 130px">
            <el-option label="采购建议" :value="1" />
            <el-option label="生产建议" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'pm:mrp:run'" type="primary" :icon="Operation" @click="handleRun">执行运算</el-button>
      <el-button v-perms="'pm:mrp:releasePurchase'" type="success" :icon="ShoppingCart"
        @click="handleReleasePurchase">投放采购订单</el-button>
      <el-button v-perms="'pm:mrp:releaseProduction'" type="warning" :icon="SetUp"
        @click="handleReleaseProduction">投放生产订单</el-button>
    </div>

    <el-card shadow="never">
      <el-alert v-if="runNo" type="info" :closable="false" style="margin-bottom: 12px">
        当前批次：<strong>{{ runNo }}</strong>
        <template v-if="lastResult">
          ，本次投放生成采购订单 {{ lastResult.purchaseOrderCount || 0 }} 张、生产订单
          {{ lastResult.productionOrderCount || 0 }} 张
          <template v-if="lastResult.skipped?.length">
            ；跳过 {{ lastResult.skipped.length }} 项：{{ lastResult.skipped.map((s: any) => s.reason).join('；') }}
          </template>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="materialCode" label="物料编码" width="130" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" min-width="120" show-overflow-tooltip />
        <el-table-column label="建议类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.suggestType === 1 ? 'primary' : 'warning'">
              {{ row.suggestType === 1 ? '采购' : '生产' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="grossQty" label="毛需求" width="100" />
        <el-table-column prop="availableQty" label="可用库存" width="100" />
        <el-table-column prop="onOrderQty" label="在途量" width="100" />
        <el-table-column prop="safeStock" label="安全库存" width="100" />
        <el-table-column prop="netQty" label="净需求" width="100" />
        <el-table-column prop="plannedQty" label="计划量" width="100" />
        <el-table-column prop="suggestDate" label="建议下单日" width="120" />
        <el-table-column prop="leadTimeDays" label="提前期" width="90" />
        <el-table-column label="投放状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.releaseStatus === 1 ? 'success' : 'info'">
              {{ row.releaseStatus === 1 ? '已投放' : '未投放' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-page="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Operation, Refresh, Search, SetUp, ShoppingCart } from '@element-plus/icons-vue'
import { mrpApi } from '@/api/production'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const runNo = ref('')
const lastResult = ref<any>(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  runNo: '',
  suggestType: undefined as number | undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await mrpApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, runNo: '', suggestType: undefined })
  loadData()
}

const handleRun = async () => {
  loading.value = true
  try {
    const result = await mrpApi.run()
    runNo.value = result.runNo || ''
    lastResult.value = null
    ElMessage.success(`运算完成，批次 ${result.runNo || ''}`)
    await loadData()
  } finally {
    loading.value = false
  }
}

const handleReleasePurchase = () => {
  ElMessageBox.confirm(
    `确认把批次 ${runNo.value || '最新批次'} 的采购建议投放为采购订单？`,
    '投放采购订单',
    { type: 'warning' }
  ).then(async () => {
    const result = await mrpApi.releasePurchase(runNo.value)
    lastResult.value = result
    ElMessage.success(`已生成采购订单 ${result.purchaseOrderCount || 0} 张`)
    loadData()
  })
}

const handleReleaseProduction = () => {
  ElMessageBox.confirm(
    `确认把批次 ${runNo.value || '最新批次'} 的生产建议投放为生产订单？`,
    '投放生产订单',
    { type: 'warning' }
  ).then(async () => {
    const result = await mrpApi.releaseProduction(runNo.value)
    lastResult.value = result
    ElMessage.success(`已生成生产订单 ${result.productionOrderCount || 0} 张`)
    loadData()
  })
}

onMounted(async () => {
  try {
    runNo.value = (await mrpApi.latestRunNo()) || ''
  } catch (e) {
    // 尚未执行过运算，留空即可
  }
  loadData()
})
</script>








