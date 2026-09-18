<template>
  <div class="chat">
    <div class="chat-body" ref="bodyRef">
      <!-- 空状态欢迎 -->
      <div v-if="!messages.length" class="welcome">
        <div class="welcome-logo">核</div>
        <div class="welcome-title">AI 智能核保助手</div>
        <div class="welcome-sub">
          您好！我是您的智能核保助手，可以帮您查询业务数据、预测核保风险、统计汇总运营情况。<br />
          试试下方的快捷指令，或直接输入你的问题。
        </div>
      </div>

      <!-- 对话消息 -->
      <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
        <div class="avatar" :class="m.role">{{ m.role === 'user' ? '我' : '核' }}</div>
        <div class="bubble" :class="m.role">
          <template v-if="m.role === 'user'">{{ m.text }}</template>
          <template v-else>
            <div v-if="m.loading" class="thinking">正在思考…</div>
            <div v-else-if="m.predicting" class="predicting">
              <div class="pred-label">
                批量预测进行中
                <span class="pred-id">{{ m.predicting.currentId }}</span>
              </div>
              <el-progress :percentage="m.predicting.total ? Math.round(m.predicting.current / m.predicting.total * 100) : 0" :stroke-width="8" />
              <div class="pred-stat">
                已完成 {{ m.predicting.current }} / {{ m.predicting.total }} 条
                <span v-if="m.predicting.current > 0"> · 预计剩余约 {{ etaText(m.predicting) }}</span>
              </div>
            </div>
            <template v-else>
              <div v-if="m.error" class="err">{{ m.error }}</div>
              <template v-else>
                <div class="intent-chips">
                  <el-tag size="small" :type="intentType(m.intent.intent)">{{ intentLabel(m.intent.intent) }}</el-tag>
                  <el-tag size="small" type="info">{{ entityLabel(m.intent.entity) }}</el-tag>
                  <el-tag size="small" v-if="m.intent.filters?.timeRange" type="warning">
                    {{ m.intent.filters.timeRange.start }} ~ {{ m.intent.filters.timeRange.end }}</el-tag>
                  <el-tag size="small" v-if="m.intent.filters?.status">{{ m.intent.filters.status }}</el-tag>
                  <el-tag size="small" v-if="m.intent.filters?.customerId">{{ m.intent.filters.customerId }}</el-tag>
                  <span class="src">· {{ m.intent.note }}</span>
                </div>

                <div v-if="m.agg" class="res">
                  <div class="mini-tiles">
                    <div class="mt"><b>{{ m.agg.applicationTotal }}</b><span>申请总数</span></div>
                    <div class="mt"><b>{{ m.agg.passRate }}%</b><span>通过率</span></div>
                    <div class="mt"><b>{{ m.agg.predictedCount }}</b><span>已预测</span></div>
                    <div class="mt"><b>{{ m.agg.avgRiskScore }}</b><span>平均分</span></div>
                  </div>
                  <div class="dist-grid">
                    <div><div class="dist-h">核保状态</div><Bars :data="m.agg.statusDist" color="#FA541C" /></div>
                    <div><div class="dist-h">产品类型</div><Bars :data="m.agg.productDist" color="#16A34A" /></div>
                    <div><div class="dist-h">风险等级</div><Bars :data="m.agg.riskLevelDist" color="#1677FF" /></div>
                  </div>
                </div>

                <div v-if="m.dist" class="res">
                  <div class="res-head">{{ m.dist.head }}</div>
                  <Bars :data="m.dist.bars" color="#FA541C" />
                  <el-table :data="m.dist.rows" border stripe size="small" max-height="280" style="margin-top:10px">
                    <el-table-column type="index" label="序号" width="56" />
                    <el-table-column prop="key" :label="m.dist.label" />
                    <el-table-column prop="count" label="数量" width="72" />
                    <el-table-column label="占比" width="80">
                      <template #default="{ row }">{{ row.percent }}%</template>
                    </el-table-column>
                  </el-table>
                </div>

                <div v-if="m.table" class="res">
                  <div class="res-head">{{ m.table.head }}</div>
                  <el-table :data="m.table.rows" border stripe size="small" max-height="320">
                    <el-table-column v-for="col in m.table.cols" :key="col.prop" :prop="col.prop"
                      :label="col.label" :formatter="col.fmt" :width="col.width" show-overflow-tooltip />
                  </el-table>
                </div>

                <!-- 自然语言统计（text-to-SQL）：展示 AI 生成的 SQL 与查询结果 -->
                <div v-if="m.statsRes" class="res">
                  <div class="res-head">
                    统计结果 · 共 {{ m.statsRes.rowCount }} 条
                    <el-tag size="small" type="success" style="margin-left: 8px">爱码 LLM 生成 SQL 并执行</el-tag>
                  </div>
                  <pre class="sql-code">{{ m.statsRes.sql }}</pre>
                  <el-table :data="m.statsRes.rows" border stripe size="small" max-height="320">
                    <el-table-column v-for="c in m.statsRes.columns" :key="c" :prop="c" :label="c"
                      min-width="110" show-overflow-tooltip />
                  </el-table>
                  <div v-if="m.statsRes.truncated" class="truncated">结果超过 200 条，仅展示前 200 条</div>
                </div>
              </template>
            </template>
          </template>
          <div class="time">{{ m.time }}</div>
        </div>
      </div>
    </div>

    <!-- 快捷指令面板（常驻，可收起） -->
    <div class="tpl-panel" v-if="panelOpen">
      <div class="tpl-head">
        <span class="tpl-title">快捷指令</span>
        <span class="tpl-toggle" @click="panelOpen = false">收起</span>
      </div>
      <div class="tpl-tabs">
        <span v-for="t in tabs" :key="t.key" class="tpl-tab" :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key">{{ t.label }}</span>
      </div>
      <div class="tpl-list">
        <div class="tpl-item" v-for="(c, i) in currentTab.items" :key="i" @click="quick(c)">
          <div class="tpl-cmd">{{ c }}</div>
          <div class="tpl-cnt">{{ usage[c] || 0 }} 次使用</div>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input">
      <el-input v-model="text" type="textarea" :rows="2" resize="none"
        placeholder="请输入你的问题，例如：帮我统计 2025 年 3 月到 6 月的核保通过率与风险分布"
        @keydown.enter.exact.prevent="send" />
      <div class="input-bar">
        <span class="hint">按 Enter 发送，Shift + Enter 换行</span>
        <div class="actions">
          <span class="lib" @click="panelOpen = !panelOpen"><span class="lib-ico" v-html="icons.rules"></span>指令库</span>
          <el-button text @click="clearAll" :disabled="!messages.length">清空</el-button>
          <el-button class="send" :loading="running" @click="send">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, h } from 'vue'
import { ElMessage } from 'element-plus'
import { nlApi, applicationApi, customerRiskApi, decisionApi, statApi, predictApi } from '../api'
import { icons } from '../icons'

const text = ref('')
const running = ref(false)
const messages = ref([])
const bodyRef = ref(null)
const activeTab = ref('query')
const panelOpen = ref(true)
const usage = reactive({})

const tabs = [
  { key: 'query', label: '查询', items: ['帮我查2025年6月的所有投保申请数据', '查已通过的投保申请', '查看所有核保决策结果'] },
  { key: 'predict', label: '预测', items: ['预测C002的核保决策', '请预测2025年6月所有客户的核保决定', '预测2024年6月的核保结果'] },
  { key: 'agg', label: '统计汇总', items: ['帮我统计2025年3月到6月的核保通过率与风险分布', '统计各产品类型的申请数量', '统计2024年申请列表中的险种分布情况'] }
]
const currentTab = computed(() => tabs.find(t => t.key === activeTab.value))

function intentLabel(x) { return { QUERY: '查询', PREDICT: '预测', AGGREGATE: '统计汇总', UNKNOWN: '未识别' }[x] || x }
function intentType(x) { return { QUERY: 'primary', PREDICT: 'warning', AGGREGATE: 'success', UNKNOWN: 'danger' }[x] || 'info' }
function entityLabel(x) { return { customer_risk_his: '历史画像', policy_applications: '投保申请', underwriting_decisions: '核保决策' }[x] || x }
function levelType(l) { return { '标准体': 'success', '次标体A级': 'warning', '次标体B级': 'warning', '高风险体': 'danger', '拒保体': 'danger' }[l] || '' }

const Bars = {
  props: { data: Array, color: String },
  setup(props) {
    return () => {
      const list = props.data || []
      if (!list.length) return h('div', { class: 'bars-empty' }, '暂无')
      const max = Math.max(...list.map(d => d.value), 1)
      return h('div', { class: 'bars' }, list.map(d => h('div', { class: 'bar-row' }, [
        h('span', { class: 'bar-name' }, d.name),
        h('div', { class: 'bar-track' }, [h('div', { class: 'bar-fill', style: { width: (d.value / max * 100) + '%', background: props.color } })]),
        h('span', { class: 'bar-val' }, d.value)
      ])))
    }
  }
}

function now() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}
function scrollBottom() {
  nextTick(() => { if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight })
}

function quick(c) { usage[c] = (usage[c] || 0) + 1; text.value = c; send() }
function clearAll() { messages.value = [] }

async function send() {
  const q = text.value.trim()
  if (!q) { ElMessage.warning('请输入指令'); return }
  messages.value.push({ role: 'user', text: q, time: now() })
  text.value = ''
  const am = reactive({ role: 'assistant', loading: true, time: now() })
  messages.value.push(am)
  scrollBottom()
  running.value = true
  try {
    const res = await nlApi.parse(q)
    const it = res.data
    am.intent = it
    if (it.intent === 'AGGREGATE') {
      await fillStats(am, q, it)
    } else if (it.intent === 'QUERY') {
      await fillQuery(am, it)
    } else if (it.intent === 'PREDICT') {
      await fillPredict(am, it)
    } else {
      am.error = '未能识别指令意图，请换种说法。'
    }
  } catch (e) {
    am.error = '处理失败：' + (e.message || '请稍后重试')
  } finally {
    am.loading = false
    running.value = false
    scrollBottom()
  }
}

// 列集与各列表页保持一致
const socialFmt = (row) => (row.hasSocialInsurance ? '是' : '否')
const targetFmt = (row) => (row.target === 1 ? '有理赔' : row.target === 0 ? '无理赔' : '')
// 列集与各列表页展示字段保持一致
const COLS = {
  customer_risk_his: [
    { prop: 'profileId', label: '画像唯一标识' }, { prop: 'customerId', label: '投保人编号' },
    { prop: 'age', label: '年龄' }, { prop: 'gender', label: '性别' }, { prop: 'occupation', label: '职业类别' },
    { prop: 'annualIncome', label: '年收入(元)' }, { prop: 'hasSocialInsurance', label: '社保', fmt: socialFmt },
    { prop: 'smokingStatus', label: '吸烟' }, { prop: 'drinkingStatus', label: '饮酒' },
    { prop: 'bmi', label: 'BMI' }, { prop: 'bloodPressure', label: '血压' },
    { prop: 'target', label: '是否理赔', fmt: targetFmt }, { prop: 'scoreV1', label: '首版评分' },
    { prop: 'createdAt', label: '创建时间' }, { prop: 'updatedAt', label: '更新时间' }
  ],
  policy_applications: [
    { prop: 'profileId', label: '投保申请人唯一标识' }, { prop: 'customerId', label: '投保人编号' },
    { prop: 'productType', label: '产品类型' }, { prop: 'productName', label: '产品名称' },
    { prop: 'coverageAmount', label: '保额(元)' }, { prop: 'premium', label: '保费(元)' },
    { prop: 'paymentFrequency', label: '缴费频率' }, { prop: 'insurancePeriod', label: '保障期限' },
    { prop: 'status', label: '申请状态' }, { prop: 'applicationDate', label: '申请日期' },
    { prop: 'createdBy', label: '创建人' }, { prop: 'createdAt', label: '创建时间' }, { prop: 'updatedAt', label: '更新时间' }
  ],
  underwriting_decisions: [
    { prop: 'decisionId', label: '核保决策唯一标识' }, { prop: 'customerId', label: '投保人编号' },
    { prop: 'applicationId', label: '投保申请编号' }, { prop: 'age', label: '年龄' }, { prop: 'gender', label: '性别' },
    { prop: 'occupation', label: '职业类别' }, { prop: 'hasSocialInsurance', label: '是否有社保', fmt: socialFmt },
    { prop: 'smokingStatus', label: '吸烟状况' }, { prop: 'drinkingStatus', label: '饮酒状况' },
    { prop: 'riskScore', label: '风险评分' }, { prop: 'riskLevel', label: '风险等级' },
    { prop: 'underwritingResult', label: '核保结论' }, { prop: 'premiumAdjustment', label: '加费比例' },
    { prop: 'keyFactors', label: '关键风险因子' }, { prop: 'createdBy', label: '创建人' },
    { prop: 'createdAt', label: '创建时间' }, { prop: 'updatedAt', label: '更新时间' }
  ]
}

const DIM_LABEL = {
  productType: '产品类型', status: '申请状态', paymentFrequency: '缴费频率', createdBy: '创建人',
  riskLevel: '风险等级', underwritingResult: '核保结论', gender: '性别', occupation: '职业',
  smokingStatus: '吸烟', drinkingStatus: '饮酒', hasSocialInsurance: '社保', target: '是否理赔',
  customerId: '投保人编号', month: '年月'
}
const AGG_ENTITIES = ['policy_applications', 'customer_risk_his', 'underwriting_decisions']

// 自然语言统计：优先 text-to-SQL（表结构+问题传给爱码 LLM 生成 SELECT 并执行），失败回退结构化统计
async function fillStats(am, q, it) {
  try {
    const res = await nlApi.stats(q)
    am.statsRes = res.data
    if (!res.data.rows.length) {
      am.error = '统计查询无结果（当前条件下暂无数据）。'
    }
  } catch (e) {
    // text-to-SQL 失败（LLM 不可达/校验不通过等）→ 回退原结构化统计
    await fillAggregate(am, it)
  }
}

async function fillAggregate(am, it) {
  const r = it.filters?.timeRange || {}
  const groupBy = Array.isArray(it.groupBy) && it.groupBy.length ? it.groupBy : null
  if (!groupBy) {
    // 无明确维度：回退到整体概览
    am.agg = (await statApi.overview({ dateFrom: r.start, dateTo: r.end })).data
    return
  }
  const entity = AGG_ENTITIES.includes(it.entity) ? it.entity : 'policy_applications'
  const d = (await statApi.aggregate({ entity, groupBy, dateFrom: r.start, dateTo: r.end })).data
  if (d.rows) {
    am.dist = {
      head: `${DIM_LABEL[groupBy[0]] || groupBy[0]}分布 · 共 ${d.total} 条`,
      label: DIM_LABEL[groupBy[0]] || groupBy[0],
      bars: d.rows.map(x => ({ name: x.key, value: x.count })),
      rows: d.rows
    }
  } else {
    am.agg = (await statApi.overview({ dateFrom: r.start, dateTo: r.end })).data
  }
}

async function fillQuery(am, it) {
  const f = it.filters || {}
  const range = f.timeRange || {}
  const params = { pageNum: 1, pageSize: 100 }
  if (f.customerId) params.customerId = f.customerId
  if (f.status) params.status = f.status
  if (f.productType) params.productType = f.productType
  // 时间范围/单日 → 三类统一按创建时间(created_at)过滤
  if (range.start) params.createdFrom = range.start
  if (range.end) params.createdTo = range.end
  if (f.date) { params.createdFrom = f.date; params.createdTo = f.date }
  let res
  if (it.entity === 'customer_risk_his') res = await customerRiskApi.page(params)
  else if (it.entity === 'underwriting_decisions') res = await decisionApi.page(params)
  else res = await applicationApi.page(params)
  const entity = COLS[it.entity] ? it.entity : 'policy_applications'
  am.table = { head: `查询结果 · 共 ${res.data.total} 条`, rows: res.data.list, cols: COLS[entity] }
}

async function fillPredict(am, it) {
  const params = { pageNum: 1, pageSize: 500 }
  const f = it.filters || {}
  if (f.customerId) params.customerId = f.customerId
  // 时间范围/单日 → 按核保画像创建时间筛选（与查询/统计口径一致）
  const r = f.timeRange || {}
  if (r.start) params.createdFrom = r.start
  if (r.end) params.createdTo = r.end
  if (f.date) { params.createdFrom = f.date; params.createdTo = f.date }
  const listRes = await decisionApi.page(params)
  const ids = listRes.data.list.map(d => d.decisionId)
  if (!ids.length) { am.error = '未找到匹配的核保记录（该条件下暂无核保画像）。'; return }

  if (ids.length <= 1) {
    const rows = (await predictApi.batch(ids)).data
    am.table = { head: `预测结果 · 共 ${rows.length} 条`, rows, cols: COLS.underwriting_decisions }
    return
  }

  // 多条：逐条顺序调 single，实时更新进度
  am.loading = false
  am.predicting = { current: 0, total: ids.length, currentId: ids[0], startTime: Date.now() }
  await nextTick()  // 确保进度条先渲染出来再开始 loop
  scrollBottom()

  const results = [], failed = []
  for (const id of ids) {
    am.predicting.currentId = id
    try {
      const res = await predictApi.single(id)
      results.push(res.data)
    } catch {
      failed.push(id)
    }
    am.predicting.current++
    await nextTick()
    scrollBottom()
  }

  // 全部完成后让进度条停留 800ms 再切换到结果表，避免闪过
  am.predicting.current = ids.length
  await nextTick()
  await new Promise(r => setTimeout(r, 800))
  am.predicting = null
  const failNote = failed.length ? `（${failed.length} 条失败：${failed.slice(0, 3).join('、')}${failed.length > 3 ? '…' : ''}）` : ''
  am.table = { head: `预测结果 · 共 ${results.length} 条${failNote}`, rows: results, cols: COLS.underwriting_decisions }
}

function etaText(p) {
  if (!p.current) return '计算中…'
  const elapsed = Date.now() - p.startTime
  const perItem = elapsed / p.current
  const remaining = Math.round(perItem * (p.total - p.current) / 1000)
  if (remaining < 60) return `${remaining} 秒`
  const m = Math.floor(remaining / 60), s = remaining % 60
  return s > 0 ? `${m} 分 ${s} 秒` : `${m} 分钟`
}
</script>

<style scoped>
.chat { height: calc(100vh - 60px - 36px); display: flex; flex-direction: column; width: 100%; }
/* 滚动容器占满整宽，竖向滚动条落在最右侧；内部消息行居中限宽 */
.chat-body { flex: 1; overflow-y: auto; padding: 6px 4px 12px; }
.chat-body > .msg, .chat-body > .welcome { max-width: 820px; margin-left: auto; margin-right: auto; }

.welcome { max-width: 660px; margin: 30px auto; text-align: center; }
.welcome-logo {
  width: 58px; height: 58px; border-radius: 15px; margin: 0 auto 14px;
  background: linear-gradient(135deg, var(--brand), var(--brand-light));
  color: #fff; font-weight: 700; font-size: 23px; display: flex; align-items: center; justify-content: center;
}
.welcome-title { font-size: 20px; font-weight: 600; color: #1f2329; }
.welcome-sub { margin-top: 10px; font-size: 13px; color: #8a8f99; line-height: 1.8; }

.msg { display: flex; gap: 10px; margin-bottom: 18px; }
.msg.user { flex-direction: row-reverse; }
.avatar {
  width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600;
}
.avatar.assistant { background: linear-gradient(135deg, var(--brand), var(--brand-light)); color: #fff; }
.avatar.user { background: #e3e6ea; color: #5f6570; border-radius: 50%; }
.bubble { max-width: 82%; padding: 12px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
/* 含结果（表格/汇总图）的助手气泡适度加宽（比普通气泡宽，但不铺满整行） */
.bubble.assistant:has(.res) { max-width: 640px; }
.bubble.user { background: var(--brand); color: #fff; border-top-right-radius: 3px; }
.bubble.assistant { background: #fff; color: #1f2329; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06); border-top-left-radius: 3px; }
.time { font-size: 11px; color: #b7bcc4; margin-top: 8px; }
.bubble.user .time { color: rgba(255, 255, 255, 0.75); text-align: right; }
.thinking { color: #8a8f99; }
.err { color: var(--el-color-danger); }

.intent-chips { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.intent-chips .src { font-size: 11px; color: #b7bcc4; }
.res { margin-top: 12px; }
.res-head { font-size: 13px; color: #4a4f57; margin-bottom: 8px; }
.sql-code {
  background: #f6f8fa; border: 1px solid #e8eaee; border-radius: 8px;
  padding: 10px 12px; margin: 0 0 10px; font-size: 12px; line-height: 1.6;
  color: #383a42; white-space: pre-wrap; word-break: break-all; font-family: Consolas, Monaco, monospace;
}
.truncated { font-size: 12px; color: #b7bcc4; margin-top: 6px; }
.mini-tiles { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.mt { background: #f7f8fa; border-radius: 10px; padding: 10px 16px; min-width: 84px; }
.mt b { display: block; font-size: 20px; color: var(--brand); }
.mt span { font-size: 12px; color: #8a8f99; }
.dist-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.dist-h { font-size: 13px; font-weight: 500; margin-bottom: 10px; color: #4a4f57; }

.tpl-panel { background: #fff; border: 1px solid #eef0f3; border-radius: 12px; padding: 12px 16px; margin: 0 auto 10px; max-width: 820px; width: 100%; }
.tpl-head { display: flex; align-items: center; justify-content: space-between; }
.tpl-title { font-size: 14px; font-weight: 600; padding-left: 8px; border-left: 3px solid var(--brand); }
.tpl-toggle { font-size: 12px; color: #8a8f99; cursor: pointer; }
.tpl-toggle:hover { color: var(--brand); }
.tpl-tabs { display: flex; gap: 6px; margin: 10px 0 4px; }
.tpl-tab { font-size: 13px; color: #4a4f57; padding: 4px 14px; border-radius: 14px; cursor: pointer; }
.tpl-tab.active { background: var(--el-color-primary-light-9); color: var(--brand); font-weight: 500; }
.tpl-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-radius: 8px; cursor: pointer; }
.tpl-item:hover { background: #f6f7f9; }
.tpl-cmd { font-size: 13px; color: #1f2329; }
.tpl-cnt { font-size: 11px; color: #b7bcc4; }

.chat-input { border: 1px solid #eef0f3; border-radius: 12px; background: #fff; padding: 10px 12px; margin: 0 auto; max-width: 820px; width: 100%; }
.chat-input :deep(.el-textarea__inner) { box-shadow: none; padding: 0; font-size: 14px; }
.input-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.hint { font-size: 12px; color: #a0a4ab; }
.actions { display: flex; align-items: center; gap: 12px; }
.lib { display: inline-flex; align-items: center; gap: 5px; font-size: 13px; color: #4a4f57; cursor: pointer; }
.lib:hover { color: var(--brand); }
.lib-ico { display: inline-flex; }
.lib-ico :deep(svg) { width: 15px; height: 15px; }
.send { background: #2f88e0; border-color: #2f88e0; color: #fff; }
.send:hover { background: #1d78d4; border-color: #1d78d4; color: #fff; }

:deep(.bars) { display: flex; flex-direction: column; gap: 10px; }
:deep(.bar-row) { display: flex; align-items: center; gap: 8px; }
:deep(.bar-name) { width: 66px; font-size: 12px; color: #4a4f57; flex-shrink: 0; text-align: right; }
:deep(.bar-track) { flex: 1; height: 14px; background: #f2f3f5; border-radius: 7px; overflow: hidden; }
:deep(.bar-fill) { height: 100%; border-radius: 7px; min-width: 2px; }
:deep(.bar-val) { width: 26px; font-size: 12px; color: #1f2329; font-weight: 500; }
:deep(.bars-empty) { color: #b7bcc4; font-size: 12px; }

.predicting { min-width: 300px; }
.pred-label { font-size: 14px; font-weight: 500; color: #1f2329; margin-bottom: 10px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pred-id { font-size: 12px; color: #8a8f99; font-weight: 400; }
.pred-stat { margin-top: 8px; font-size: 12px; color: #8a8f99; }
</style>
