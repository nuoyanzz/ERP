import router from '@/router'
import { useUserStore } from '@/stores/user'

const WHITE_LIST = ['/login']

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  document.title = `${(to.meta?.title as string) || '工作台'} - ERP管理系统`

  if (userStore.token) {
    if (to.path === '/login') {
      next('/')
      return
    }
    if (!userStore.menusLoaded) {
      userStore
        .loadMenus()
        .then(() => next({ ...to, replace: true }))
        .catch(() => {
          userStore.resetToken()
          next('/login')
        })
      return
    }
    next()
    return
  }

  if (WHITE_LIST.includes(to.path)) {
    next()
    return
  }
  next('/login')
})








