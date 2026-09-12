<template>
  <div>
    <div class="hero">
      <div class="hero-left">
        <div class="hero-title">{{ greeting }}，管理员</div>
        <div class="hero-desc">欢迎回到 AI 智能核保系统，以下是数据概况。</div>
      </div>
      <div class="hero-date">{{ today }}</div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="s in cards" :key="s.key">
        <div class="stat-card" @click="$router.push(s.to)">
          <div class="stat-icon" :style="{ background: s.light, color: s.main }" v-html="s.icon"></div>
          <div class="stat-meta">
            <div class="stat-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card">
      <div class="page-title">功能入口</div>
      <div class="entry-grid">
        <div class="entry" v-for="s in cards" :key="s.key" @click="$router.push(s.to)">
          <div class="entry-icon" :style="{ background: s.light, color: s.main }" v-html="s.icon"></div>
          <div class="entry-text">{{ s.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { customerRiskApi, applicationApi, decisionApi } from '../api'
import { icons } from '../icons'

const cards = ref([
  { key: 'his', label: '历史客户画像', value: '—', icon: icons.users, main: '#FA541C', light: '#FFECE2', to: '/history' },
  { key: 'app', label: '投保申请记录', value: '—', icon: icons.doc, main: '#16A34A', light: '#E7F6EC', to: '/applications' },
  { key: 'dec', label: '核保决策结果', value: '—', icon: icons.check, main: '#D48806', light: '#FCF3E2', to: '/decisions' },
  { key: 'rule', label: '风险计分维度', value: '8', icon: icons.rules, main: '#1677FF', light: '#E8F1FF', to: '/rules' }
])

const greeting = (() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})()

const today = (() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})()

onMounted(async () => {
  try {
    const [a, b, c] = await Promise.all([
      customerRiskApi.page({ pageNum: 1, pageSize: 1 }),
      applicationApi.page({ pageNum: 1, pageSize: 1 }),
      decisionApi.page({ pageNum: 1, pageSize: 1 })
    ])
    cards.value[0].value = a.data.total
    cards.value[1].value = b.data.total
    cards.value[2].value = c.data.total
  } catch (e) {
    // 后端未启动时保持占位
  }
})
</script>

<style scoped>
.hero {
  background: linear-gradient(100deg, var(--brand-dark), var(--brand) 55%, var(--brand-light));
  color: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hero-title { font-size: 22px; font-weight: 600; }
.hero-desc { margin-top: 8px; font-size: 13px; color: rgba(255, 255, 255, 0.9); }
.hero-date {
  font-size: 13px;
  background: rgba(255, 255, 255, 0.2);
  padding: 7px 16px;
  border-radius: 16px;
  white-space: nowrap;
}

.stat-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: transform 0.15s, box-shadow 0.15s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08); }
.stat-icon {
  width: 46px; height: 46px; border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-icon :deep(svg) { width: 24px; height: 24px; }
.stat-num { font-size: 26px; font-weight: 700; color: #1f2329; line-height: 1.1; }
.stat-label { margin-top: 4px; color: #8a8f99; font-size: 13px; }

.entry-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.entry {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 18px;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.entry:hover { border-color: var(--brand); background: var(--el-color-primary-light-9); }
.entry-icon {
  width: 34px; height: 34px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
}
.entry-icon :deep(svg) { width: 19px; height: 19px; }
.entry-text { font-size: 14px; color: #1f2329; }
</style>
