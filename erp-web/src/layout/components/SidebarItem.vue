<template>
  <!-- 目录：渲染成可展开的子菜单 -->
  <el-sub-menu v-if="isDirectory" :index="String(node.id)">
    <template #title>
      <el-icon v-if="node.icon"><component :is="iconName(node.icon)" /></el-icon>
      <span>{{ node.menuName }}</span>
    </template>
    <sidebar-item v-for="child in node.children || []" :key="child.id" :node="child" />
  </el-sub-menu>

  <!-- 页面：渲染成菜单项 -->
  <el-menu-item v-else :index="fullPath">
    <el-icon v-if="node.icon"><component :is="iconName(node.icon)" /></el-icon>
    <template #title>{{ node.menuName }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number
  path?: string | null
  component?: string | null
  icon?: string | null
  children?: MenuNode[]
}

/**
 * 递归菜单项。目录（menuType=1）渲染为子菜单，页面（menuType=2）渲染为菜单项。
 */
const props = defineProps<{ node: MenuNode }>()

const isDirectory = computed(() => props.node.menuType === 1)

const fullPath = computed(() => {
  const path = props.node.path
  if (!path) {
    return String(props.node.id)
  }
  return path.startsWith('/') ? path : `/${path}`
})

/**
 * 解析菜单图标。Element Plus 的图标以组件名导出，
 * 这里做一次安全查找，找不到时回退到默认图标。
 */
const iconName = (name: string) => {
  return (ElementPlusIconsVue as any)[name] || ElementPlusIconsVue.Menu
}
</script>








