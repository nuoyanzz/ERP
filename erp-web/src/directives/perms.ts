import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 权限指令：按权限码控制元素显隐。
 * 用法：v-perms="'pa:order:audit'"，无权限时移除该元素。
 */
export const perms: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    if (!required.length) {
      return
    }
    const userStore = useUserStore()
    const granted = required.some((code) => userStore.hasPerm(code))
    if (!granted) {
      el.parentNode?.removeChild(el)
    }
  }
}








