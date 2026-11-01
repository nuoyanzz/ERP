<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '210px'" class="layout__aside">
      <div class="layout__logo">
        <el-icon :size="20"><Setting /></el-icon>
        <span v-if="!collapsed" class="layout__logo-text">ERP 管理系统</span>
      </div>
      <el-scrollbar class="layout__menu-scroll">
        <el-menu
          :default-active="activePath"
          :collapse="collapsed"
          :unique-opened="false"
          router
          background-color="transparent"
        >
          <sidebar-item v-for="node in menus" :key="node.id" :node="node" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="layout__main">
      <!-- 顶栏 -->
      <el-header class="layout__header">
        <div class="layout__header-left">
          <el-icon class="layout__collapse" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="layout__header-right">
          <el-tooltip content="退出登录" placement="bottom">
            <el-dropdown @command="handleCommand">
              <span class="layout__user">
                <el-avatar :size="28">{{ (nickname || 'U').charAt(0) }}</el-avatar>
                <span class="layout__user-name">{{ nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-tooltip>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout__content">
        <router-view v-slot="{ Component }">
          <keep-alive :max="8">
            <component :is="Component" :key="routeKey" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Expand, Fold, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import SidebarItem from './components/SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

const menus = computed(() => userStore.menus || [])
const nickname = computed(() => userStore.nickname)

const activePath = computed(() => route.path)

const routeKey = computed(() => route.fullPath)

/**
 * 面包屑：取路由匹配链上的标题，最后一段是当前页面。
 */
const breadcrumbs = computed(() => {
  const matched = route.matched.filter((item) => item.meta?.title)
  if (matched.length === 0 && route.meta?.title) {
    return [route.meta.title as string]
  }
  return matched.map((item) => item.meta?.title as string)
})

const handleCommand = async (command: string) => {
  if (command !== 'logout') {
    return
  }
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  await userStore.logout()
  router.push('/login')
}

watch(
  () => route.path,
  () => {
    collapsed.value = false
  }
)
</script>

<style scoped lang="scss">
.layout {
  height: 100vh;
}

.layout__aside {
  background-color: #1f2a3d;
  transition: width 0.2s;
  overflow: hidden;
}

.layout__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 56px;
  padding: 0 18px;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  border-bottom: 1px solid rgb(255 255 255 / 6%);
}

.layout__logo-text {
  overflow: hidden;
}

.layout__menu-scroll {
  height: calc(100vh - 56px);
}

/* 侧边栏是深色底，菜单文字需改为浅色，否则在深色背景上几乎不可读 */
.layout__aside :deep(.el-menu) {
  background-color: transparent;
  border-right: none;
}

.layout__aside :deep(.el-menu-item),
.layout__aside :deep(.el-sub-menu__title) {
  color: rgb(255 255 255 / 72%);
}

.layout__aside :deep(.el-menu-item:hover),
.layout__aside :deep(.el-sub-menu__title:hover) {
  color: #fff;
  background-color: rgb(255 255 255 / 6%);
}

.layout__aside :deep(.el-menu-item.is-active) {
  color: #fff;
  background-color: var(--el-color-primary);
}

.layout__aside :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
}

.layout__aside :deep(.el-sub-menu .el-menu) {
  background-color: rgb(0 0 0 / 16%);
}

.layout__main {
  min-width: 0;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 18px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.layout__header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.layout__collapse {
  cursor: pointer;
  font-size: 18px;
  color: var(--el-text-color-regular);
}

.layout__header-right {
  display: flex;
  align-items: center;
}

.layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.layout__user-name {
  font-size: 13px;
}

.layout__content {
  padding: 0;
  background-color: var(--el-bg-color-page);
  overflow-y: auto;
}
</style>








