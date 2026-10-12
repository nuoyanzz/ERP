<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">生产成本归集</div>
        <div class="page-header__desc">查看生产订单的材料、人工与制造费用构成</div>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form">
        <el-form-item label="生产订单">
          <el-select v-model="query.moId" filterable placeholder="全部" clearable style="width: 220px">
            <el-option v-for="m in moList" :key="m.id" :label="m.moNo" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成本类型">
          <el-select v-model="query.costType" placeholder="全部" clearable style="width: 140px">
            <el-option label="材料成本" :value="1" />
            <el-option label="人工成本" :value="2" />
            <el-option label="制造费用" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <span class="section-title">成本明细</span>
          </template>
          <el-table v-loading="loading" :data="list" border>
            <el-table-column prop="moNo" label="生产订单" min-width="150" />
            <el-table-column label="成本类型" width="110">
              <template #default="{ row }">
                <el-tag :type="costTypeTag(row.costType)">{{ costTypeText(row.costType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="costDate" label="成本日期" width="120" />
            <el-table-column prop="materialCost" label="材料成本" width="120" />
            <el-table-column prop="laborAmount" label="人工成本" width="120" />
            <el-table-column prop="overheadAmount" label="制造费用" width="120" />
            <el-table-column prop="otherAmount" label="其他" width="110" />
            <el-table-column prop="totalAmount" label="合计" width="130" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          </el-table>

          <div class="pager">
            <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
              :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
              @size-change="loadData" @current-page="loadData" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <template #header>
            <span class="section-title">工单成本汇总</span>
          </template>
          <div v-if="selectedMoId" class="summary">
            <div class="summary__row">
              <span class="summary__label">材料成本</span>
              <span class="summary__value">{{ summary.materialCost ?? 0 }}</span>
            </div>
            <div class="summary__row">
              <span class="summary__label">人工成本</span>
              <span class="summary__value">{{ summary.laborAmount ?? 0 }}</span>
            </div>
            <div class="summary__row">
              <span class="summary__label">制造费用</span>
              <span class="summary__value">{{ summary.overheadAmount ?? 0 }}</span>
            </div>
            <div class="summary__row summary__row--total">
              <span class="summary__label">成本合计</span>
              <span class="summary__value">{{ summary.totalAmount ?? 0 }}</span>
            </div>
            <div v-if="summary.unitCost" class="summary__row">
              <span class="summary__label">单位成本</span>
              <span class="summary__value">{{ summary.unitCost }}</span>
            </div>
          </div>
          <el-empty v-else description="选择左侧的生产订单查看成本汇总" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { costApi, moApi } from '@/api/production'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const moList = ref<any[]>([])
const summary = ref<any>({})
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  moId: undefined as number | undefined,
  costType: undefined as number | undefined
})

const selectedMoId = computed(() => query.moId)

const costTypeText = (type: number) =>
  ({ 1: '材料成本', 2: '人工成本', 3: '制造费用', 4: '其他' } as Record<number, string>)[type] || '其他'

const costTypeTag = (type: number) => {
  switch (type) {
    case 1:
      return 'primary'
    case 2:
      return 'success'
    case 3:
      return 'warning'
    default:
      return 'info'
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await costApi.page(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadSummary = async () => {
  if (!query.moId) {
    summary.value = {}
    return
  }
  summary.value = (await costApi.detail(query.moId)) || {}
}

const handleSearch = () => {
  query.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, moId: undefined, costType: undefined })
  summary.value = {}
  loadData()
}

watch(() => query.moId, loadSummary)

onMounted(async () => {
  loadData()
  const moRes = await moApi.page({ pageNum: 1, pageSize: 200 })
  moList.value = moRes?.records || []
})
</script>

<style scoped lang="scss">
.summary {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &__row {
    display: flex;
    justify-content: space-between;
    padding: 9px 4px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    font-size: 13px;

    &:last-child {
      border-bottom: none;
    }

    &--total {
      font-weight: 600;
      padding-top: 12px;
    }
  }

  &__label {
    color: var(--el-text-color-secondary);
  }

  &__value {
    color: var(--el-text-color-primary);
    font-weight: 500;
  }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
}
</style>








