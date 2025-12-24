import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页', icon: 'HomeFilled' }
        },
        {
          path: 'api-market',
          name: 'ApiMarket',
          component: () => import('@/views/api-market/ApiMarketView.vue'),
          meta: { title: '接口市场', icon: 'Shop' }
        },
        {
          path: 'api-list',
          name: 'ApiList',
          component: () => import('@/views/api-list/ApiListView.vue'),
          meta: { title: '我的接口', icon: 'Connection' }
        },
        {
          path: 'my-statistics',
          name: 'MyStatistics',
          component: () => import('@/views/my-statistics/MyStatisticsView.vue'),
          meta: { title: '我的调用统计', icon: 'DataLine' }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { title: '个人中心', icon: 'UserFilled' }
        },
        {
          path: 'sdk-guide',
          name: 'SdkGuide',
          component: () => import('@/views/sdk-guide/SdkGuideView.vue'),
          meta: { title: 'SDK使用指南', icon: 'Document' }
        },
        // 管理员专用页面
        {
          path: 'api-manage',
          name: 'ApiManage',
          component: () => import('@/views/api-manage/ApiListView.vue'),
          meta: { title: 'API管理', icon: 'Setting', requiresAdmin: true }
        },
        {
          path: 'user-manage',
          name: 'UserManage',
          component: () => import('@/views/user/UserListView.vue'),
          meta: { title: '用户管理', icon: 'User', requiresAdmin: true }
        },
        {
          path: 'global-statistics',
          name: 'GlobalStatistics',
          component: () => import('@/views/statistics/StatisticsView.vue'),
          meta: { title: '全局统计', icon: 'TrendCharts', requiresAdmin: true }
        },
        {
          path: 'operate-log',
          name: 'OperateLog',
          component: () => import('@/views/log/OperateLogView.vue'),
          meta: { title: '操作日志', icon: 'Document', requiresAdmin: true }
        },
        {
          path: 'api-permission',
          name: 'ApiPermission',
          component: () => import('@/views/api-permission/ApiPermissionView.vue'),
          meta: { title: '接口权限管理', icon: 'Key', requiresAdmin: true }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || 'API平台'} - API平台`

  const token = getToken()
  const userStore = useUserStore()

  // 如果有 token 但没有用户信息，尝试获取用户信息
  if (token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      // 获取用户信息失败，清除 token，跳转登录页
      userStore.logout()
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 需要登录但没有 token
  if (to.meta.requiresAuth !== false && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录访问登录页，跳转首页
  if (to.path === '/login' && token) {
    next({ path: '/' })
    return
  }

  // 管理员路由校验
  if (to.meta.requiresAdmin) {
    const isAdmin = userStore.userInfo?.permissionType === 3
    if (!isAdmin) {
      next({ path: '/dashboard' })
      return
    }
  }

  next()
})

export default router

