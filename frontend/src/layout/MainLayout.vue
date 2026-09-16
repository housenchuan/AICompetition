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
        <div class="user">
          <div class="avatar">管</div>
          <div class="user-meta">
            <div class="user-name">管理员</div>
            <div class="user-role">核保专员</div>
          </div>
        </div>
      </div>
    </el-header>

    <el-container class="body">
      <el-aside width="188px" class="aside">
        <div class="nav-label">导航菜单</div>
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item index="/home">
            <span class="mi" v-html="icons.home"></span><span>首页概览</span>
          </el-menu-item>
          <el-menu-item index="/assistant">
            <span class="mi" v-html="icons.chat"></span><span>智能助手</span>
          </el-menu-item>
          <el-menu-item index="/history">
            <span class="mi" v-html="icons.users"></span><span>历史客户画像</span>
          </el-menu-item>
          <el-menu-item index="/applications">
            <span class="mi" v-html="icons.doc"></span><span>投保申请记录</span>
          </el-menu-item>
          <el-menu-item index="/decisions">
            <span class="mi" v-html="icons.check"></span><span>核保决策结果</span>
          </el-menu-item>
          <el-menu-item index="/summary">
            <span class="mi" v-html="icons.chart"></span><span>数据汇总统计</span>
          </el-menu-item>
          <el-menu-item index="/rules">
            <span class="mi" v-html="icons.rules"></span><span>风险计分规则</span>
          </el-menu-item>
          <el-menu-item index="/feedback">
            <span class="mi" v-html="icons.bulb"></span><span>用户反馈</span>
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
import { useRoute } from 'vue-router'
import { icons } from '../icons'
import FeedbackDialog from '../components/FeedbackDialog.vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
const feedbackVisible = ref(false)
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
.user { display: flex; align-items: center; gap: 9px; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--el-color-primary-light-9); color: var(--brand);
  font-weight: 600; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
}
.user-name { font-size: 13px; color: #1f2329; font-weight: 500; line-height: 1.2; }
.user-role { font-size: 11px; color: #a0a4ab; }

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
