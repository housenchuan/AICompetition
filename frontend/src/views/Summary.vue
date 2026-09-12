<template>
  <div v-loading="loading">
    <div class="page-title">数据汇总统计</div>

    <div class="filter-bar">
      <el-form :model="{}" label-width="76px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="申请日期">
              <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="filter-actions">
          <el-button type="primary" @click="load">统计</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </div>

    <el-row :gutter="16" class="tiles">
      <el-col :xs="12" :sm="6" v-for="t in tiles" :key="t.label">
        <div class="tile">
          <div class="tile-icon" :style="{ background: t.light, color: t.main }" v-html="t.icon"></div>
          <div>
            <div class="tile-num">{{ t.value }}<span v-if="t.suffix" class="suffix">{{ t.suffix }}</span></div>
            <div class="tile-label">{{ t.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8">
        <div class="page-card">
          <div class="sec-title">核保状态分布</div>
          <Bars :data="data.statusDist" color="#FA541C" empty="暂无申请数据" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card">
          <div class="sec-title">产品类型分布</div>
          <Bars :data="data.productDist" color="#16A34A" empty="暂无申请数据" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card">
          <div class="sec-title">风险等级分布（已预测）</div>
          <Bars :data="data.riskLevelDist" color="#1677FF" empty="暂无预测结果，请先在核保决策页发起预测" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { statApi } from '../api'
import { icons } from '../icons'

const loading = ref(false)
const dateRange = ref([])
const data = reactive({ statusDist: [], productDist: [], riskLevelDist: [] })
const tiles = ref([])

// 轻量水平条形图组件
const Bars = {
  props: { data: Array, color: String, empty: String },
  setup(props) {
    return () => {
      const list = props.data || []
      if (!list.length) return h('div', { class: 'bars-empty' }, props.empty || '暂无数据')
      const max = Math.max(...list.map(d => d.value), 1)
      return h('div', { class: 'bars' }, list.map(d =>
        h('div', { class: 'bar-row' }, [
          h('span', { class: 'bar-name' }, d.name),
          h('div', { class: 'bar-track' }, [
            h('div', { class: 'bar-fill', style: { width: (d.value / max * 100) + '%', background: props.color } })
          ]),
          h('span', { class: 'bar-val' }, d.value)
        ])
      ))
    }
  }
}

async function load() {
  loading.value = true
  try {
    const body = { dateFrom: dateRange.value?.[0], dateTo: dateRange.value?.[1] }
    const res = await statApi.overview(body)
    const d = res.data
    data.statusDist = d.statusDist || []
    data.productDist = d.productDist || []
    data.riskLevelDist = d.riskLevelDist || []
    tiles.value = [
      { label: '投保申请总数', value: d.applicationTotal, icon: icons.doc, main: '#FA541C', light: '#FFECE2' },
      { label: '核保通过率', value: d.passRate, suffix: '%', icon: icons.check, main: '#16A34A', light: '#E7F6EC' },
      { label: '已预测决策', value: d.predictedCount, icon: icons.chart, main: '#1677FF', light: '#E8F1FF' },
      { label: '平均风险评分', value: d.avgRiskScore, icon: icons.rules, main: '#D48806', light: '#FCF3E2' }
    ]
  } finally {
    loading.value = false
  }
}

function reset() { dateRange.value = []; load() }

onMounted(load)
</script>

<style scoped>
.tiles { margin-bottom: 16px; }
.tile {
  background: #fff; border-radius: 12px; padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  display: flex; align-items: center; gap: 14px;
}
.tile-icon {
  width: 46px; height: 46px; border-radius: 11px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.tile-icon :deep(svg) { width: 24px; height: 24px; }
.tile-num { font-size: 26px; font-weight: 700; color: #1f2329; line-height: 1.1; }
.tile-num .suffix { font-size: 14px; font-weight: 500; margin-left: 2px; }
.tile-label { margin-top: 4px; color: #8a8f99; font-size: 13px; }

.sec-title { font-size: 15px; font-weight: 600; margin-bottom: 16px; }
:deep(.bars) { display: flex; flex-direction: column; gap: 14px; }
:deep(.bar-row) { display: flex; align-items: center; gap: 10px; }
:deep(.bar-name) { width: 96px; font-size: 13px; color: #4a4f57; flex-shrink: 0; text-align: right; }
:deep(.bar-track) { flex: 1; height: 16px; background: #f2f3f5; border-radius: 8px; overflow: hidden; }
:deep(.bar-fill) { height: 100%; border-radius: 8px; transition: width 0.4s; min-width: 2px; }
:deep(.bar-val) { width: 34px; font-size: 13px; color: #1f2329; font-weight: 500; }
:deep(.bars-empty) { color: #a0a4ab; font-size: 13px; padding: 20px 0; text-align: center; }
</style>
