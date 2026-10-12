import { defineStore } from 'pinia'
import request from '@/utils/request'
import type { LoginVO } from '@/api/auth'

interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number
  perms: string | null
  path: string | null
  component: string | null
  icon: string | null
  sortNo: number
  children?: MenuNode[]
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: (localStorage.getItem('erp_token') || '') as string,
    userId: (localStorage.getItem('erp_user_id') || '') as string,
    nickname: (localStorage.getItem('erp_nickname') || '') as string,
    perms: [] as string[],
    menus: [] as MenuNode[],
    menusLoaded: false
  }),
  actions: {
    async login(payload: { username: string; password: string }) {
      const data = await request.post<any, LoginVO>('/auth/login', payload)
      this.token = data.token
      this.userId = String(data.userId || '')
      this.nickname = data.nickname || data.username || ''
      this.perms = data.perms || []
      localStorage.setItem('erp_token', this.token)
      localStorage.setItem('erp_user_id', this.userId)
      localStorage.setItem('erp_nickname', this.nickname)
      return data
    },
    async loadMenus() {
      const [menus, perms] = await Promise.all([
        request.get<any, MenuNode[]>('/sys/menu/my'),
        request.get<any, string[]>('/sys/menu/my-perms')
      ])
      this.menus = menus || []
      this.perms = perms || []
      this.menusLoaded = true
      return this.menus
    },
    hasPerm(perm: string) {
      return this.perms.includes(perm)
    },
    resetToken() {
      this.token = ''
      this.userId = ''
      this.nickname = ''
      this.perms = []
      this.menus = []
      this.menusLoaded = false
      localStorage.removeItem('erp_token')
      localStorage.removeItem('erp_user_id')
      localStorage.removeItem('erp_nickname')
    },
    async logout() {
      this.resetToken()
    }
  }
})








