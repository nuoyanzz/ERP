import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 静态路由：无需权限即可访问。
 * 业务页面由后端菜单驱动，在登录后动态挂载。
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '工作台', icon: 'dashboard' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 已挂载的业务路由路径，用于避免重复挂载。
 */
const mountedPaths = new Set<string>()

/**
 * 把后端返回的菜单树转换成路由记录。
 * <p>
 * 后端菜单的 component 字段是相对路径（如 sys/user/index），
 * 这里用 import.meta.glob 预加载的页面模块做匹配。
 */
const pageModules = import.meta.glob('../views/**/*.vue')

function resolveComponent(component?: string | null) {
  if (!component) {
    return undefined
  }
  const normalized = component.replace(/^\/+/, '')
  const candidates = [
    `../views/${normalized}.vue`,
    `../views/${normalized}/index.vue`
  ]
  for (const candidate of candidates) {
    if (pageModules[candidate]) {
      return pageModules[candidate]
    }
  }
  return undefined
}

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
  visible: number
  children?: MenuNode[]
}

/**
 * 挂载业务路由。菜单类型 2 为页面，1 为目录。
 */
export function mountBusinessRoutes(menus: MenuNode[]) {
  const routes: RouteRecordRaw[] = []

  const walk = (nodes: MenuNode[], parentPath: string) => {
    for (const node of nodes) {
      if (node.visible === 1) {
        continue
      }
      const fullPath = node.path
        ? (node.path.startsWith('/') ? node.path : `${parentPath}/${node.path}`.replace(/\/+/g, '/'))
        : parentPath

      if (node.menuType === 1) {
        // 目录：子菜单的父级
        walk(node.children || [], fullPath === '/' ? '' : fullPath)
        continue
      }

      if (node.menuType !== 2 || !node.component) {
        continue
      }
      if (mountedPaths.has(fullPath)) {
        continue
      }
      const loader = resolveComponent(node.component)
      if (!loader) {
        continue
      }
      mountedPaths.add(fullPath)
      routes.push({
        path: fullPath,
        name: `menu-${node.id}`,
        component: loader,
        meta: {
          title: node.menuName,
          icon: node.icon,
          perms: node.perms
        }
      })
    }
  }

  walk(menus, '')
  routes.forEach((route) => router.addRoute(route))
  return routes
}

export default router








