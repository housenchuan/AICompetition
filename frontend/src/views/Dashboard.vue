<template>
  <div>
    <div class="hero">
      <div class="hero-left">
        <div class="hero-title">{{ greeting }}，管理员</div>
        <div class="hero-desc">欢迎回到 AI 智能核保系统，以下是数据概况。</div>
      </div>
      <div class="hero-date">{{ today }}</div>
    </div>

    <div class="page-card">
      <div class="page-title">功能入口</div>
      <el-row :gutter="16" class="entry-grid">
        <el-col :xs="12" :sm="6" v-for="s in cards" :key="s.key">
          <div class="entry" @click="$router.push(s.to)">
            <div class="entry-icon" :style="{ background: s.light, color: s.main }" v-html="s.icon"></div>
            <div class="entry-text">{{ s.label }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <div class="page-title">数据概览</div>
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
      <div ref="chartEl" class="overview-chart"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { customerRiskApi, applicationApi, decisionApi } from '../api'
import { icons } from '../icons'

const chartEl = ref(null)
let chart = null

const cards = ref([
  { key: 'his', label: '历史客户画像', value: '—', icon: icons.users, main: '#FA541C', light: '#FFECE2', to: '/history' },
  { key: 'app', label: '投保申请记录', value: '—', icon: icons.doc, main: '#16A34A', light: '#E7F6EC', to: '/applications' },
  { key: 'dec', label: '核保决策结果', value: '—', icon: icons.check, main: '#D48806', light: '#FCF3E2', to: '/decisions' },
  { key: 'rule', label: '风险计分维度', value: 8, icon: icons.rules, main: '#1677FF', light: '#E8F1FF', to: '/rules' }
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
  await nextTick()
  renderChart()
})

function renderChart() {
  if (!chartEl.value) return
  if (chart && chart.getDom() !== chartEl.value) { chart.dispose(); chart = null }
  if (!chart) chart = echarts.init(chartEl.value)
  const data = cards.value.map(s => ({
    value: typeof s.value === 'number' ? s.value : 0,
    itemStyle: { color: s.main, borderRadius: [6, 6, 0, 0] }
  }))
  chart.setOption({
    grid: { top: 24, right: 20, bottom: 28, left: 44 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: cards.value.map(s => s.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { color: '#8a8f99' }
    },
    series: [{
      type: 'bar',
      data,
      barWidth: '42%',
      label: { show: true, position: 'top', color: '#1f2329', fontWeight: 600 }
    }]
  })
}

function onResize() { chart && chart.resize() }
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart && chart.dispose(); chart = null
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

.stat-row { margin-bottom: 20px; }
.stat-card {
  background: #fafbfc;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  padding: 16px 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06); border-color: var(--brand); }
.stat-icon {
  width: 46px; height: 46px; border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-icon :deep(svg) { width: 24px; height: 24px; }
.stat-num { font-size: 26px; font-weight: 700; color: #1f2329; line-height: 1.1; }
.stat-label { margin-top: 4px; color: #8a8f99; font-size: 13px; }

.overview-chart { width: 100%; height: 300px; }

.entry {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 18px;
  border: 1px solid #eef0f3;
  border-radius: 12px;
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
