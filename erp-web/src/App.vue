<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { mountBusinessRoutes } from '@/router'

const userStore = useUserStore()

/**
 * 登录后按后端返回的菜单树动态挂载业务路由。
 * 菜单与页面的对应关系由后端配置驱动，新增模块无需改动前端路由表。
 */
onMounted(async () => {
  if (!userStore.token || userStore.menusLoaded) {
    return
  }
  try {
    const menus = await userStore.loadMenus()
    mountBusinessRoutes(menus)
  } catch (e) {
    // 菜单加载失败由路由守卫统一跳转登录页
  }
})
</script>








