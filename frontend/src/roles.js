// ============================================================
//  角色与页面/数据权限配置（集中维护，一处修改全局生效）
//  说明：本系统为竞赛演示，无登录体系，角色通过右上角切换并持久化到
//        localStorage。菜单按角色白名单过滤，路由守卫做页面级拦截。
// ============================================================
import { ref } from 'vue'

// 全部菜单（顺序即侧边栏展示顺序）；icon 为 icons.js 中的键
export const MENUS = [
  { path: '/home',         title: '首页概览',     icon: 'home' },
  { path: '/assistant',    title: '智能助手',     icon: 'chat' },
  { path: '/history',      title: '历史客户画像', icon: 'users' },
  { path: '/applications', title: '投保申请记录', icon: 'doc' },
  { path: '/decisions',    title: '核保决策结果', icon: 'check' },
  { path: '/summary',      title: '数据汇总统计', icon: 'chart' },
  { path: '/rules',        title: '风险计分规则', icon: 'rules' },
  { path: '/feedback',     title: '用户反馈',     icon: 'bulb' }
]

const ALL = MENUS.map(m => m.path)

// 各角色可见菜单白名单（页面/数据权限）
export const ROLES = {
  // 管理员：全部页面
  admin: {
    key: 'admin', name: '管理员', avatar: '管',
    menus: [...ALL]
  },
  // 核保专员：除「用户反馈、历史客户画像、数据汇总统计」外的 5 个菜单
  underwriter: {
    key: 'underwriter', name: '核保专员', avatar: '员',
    menus: ['/home', '/assistant', '/applications', '/decisions', '/rules']
  },
  // 核保主管：除「用户反馈、历史客户画像」外的 6 个菜单
  supervisor: {
    key: 'supervisor', name: '核保主管', avatar: '主',
    menus: ['/home', '/assistant', '/applications', '/decisions', '/summary', '/rules']
  }
}

export const ROLE_LIST = [ROLES.admin, ROLES.underwriter, ROLES.supervisor]
export const DEFAULT_ROLE = 'admin'
const STORAGE_KEY = 'app_role'

function loadRole() {
  try {
    const r = localStorage.getItem(STORAGE_KEY)
    if (r && ROLES[r]) return r
  } catch (e) { /* localStorage 不可用时回退默认 */ }
  return DEFAULT_ROLE
}

// 当前角色（响应式，全局共享）
export const currentRoleKey = ref(loadRole())

export function setRole(key) {
  if (!ROLES[key]) return
  currentRoleKey.value = key
  try { localStorage.setItem(STORAGE_KEY, key) } catch (e) { /* 忽略 */ }
}

export function currentRole() {
  return ROLES[currentRoleKey.value] || ROLES[DEFAULT_ROLE]
}

// 当前角色可见菜单路径集合
export function allowedPaths(key = currentRoleKey.value) {
  return (ROLES[key] || ROLES[DEFAULT_ROLE]).menus
}

// 判断某路径当前角色是否有权访问
export function canAccess(path, key = currentRoleKey.value) {
  return allowedPaths(key).includes(path)
}
