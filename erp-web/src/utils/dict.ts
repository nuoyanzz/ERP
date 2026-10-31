import { ref, type Ref } from 'vue'
import { dictApi, type DictDataVO } from '@/api/dict'

/**
 * 字典下拉选项。组件内通过 v-model 绑定选中值。
 *
 * 用法：
 *   const statusOptions = useDictOptions('pa_order_status')
 *   <el-select v-model="form.status">
 *     <el-option v-for="opt in statusOptions" :key="opt.dictValue" :label="opt.dictLabel" :value="opt.dictValue" />
 *   </el-select>
 */
export interface DictOption {
  label: string
  value: string
  cssClass?: string
}

export function useDictOptions(dictType: string): Ref<DictOption[]> {
  const options = ref<DictOption[]>([]) as Ref<DictOption[]>

  const load = async () => {
    const list: DictDataVO[] = await dictApi.list(dictType)
    options.value = list.map((item) => ({
      label: item.dictLabel,
      value: item.dictValue,
      cssClass: item.cssClass
    }))
  }

  load().catch(() => {
    // 字典加载失败不阻塞页面，下拉为空即可
  })

  return options
}

/**
 * 按值取字典标签，用于列表页把状态码翻译成可读文本。
 */
export async function useDictLabel(dictType: string, value: string | number | undefined) {
  const label = ref('')
  if (value === undefined || value === null) {
    return label
  }
  try {
    const list = await dictApi.list(dictType)
    label.value = list.find((item) => item.dictValue === String(value))?.dictLabel ?? String(value)
  } catch {
    label.value = String(value)
  }
  return label
}








