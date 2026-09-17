import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import { canAccess } from '../roles'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', meta: { title: '首页' }, component: () => import('../views/Dashboard.vue') },
      { path: 'assistant', name: 'assistant', meta: { title: '智能助手' }, component: () => import('../views/Assistant.vue') },
      { path: 'history', name: 'history', meta: { title: '历史客户画像' }, component: () => import('../views/HistoryList.vue') },
      { path: 'applications', name: 'applications', meta: { title: '投保申请记录' }, component: () => import('../views/ApplicationList.vue') },
      { path: 'decisions', name: 'decisions', meta: { title: '核保决策结果' }, component: () => import('../views/DecisionList.vue') },
      { path: 'summary', name: 'summary', meta: { title: '数据汇总统计' }, component: () => import('../views/Summary.vue') },
      { path: 'rules', name: 'rules', meta: { title: '风险计分规则' }, component: () => import('../views/RuleView.vue') },
      { path: 'feedback', name: 'feedback', meta: { title: '用户反馈' }, component: () => import('../views/FeedbackView.vue') }
    ]
  },
  // 兜底：未知路径（含已下线的 /decisions）重定向到首页
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 页面级权限拦截：当前角色无权访问的页面重定向到首页
router.beforeEach((to) => {
  if (to.path === '/home' || to.path === '/') return true
  if (!canAccess(to.path)) return '/home'
  return true
})

export default router
