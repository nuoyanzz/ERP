import request from '@/utils/request'

export interface DictDataVO {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  sortNo: number
  cssClass?: string
  listClass?: string
  isDefault: number
}

/**
 * 字典数据在应用内缓存，避免每个页面重复请求同一份字典。
 */
const cache = new Map<string, DictDataVO[]>()

export const dictApi = {
  /**
   * 按字典类型读取字典项，结果在应用内缓存。
   */
  async list(dictType: string, force = false): Promise<DictDataVO[]> {
    if (!force && cache.has(dictType)) {
      return cache.get(dictType)!
    }
    const data = await request.get<any, DictDataVO[]>(`/system/dict/data/${dictType}`)
    cache.set(dictType, data || [])
    return cache.get(dictType)!
  },

  /**
   * 清空本地字典缓存。字典管理页维护后调用，或在刷新页面时自然重置。
   */
  clearCache(): void {
    cache.clear()
  },

  /**
   * 通知服务端刷新字典缓存。
   */
  refresh: () => request.post<any, void>('/system/dict/refresh')
}








