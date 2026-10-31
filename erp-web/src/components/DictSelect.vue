<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    filterable
    @update:model-value="handleChange"
  >
    <el-option
      v-for="opt in options"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { dictApi, type DictDataVO } from '@/api/dict'

/**
 * 字典下拉选择器。
 * 状态、类型等枚举值统一由此组件渲染，字典在管理端可配置。
 */
const props = withDefaults(
  defineProps<{
    /** 字典类型编码，如 pa_order_status */
    dictType: string
    modelValue?: string | number
    placeholder?: string
    clearable?: boolean
    disabled?: boolean
  }>(),
  {
    modelValue: undefined,
    placeholder: '请选择',
    clearable: false,
    disabled: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | undefined): void
  (e: 'change', value: string | number | undefined): void
}>()

const options = ref<DictDataVO[]>([])

const load = async () => {
  options.value = await dictApi.list(props.dictType)
}

onMounted(load)
watch(() => props.dictType, load)

const handleChange = (value: string | number | undefined) => {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>








