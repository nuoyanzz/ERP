<template>
  <el-tag v-if="label" :type="tagType" size="small" effect="light">
    {{ label }}
  </el-tag>
  <span v-else>-</span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { dictApi, type DictDataVO } from '@/api/dict'

/**
 * 字典标签。把状态码渲染成带颜色的标签。
 */
const props = withDefaults(
  defineProps<{
    dictType: string
    value?: string | number
  }>(),
  { value: undefined }
)

const label = ref('')
const listClass = ref('')

const load = async () => {
  if (props.value === undefined || props.value === null || props.value === '') {
    label.value = ''
    return
  }
  try {
    const list: DictDataVO[] = await dictApi.list(props.dictType)
    const item = list.find((opt) => opt.dictValue === String(props.value))
    label.value = item?.dictLabel ?? String(props.value)
    listClass.value = item?.listClass ?? ''
  } catch {
    label.value = String(props.value)
  }
}

watch(() => [props.dictType, props.value], load, { immediate: true })

/**
 * 把字典配置的样式类映射为 el-tag 的 type。
 */
const tagType = computed(() => {
  switch (listClass.value) {
    case 'success':
      return 'success'
    case 'warning':
      return 'warning'
    case 'danger':
      return 'danger'
    case 'primary':
      return 'primary'
    case 'info':
      return 'info'
    default:
      return 'info'
  }
})
</script>








