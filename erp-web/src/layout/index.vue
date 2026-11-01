<template>
  <div class="kd-layout">
    <!-- ============ 顶部：品牌条 + 模块功能条 ============ -->
    <header class="kd-topbar">
      <div class="kd-topbar__brand">
        <el-icon :size="18"><Setting /></el-icon>
        <span class="kd-topbar__logo-text">ERP 管理系统</span>
      </div>

      <!-- 模块级功能条：点击切换左侧树 -->
      <nav class="kd-modbar">
        <button
          v-for="mod in modules"
          :key="mod.id"
          type="button"
          class="kd-modbar__item"
          :class="{ 'is-active': activeModuleId === mod.id }"
          @click="switchModule(mod)"
        >
          {{ mod.menuName }}
        </button>
      </nav>

      <div class="kd-topbar__right">
        <el-tooltip content="退出登录" placement="bottom">
          <el-dropdown @command="handleCommand">
            <span class="kd-topbar__user">
              <el-avatar :size="22">{{ (nickname || 'U').charAt(0) }}</el-avatar>
              <span class="kd-topbar__user-name">{{ nickname }}</span>
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
    </header>

    <div class="kd-body">
      <!-- ============ 左侧：模块内的功能树 ============ -->
      <aside v-if="sidebarVisible" class="kd-sidebar">
        <div class="kd-sidebar__title">
          {{ activeModuleName }}
        </div>
        <el-scrollbar class="kd-sidebar__scroll">
          <el-menu
            :default-active="activePath"
            :unique-opened="false"
            router
            class="kd-sidebar__menu"
          >
            <sidebar-item v-for="node in sidebarMenus" :key="node.id" :node="node" />
          </el-menu>
        </el-scrollbar>
      </aside>

      <!-- ============ 右侧：工作区 ============ -->
      <main class="kd-workspace">
        <!-- 工作区页签 -->
        <div class="kd-tabs">
          <div class="kd-tabs__crumb">
            {{ breadcrumbs.join(' / ') }}
          </div>
        </div>

        <div class="kd-workspace__body">
          <router-view v-slot="{ Component }">
            <keep-alive :max="8">
              <component :is="Component" :key="routeKey" />
            </keep-alive>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import SidebarItem from './components/SidebarItem.vue'

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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed<MenuNode[]>(() => (userStore.menus as MenuNode[]) || [])

/**
 * 顶部功能条：一级节点即模块。
 * 工作台是菜单类型 2 的独立页面，其余是目录（类型 1），都作为模块入口。
 */
const modules = computed(() => menus.value.filter((node) => node.menuType === 1 || node.menuType === 2))

/**
 * 当前激活的模块：优先按路由路径反查所属目录，其次取第一个模块。
 */
const activeModuleId = computed(() => {
  const path = route.path
  const hit = modules.value.find((mod) => {
    if (mod.menuType === 2) {
      return path === mod.path || path.startsWith(`${mod.path}/`)
    }
    return path === mod.path || path.startsWith(`${mod.path}/`)
  })
  return hit ? hit.id : modules.value.length ? modules.value[0].id : 0
})

const activeModuleName = computed(() => {
  const mod = modules.value.find((m) => m.id === activeModuleId.value)
  return mod ? mod.menuName : '功能菜单'
})

/**
 * 左侧树：只展示当前模块下的二级菜单。
 * 目录本身作为分组标题，其子节点渲染为可点击项。
 */
const sidebarMenus = computed<MenuNode[]>(() => {
  const mod = modules.value.find((m) => m.id === activeModuleId.value)
  if (!mod || mod.menuType === 2) {
    return []
  }
  return (mod.children || []).filter((child) => child.menuType === 2)
})

/**
 * 工作台这类单页模块没有下级菜单，此时收起左侧树，
 * 让工作台独占整个宽度，避免出现空白的树面板。
 */
const sidebarVisible = computed(() => {
  const mod = modules.value.find((m) => m.id === activeModuleId.value)
  return !!mod && mod.menuType === 1 && sidebarMenus.value.length > 0
})

const nickname = computed(() => userStore.nickname)

const activePath = computed(() => route.path)

const routeKey = computed(() => route.fullPath)

/**
 * 面包屑：模块名 + 页面名。
 */
const breadcrumbs = computed(() => {
  const matched = route.matched.filter((item) => item.meta?.title)
  const titles = matched.map((item) => item.meta?.title as string)
  return titles.length ? titles : [activeModuleName.value]
})

/**
 * 切换模块：跳到该模块的第一个可用页面。
 */
const switchModule = (mod: MenuNode) => {
  if (activeModuleId.value === mod.id) {
    return
  }
  if (mod.menuType === 2 && mod.path) {
    router.push(mod.path)
    return
  }
  const first = (mod.children || []).find((child) => child.menuType === 2 && child.path)
  if (first?.path) {
    router.push(first.path.startsWith('/') ? first.path : `/${first.path}`)
  }
}

const handleCommand = async (command: string) => {
  if (command !== 'logout') {
    return
  }
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  await userStore.logout()
  router.push('/login')
}

</script>

<style scoped lang="scss">
/* ============================================================
   三段式主界面布局
   顶部模块功能条 + 左侧功能树 + 右侧工作区
   平面、紧凑、靠边框分区
   ============================================================ */

.kd-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* ---------- 顶部 ---------- */
.kd-topbar {
  display: flex;
  align-items: stretch;
  height: 38px;
  flex-shrink: 0;
  background-color: #1a5fb4;
  color: #fff;
}

.kd-topbar__brand {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  flex-shrink: 0;
  background-color: #154a8c;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.kd-topbar__logo-text {
  letter-spacing: 0.5px;
}

/* 模块功能条 */
.kd-modbar {
  display: flex;
  align-items: stretch;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.kd-modbar::-webkit-scrollbar {
  display: none;
}

.kd-modbar__item {
  padding: 0 14px;
  border: none;
  background: transparent;
  color: rgb(255 255 255 / 82%);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: background-color 0.15s;
}

.kd-modbar__item:hover {
  background-color: rgb(255 255 255 / 12%);
  color: #fff;
}

.kd-modbar__item.is-active {
  background-color: #154a8c;
  color: #fff;
  border-bottom-color: #ffd04b;
}

.kd-topbar__right {
  display: flex;
  align-items: center;
  padding: 0 12px;
  flex-shrink: 0;
  background-color: #154a8c;
}

.kd-topbar__user {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  font-size: 13px;
  outline: none;
}

/* ---------- 主体 ---------- */
.kd-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* ---------- 左侧树 ---------- */
.kd-sidebar {
  width: 180px;
  flex-shrink: 0;
  background-color: #fff;
  border-right: 1px solid var(--el-border-color);
  display: flex;
  flex-direction: column;
}

.kd-sidebar__title {
  padding: 7px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.kd-sidebar__scroll {
  flex: 1;
  min-height: 0;
}

/* 菜单项：平面化，激活项用左侧色条标识 */
.kd-sidebar__menu {
  border-right: none;
  background-color: transparent;
}

.kd-sidebar__menu :deep(.el-menu-item),
.kd-sidebar__menu :deep(.el-sub-menu__title) {
  height: 30px;
  line-height: 30px;
  padding-left: 14px !important;
  font-size: 13px;
  color: var(--el-text-color-regular);
  border-right: none;
  border-left: 2px solid transparent;
}

.kd-sidebar__menu :deep(.el-menu-item:hover),
.kd-sidebar__menu :deep(.el-sub-menu__title:hover) {
  background-color: var(--el-fill-color-light);
  color: var(--el-color-primary);
}

.kd-sidebar__menu :deep(.el-menu-item.is-active) {
  background-color: #e8f0fb;
  color: var(--el-color-primary);
  font-weight: 600;
  border-left-color: var(--el-color-primary);
}

.kd-sidebar__menu :deep(.el-icon) {
  font-size: 13px;
  margin-right: 6px;
  color: var(--el-text-color-secondary);
}

/* ---------- 右侧工作区 ---------- */
.kd-workspace {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
}

/* 工作区页签条 */
.kd-tabs {
  display: flex;
  align-items: center;
  height: 30px;
  padding: 0 8px;
  flex-shrink: 0;
  background-color: #fff;
  border-bottom: 1px solid var(--el-border-color);
}

.kd-tabs__crumb {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kd-tabs__spacer {
  flex: 1;
}

.kd-workspace__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

</style>
