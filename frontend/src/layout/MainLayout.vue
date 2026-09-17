<template>
  <el-container class="layout">
    <el-header class="topnav">
      <div class="brand">
        <div class="brand-logo">核</div>
        <div class="brand-text">
          <div class="brand-name">AI 智能核保风险评估系统</div>
          <div class="brand-en">AI Underwriting Risk Platform</div>
        </div>
      </div>
      <div class="topnav-right">
        <span class="status"><i class="dot"></i>系统运行中</span>
        <div class="role-switch" v-click-outside="closeRoleMenu">
          <div class="user" @click="roleMenuOpen = !roleMenuOpen">
            <div class="avatar">{{ role.avatar }}</div>
            <div class="user-meta">
              <div class="user-name">{{ role.name }}</div>
              <div class="user-role">点击切换角色</div>
            </div>
            <svg class="caret" :class="{ open: roleMenuOpen }" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
          </div>
          <div v-if="roleMenuOpen" class="role-menu">
            <div v-for="r in roleList" :key="r.key" class="role-item"
              :class="{ current: r.key === role.key }" @click="onSwitchRole(r.key)">
              <span class="tick">{{ r.key === role.key ? '✓' : '' }}</span>{{ r.name }}
            </div>
          </div>
        </div>
      </div>
    </el-header>

    <el-container class="body">
      <el-aside width="188px" class="aside">
        <div class="nav-label">导航菜单</div>
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item v-for="m in visibleMenus" :key="m.path" :index="m.path">
            <span class="mi" v-html="icons[m.icon]"></span><span>{{ m.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 全局灯泡浮动按钮 -->
    <div class="fab-bulb" title="提交反馈" @click="feedbackVisible = true">
      <span v-html="icons.bulb"></span>
    </div>

    <!-- 全局反馈弹框 -->
    <FeedbackDialog v-model="feedbackVisible" />
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ClickOutside as vClickOutside } from 'element-plus'
import { icons } from '../icons'
import FeedbackDialog from '../components/FeedbackDialog.vue'
import { MENUS, ROLE_LIST, currentRole, setRole, canAccess } from '../roles'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const feedbackVisible = ref(false)

// 角色与按角色过滤后的可见菜单
const role = computed(() => currentRole())
const roleList = ROLE_LIST
const roleMenuOpen = ref(false)
const visibleMenus = computed(() => MENUS.filter(m => role.value.menus.includes(m.path)))

function closeRoleMenu() { roleMenuOpen.value = false }

function onSwitchRole(key) {
  setRole(key)
  roleMenuOpen.value = false
  // 切换后若当前页面对新角色不可见，回到首页
  if (!canAccess(route.path)) router.replace('/home')
}
</script>

<style scoped>
.layout { height: 100vh; }

.topnav {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #eef0f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
}
.brand { display: flex; align-items: center; gap: 12px; }
.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: linear-gradient(135deg, var(--brand), var(--brand-light));
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-name { font-size: 16px; font-weight: 600; color: #1f2329; line-height: 1.2; }
.brand-en { font-size: 11px; color: #a0a4ab; letter-spacing: 0.5px; }

.topnav-right { display: flex; align-items: center; gap: 20px; }
.status { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #52c41a; }
.status .dot { width: 7px; height: 7px; border-radius: 50%; background: #52c41a; display: inline-block; }

.role-switch { position: relative; }
.user { display: flex; align-items: center; gap: 9px; cursor: pointer; user-select: none; }
.caret { color: #a0a4ab; margin-left: 2px; transition: transform 0.2s, color 0.15s; }
.caret.open { transform: rotate(180deg); }
.user:hover .caret { color: var(--brand); }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--el-color-primary-light-9); color: var(--brand);
  font-weight: 600; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
}
.user-name { font-size: 13px; color: #1f2329; font-weight: 500; line-height: 1.2; }
.user-role { font-size: 11px; color: #a0a4ab; }

.role-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 140px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  padding: 6px;
  z-index: 1000;
}
.role-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #4a4f57;
  cursor: pointer;
}
.role-item:hover { background: #f6f7f9; }
.role-item.current { color: var(--brand); font-weight: 600; }
.tick { width: 12px; display: inline-block; color: var(--brand); }

.body { height: calc(100vh - 60px); }
.aside {
  background: #fff;
  border-right: 1px solid #eef0f3;
  padding-top: 8px;
}
.nav-label {
  font-size: 11px;
  color: #a0a4ab;
  padding: 8px 20px 4px;
  letter-spacing: 1px;
}
.menu { border-right: none; }
.menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  font-size: 13px;
  margin: 2px 10px;
  border-radius: 8px;
  color: #4a4f57;
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9);
  color: var(--brand);
  font-weight: 500;
}
.menu :deep(.el-menu-item:hover) { background: #f6f7f9; }
.mi { display: inline-flex; align-items: center; margin-right: 8px; }
.mi :deep(svg) { width: 16px; height: 16px; }

.main { padding: 18px; background: var(--page-bg); overflow-y: auto; }

.fab-bulb {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand), var(--brand-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(250, 84, 28, 0.45);
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 999;
}
.fab-bulb:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(250, 84, 28, 0.55);
}
.fab-bulb :deep(svg) { width: 22px; height: 22px; }
</style>
