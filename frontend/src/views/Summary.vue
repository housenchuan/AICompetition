<template>
  <div v-loading="loading">
    <div class="page-title">数据汇总统计</div>

    <!-- 概览指标 -->
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

    <!-- 统计构建器 -->
    <div class="filter-bar">
      <el-form :model="form" label-width="86px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="6" :xl="5">
            <el-form-item label="数据源">
              <el-select v-model="form.entity" style="width:100%" @change="onEntityChange">
                <el-option v-for="e in ENTITIES" :key="e.value" :label="e.label" :value="e.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="10" :xl="8">
            <el-form-item label="分组维度">
              <el-select v-model="form.groupBy" multiple :multiple-limit="4" collapse-tags collapse-tags-tooltip
                placeholder="选择 1~4 个维度（单维出图+表，多维出交叉/组合表）" style="width:100%">
                <el-option v-for="d in dims" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="创建时间">
              <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="filter-actions">
          <el-button type="primary" @click="runStat">统计</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </div>

    <!-- 单维：图 + 表 -->
    <el-row v-if="result && result.rows" :gutter="16">
      <el-col :span="14">
        <div class="page-card">
          <div class="card-hd">
            <span class="sec-title">{{ dimLabel(result.dims[0]) }}分布</span>
            <el-radio-group v-model="chartType" size="small" @change="renderChart">
              <el-radio-button value="bar">柱状图</el-radio-button>
              <el-radio-button value="pie">饼图</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="result.rows.length" ref="chartEl" class="chart"></div>
          <div v-else class="empty">该时间范围内暂无数据</div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <div class="sec-title">明细（共 {{ result.total }} 条）</div>
          <el-table :data="result.rows" border stripe size="small" max-height="360">
            <el-table-column type="index" label="序号" width="56" />
            <el-table-column prop="key" :label="dimLabel(result.dims[0])" />
            <el-table-column prop="count" label="数量" width="80" />
            <el-table-column label="占比" width="90">
              <template #default="{ row }">{{ row.percent }}%</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 双维：透视表 -->
    <div v-else-if="result && result.pivot" class="page-card">
      <div class="sec-title">{{ dimLabel(result.dims[0]) }} × {{ dimLabel(result.dims[1]) }} 交叉统计（共 {{ result.pivot.total }} 条）</div>
      <el-table :data="pivotRows" border stripe size="small" max-height="480" show-summary :summary-method="pivotSummary">
        <el-table-column prop="__row" :label="dimLabel(result.dims[0])" fixed="left" width="130" />
        <el-table-column v-for="(c, i) in result.pivot.colKeys" :key="c" :prop="'c' + i" :label="c" min-width="90" align="center" />
        <el-table-column prop="__total" label="合计" width="80" align="center" fixed="right" />
      </el-table>
    </div>

    <!-- 三/四维：扁平组合表 -->
    <div v-else-if="result && result.groups" class="page-card">
      <div class="sec-title">{{ result.dims.map(dimLabel).join(' × ') }} 组合统计（共 {{ result.total }} 条 · {{ result.groups.length }} 种组合）</div>
      <el-table :data="result.groups" border stripe size="small">
        <el-table-column type="index" label="序号" width="56" />
        <el-table-column v-for="(d, i) in result.dims" :key="d" :label="dimLabel(d)" min-width="110">
          <template #default="{ row }">{{ row.keys[i] }}</template>
        </el-table-column>
        <el-table-column prop="count" label="数量" width="80" align="center" />
        <el-table-column label="占比" width="90" align="center">
          <template #default="{ row }">{{ row.percent }}%</template>
        </el-table-column>
      </el-table>
    </div>

    <div v-else class="page-card empty-hint">请选择数据源与维度后点击「统计」</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statApi } from '../api'
import { icons } from '../icons'

const loading = ref(false)
const dateRange = ref([])
const tiles = ref([])
const result = ref(null)
const chartType = ref('bar')
const chartEl = ref(null)
let chart = null


const ENTITIES = [
  { label: '投保申请记录', value: 'policy_applications' },
  { label: '历史客户画像', value: 'customer_risk_his' },
  { label: '核保决策结果', value: 'underwriting_decisions' }
]

const DIMS = {
  policy_applications: [
    { label: '产品类型', value: 'productType' }, { label: '申请状态', value: 'status' },
    { label: '缴费频率', value: 'paymentFrequency' }, { label: '创建人', value: 'createdBy' },
    { label: '投保人编号', value: 'customerId' }, { label: '创建年月', value: 'month' }
  ],
  customer_risk_his: [
    { label: '性别', value: 'gender' }, { label: '职业', value: 'occupation' },
    { label: '吸烟', value: 'smokingStatus' }, { label: '饮酒', value: 'drinkingStatus' },
    { label: '社保', value: 'hasSocialInsurance' }, { label: '是否理赔', value: 'target' },
    { label: '投保人编号', value: 'customerId' }, { label: '创建年月', value: 'month' }
  ],
  underwriting_decisions: [
    { label: '风险等级', value: 'riskLevel' }, { label: '核保结论', value: 'underwritingResult' },
    { label: '性别', value: 'gender' }, { label: '职业', value: 'occupation' },
    { label: '吸烟', value: 'smokingStatus' }, { label: '饮酒', value: 'drinkingStatus' },
    { label: '社保', value: 'hasSocialInsurance' }, { label: '投保人编号', value: 'customerId' },
    { label: '创建年月', value: 'month' }
  ]
}

const form = reactive({ entity: 'policy_applications', groupBy: ['productType'] })
const dims = computed(() => DIMS[form.entity] || [])

function dimLabel(v) {
  for (const list of Object.values(DIMS)) {
    const f = list.find(d => d.value === v)
    if (f) return f.label
  }
  return v
}

function onEntityChange() {
  form.groupBy = dims.value[0] ? [dims.value[0].value] : []
}

// 透视表行数据
const pivotRows = computed(() => {
  if (!result.value?.pivot) return []
  const p = result.value.pivot
  return p.rowKeys.map((rk, ri) => {
    const row = { __row: rk, __total: p.rowTotals[ri] }
    p.colKeys.forEach((c, ci) => { row['c' + ci] = p.matrix[ri][ci] })
    return row
  })
})
function pivotSummary() {
  const p = result.value.pivot
  const sums = ['合计']
  p.colKeys.forEach((c, ci) => sums.push(p.colTotals[ci]))
  sums.push(p.total)
  return sums
}

async function runStat() {
  if (!form.groupBy?.length) return
  loading.value = true
  try {
    const body = { entity: form.entity, groupBy: form.groupBy, dateFrom: dateRange.value?.[0], dateTo: dateRange.value?.[1] }
    result.value = (await statApi.aggregate(body)).data
    if (result.value.rows) renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!result.value?.rows?.length) return
  nextTick(() => {
    if (!chartEl.value) return
    // 容器可能因单维/多维切换被 v-if 重建，实例绑到了旧节点则先销毁重建
    if (chart && chart.getDom() !== chartEl.value) { chart.dispose(); chart = null }
    if (!chart) chart = echarts.init(chartEl.value)
    const rows = result.value.rows
    const palette = ['#FA541C', '#16A34A', '#1677FF', '#D48806', '#722ED1', '#13C2C2', '#EB2F96']
    const option = chartType.value === 'pie'
      ? {
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, type: 'scroll' },
          color: palette,
          series: [{ type: 'pie', radius: ['40%', '68%'], center: ['50%', '46%'],
            data: rows.map(r => ({ name: r.key, value: r.count })),
            label: { formatter: '{b}\n{c}' } }]
        }
      : {
          tooltip: { trigger: 'axis' },
          grid: { left: 10, right: 20, bottom: 10, top: 20, containLabel: true },
          xAxis: { type: 'category', data: rows.map(r => r.key), axisLabel: { interval: 0, rotate: rows.length > 6 ? 30 : 0 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{ type: 'bar', data: rows.map(r => r.count), barMaxWidth: 46,
            itemStyle: { color: '#FA541C', borderRadius: [4, 4, 0, 0] }, label: { show: true, position: 'top' } }]
        }
    chart.setOption(option, true)
    chart.resize()
  })
}

function reset() {
  dateRange.value = []
  form.entity = 'policy_applications'
  form.groupBy = []
  result.value = null
}

async function loadTiles() {
  try {
    const d = (await statApi.overview({})).data
    tiles.value = [
      { label: '投保申请总数', value: d.applicationTotal, icon: icons.doc, main: '#FA541C', light: '#FFECE2' },
      { label: '核保通过率', value: d.passRate, suffix: '%', icon: icons.check, main: '#16A34A', light: '#E7F6EC' },
      { label: '已预测决策', value: d.predictedCount, icon: icons.chart, main: '#1677FF', light: '#E8F1FF' },
      { label: '平均风险评分', value: d.avgRiskScore, icon: icons.rules, main: '#D48806', light: '#FCF3E2' }
    ]
  } catch (e) { /* 后端未启动时留空 */ }
}

window.addEventListener('resize', () => chart && chart.resize())

onMounted(async () => { await loadTiles(); runStat() })
</script>

<style scoped>
.tiles { margin-bottom: 16px; }
.tile {
  background: #fff; border-radius: 12px; padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  display: flex; align-items: center; gap: 14px;
}
.tile-icon { width: 46px; height: 46px; border-radius: 11px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.tile-icon :deep(svg) { width: 24px; height: 24px; }
.tile-num { font-size: 26px; font-weight: 700; color: #1f2329; line-height: 1.1; }
.tile-num .suffix { font-size: 14px; font-weight: 500; margin-left: 2px; }
.tile-label { margin-top: 4px; color: #8a8f99; font-size: 13px; }

.card-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.sec-title { font-size: 15px; font-weight: 600; }
.chart { height: 340px; width: 100%; }
.empty { color: #a0a4ab; font-size: 13px; text-align: center; padding: 40px 0; }
.empty-hint { color: #a0a4ab; font-size: 13px; text-align: center; padding: 40px 0; }
.filter-bar :deep(.el-form-item__label) { white-space: nowrap; }
</style>
