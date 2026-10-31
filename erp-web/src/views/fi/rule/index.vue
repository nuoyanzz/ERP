<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <div class="page-header__title">科目映射规则</div>
        <div class="page-header__desc">配置「业务单据 → 会计科目」的映射，修改后新生成的单据会生效</div>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="rule-tip"
      title="规则说明" description="规则按「业务类型 + 规则键」唯一确定一条映射。当业务单据生成凭证时，系统按业务类型与规则键查找对应科目编码；未配置规则的科目会报错「凭证科目规则未配置」，因此修改规则前请确认目标科目已存在且为明细科目。规则仅对修改之后新生成的单据生效，已生成的凭证不受影响。" />

    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="toolbar-form" @submit.prevent>
        <el-form-item label="业务类型">
          <el-select v-model="query.bizType" placeholder="全部" clearable filterable style="width: 180px">
            <el-option v-for="item in bizTypeOptions" :key="item.value" :label="`${item.label} (${item.value})`"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则键">
          <el-input v-model="query.ruleKey" placeholder="规则键" clearable style="width: 140px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="科目编码">
          <el-input v-model="query.subjectCode" placeholder="科目编码" clearable style="width: 140px"
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar-actions">
      <el-button v-perms="'fi:rule:add'" type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column type="index" label="#" width="45" />
        <el-table-column prop="bizType" label="业务类型" min-width="160">
          <template #default="{ row }">
            {{ bizTypeText(row.bizType) }}
            <span class="code-text">{{ row.bizType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ruleKey" label="规则键" min-width="140">
          <template #default="{ row }">
            {{ ruleKeyText(row.ruleKey) }}
            <span class="code-text">{{ row.ruleKey }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subjectCode" label="科目编码" min-width="140" />
        <el-table-column prop="subjectName" label="科目名称" min-width="160">
          <template #default="{ row }">{{ row.subjectName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-perms="'fi:rule:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-perms="'fi:rule:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑映射规则' : '新增映射规则'" width="520px">
      <el-form ref="formRef" :model="editing" :rules="rules" label-width="100px">
        <el-form-item label="业务类型" prop="bizType">
          <el-select v-model="editing.bizType" filterable allow-create default-first-option placeholder="选择或输入业务类型"
            style="width: 100%">
            <el-option v-for="item in bizTypeOptions" :key="item.value" :label="`${item.label} (${item.value})`"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则键" prop="ruleKey">
          <el-select v-model="editing.ruleKey" filterable allow-create default-first-option placeholder="选择或输入规则键"
            style="width: 100%">
            <el-option v-for="item in ruleKeyOptions" :key="item.value" :label="`${item.label} (${item.value})`"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="科目编码" prop="subjectCode">
          <el-select v-model="editing.subjectCode" filterable allow-create default-first-option placeholder="选择或输入科目编码"
            style="width: 100%">
            <el-option v-for="item in accountOptions" :key="item.value" :label="`${item.value} ${item.label}`"
              :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editing.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { accountApi, bizAccountRuleApi, type AccountTreeVO } from '@/api/finance'

/** 业务类型：与后端生成凭证时使用的 bizType 一致 */
const bizTypeOptions = [
  { label: '采购入库', value: 'PA_INSTOCK' },
  { label: '采购退货', value: 'PA_RETURN' },
  { label: '销售出库', value: 'SA_OUTSTOCK' },
  { label: '销售退货', value: 'SA_RETURN' },
  { label: '库存盘点', value: 'IC_CHECK' },
  { label: '生产领料', value: 'PM_PICK' },
  { label: '生产报工', value: 'PM_REPORT' },
  { label: '完工入库', value: 'PM_FINISH' },
  { label: '应收收款', value: 'AR_RECEIPT' },
  { label: '坏账计提', value: 'AR_BAD_DEBT' },
  { label: '应付付款', value: 'AP_PAYMENT' },
  { label: '薪资核算', value: 'HR_SALARY' },
  { label: '损益结转', value: 'CARRY_FORWARD' },
  { label: '年末结转', value: 'YEAR_END' }
]

/** 规则键：与各业务单据生成凭证行时使用的 ruleKey 一致 */
const ruleKeyOptions = [
  { label: '存货', value: 'MATERIAL' },
  { label: '收入', value: 'REVENUE' },
  { label: '成本', value: 'COST' },
  { label: '税金', value: 'TAX' },
  { label: '应收', value: 'RECEIVABLE' },
  { label: '应付', value: 'PAYABLE' },
  { label: '银行', value: 'BANK' },
  { label: '库存', value: 'STOCK' },
  { label: '待处理', value: 'SUSPENSE' },
  { label: '制造费用', value: 'OVERHEAD' },
  { label: '费用', value: 'EXPENSE' },
  { label: '社保', value: 'SOCIAL' },
  { label: '个税', value: 'TAX_SALARY' },
  { label: '坏账损失', value: 'BAD_DEBT_LOSS' },
  { label: '坏账准备', value: 'BAD_DEBT_PROVISION' }
]

const bizTypeText = (value: string) => bizTypeOptions.find((item) => item.value === value)?.label ?? value
const ruleKeyText = (value: string) => ruleKeyOptions.find((item) => item.value === value)?.label ?? value

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const accountTree = ref<AccountTreeVO[]>([])
const accountOptions = ref<{ label: string; value: string }[]>([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  bizType: '',
  ruleKey: '',
  subjectCode: ''
})
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editing = reactive<any>({})

const rules = {
  bizType: [{ required: true, message: '请选择或输入业务类型', trigger: 'change' }],
  ruleKey: [{ required: true, message: '请选择或输入规则键', trigger: 'change' }],
  subjectCode: [{ required: true, message: '请选择或输入科目编码', trigger: 'change' }]
}

const emptyEditing = () => ({
  id: undefined,
  bizType: '',
  ruleKey: '',
  subjectCode: '',
  remark: ''
})

/** 拉平科目树，生成科目编码下拉选项 */
const flattenAccounts = (nodes: AccountTreeVO[], result: { label: string; value: string }[] = []) => {
  for (const node of nodes) {
    result.push({ label: node.subjectName, value: node.subjectCode })
    if (node.children?.length) {
      flattenAccounts(node.children, result)
    }
  }
  return result
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await bizAccountRuleApi.page(query)
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
  Object.assign(query, { pageNum: 1, pageSize: 10, bizType: '', ruleKey: '', subjectCode: '' })
  loadData()
}

const handleCreate = () => {
  Object.assign(editing, emptyEditing())
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  const detail = await bizAccountRuleApi.detail(row.id)
  Object.assign(editing, emptyEditing(), detail)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        id: editing.id,
        bizType: editing.bizType,
        ruleKey: editing.ruleKey,
        subjectCode: editing.subjectCode,
        remark: editing.remark
      }
      if (editing.id) {
        await bizAccountRuleApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await bizAccountRuleApi.create(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除规则「${row.bizType} / ${row.ruleKey}」？`, '提示', { type: 'warning' }).then(async () => {
    await bizAccountRuleApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(async () => {
  accountTree.value = await accountApi.tree()
  accountOptions.value = flattenAccounts(accountTree.value)
  loadData()
})
</script>

<style scoped lang="scss">
.rule-tip {
  margin-bottom: 12px;
}

.code-text {
  margin-left: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
