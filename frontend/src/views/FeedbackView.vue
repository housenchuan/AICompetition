<template>
  <div>
    <div class="page-title">用户反馈渠道 <span class="subtitle">feedback_channel</span></div>
    <p class="page-desc">汇聚核保员 / 代理人 / 客户 / 内部质检的多渠道反馈，覆盖问题、建议、误判申诉、规则优化与投诉，支持全流程跟踪与满意度回访</p>

    <!-- 统计磁贴 -->
    <el-row :gutter="14" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">反馈工单总数</div>
          <div class="stat-value">{{ stats.total ?? '—' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card pending">
          <div class="stat-label">待处理</div>
          <div class="stat-value">{{ stats.pending ?? '—' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card resolved">
          <div class="stat-label">已解决 / 已关闭</div>
          <div class="stat-value">{{ stats.resolvedOrClosed ?? '—' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card satisfaction">
          <div class="stat-label">平均满意度</div>
          <div class="stat-value">{{ stats.avgSatisfaction ?? '—' }} <span class="stat-unit">/ 5</span></div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="14" class="chart-row">
      <!-- 反馈类型分布（环形图） -->
      <el-col :span="12">
        <div class="page-card chart-card">
          <div class="chart-title">● 反馈类型分布</div>
          <div class="donut-wrap">
            <svg width="160" height="160" viewBox="0 0 160 160" class="donut-svg">
              <g transform="rotate(-90 80 80)">
                <circle v-for="seg in donutSegments" :key="seg.type"
                  cx="80" cy="80" r="55" fill="none"
                  :stroke="seg.color" stroke-width="22"
                  :stroke-dasharray="`${seg.len} ${seg.rest}`"
                  :transform="`rotate(${seg.startAngle} 80 80)`"
                />
              </g>
              <text x="80" y="75" text-anchor="middle" font-size="22" font-weight="700" fill="#1f2329">{{ stats.total ?? 0 }}</text>
              <text x="80" y="93" text-anchor="middle" font-size="11" fill="#888">工单总数</text>
            </svg>
            <div class="donut-legend">
              <div v-for="seg in donutSegments" :key="seg.type" class="legend-item">
                <span class="legend-dot" :style="{ background: seg.color }"></span>
                <span class="legend-type">{{ seg.type }}：</span>
                <span class="legend-count">{{ seg.count }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 反馈状态分布（横向条形） -->
      <el-col :span="12">
        <div class="page-card chart-card">
          <div class="chart-title">● 反馈状态分布</div>
          <div class="status-bars">
            <div v-for="(item, idx) in statusBars" :key="item.status" class="bar-row">
              <span class="bar-label">{{ item.status }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: item.pct + '%', background: statusColors[idx] }"></div>
              </div>
              <span class="bar-count">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-row :gutter="12" align="middle">
        <el-col :span="4">
          <el-select v-model="query.type" placeholder="类型：全部" clearable size="small" @change="loadList(1)">
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="query.status" placeholder="状态：全部" clearable size="small" @change="loadList(1)">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="query.priority" placeholder="优先级：全部" clearable size="small" @change="loadList(1)">
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-button type="primary" size="small" @click="loadList(1)">查询</el-button>
          <el-button size="small" @click="resetQuery">重置</el-button>
        </el-col>
        <el-col :span="5" :offset="1">
          <el-input v-model="query.keyword" placeholder="搜索标题 / 内容 / 关联编号"
            size="small" clearable @keyup.enter="loadList(1)" @clear="loadList(1)" />
        </el-col>
        <el-col :span="3" style="text-align:right">
          <el-button size="small" @click="openSuggestions">规则建议闭环</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 工单列表 -->
    <div class="page-card">
      <el-table :data="tableData" v-loading="loading" size="small" row-key="id">
        <el-table-column label="工单号" prop="id" width="76" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="来源" prop="source" width="90" />
        <el-table-column label="优先级" width="74">
          <template #default="{ row }">
            <span :class="['priority', 'p-' + row.priority]">{{ row.priority }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联核保" prop="relatedNo" width="88">
          <template #default="{ row }">{{ row.relatedNo ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" prop="handler" width="80">
          <template #default="{ row }">{{ row.handler ?? '未分配' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" prop="createdAt" width="140" />
        <el-table-column label="满意度" width="90">
          <template #default="{ row }">
            <span v-if="row.satisfaction" class="stars">{{ '★'.repeat(row.satisfaction) }}</span>
            <span v-else class="no-star">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openHandle(row)">处理</el-button>
            <el-button v-if="canSuggest(row)" link type="success" size="small" @click="toRuleSuggestion(row)">转规则建议</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          small
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- 提交反馈弹框（页面内也有入口） -->
    <FeedbackDialog v-model="dialogVisible" @submitted="onSubmitted" />

    <!-- 创新点⑤：反馈驱动的规则优化闭环 -->
    <el-dialog v-model="suggestVisible" title="反馈驱动的规则优化闭环" width="760px">
      <div class="sug-stats">
        <span>建议总数 <b>{{ sugStats.total ?? 0 }}</b></span>
        <span>待确认 <b class="c-warn">{{ sugStats['待确认'] ?? 0 }}</b></span>
        <span>已采纳 <b class="c-ok">{{ sugStats['已采纳'] ?? 0 }}</b></span>
        <span>已驳回 <b class="c-muted">{{ sugStats['已驳回'] ?? 0 }}</b></span>
        <span>采纳率 <b class="c-ok">{{ sugStats.adoptRate ?? 0 }}%</b></span>
      </div>
      <el-table :data="suggestions" size="small" border max-height="380">
        <el-table-column label="建议号" prop="id" width="76" />
        <el-table-column label="来源反馈" prop="fromFeedbackId" width="88" />
        <el-table-column label="来源类型" prop="sourceType" width="90" />
        <el-table-column label="关联核保" prop="relatedNo" width="88">
          <template #default="{ row }">{{ row.relatedNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="建议内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title }}<span v-if="row.suggestedChange"> · {{ row.suggestedChange }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="82">
          <template #default="{ row }">
            <el-tag :type="sugTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待确认'">
              <el-button link type="success" size="small" @click="reviewSuggestion(row, true)">采纳</el-button>
              <el-button link type="danger" size="small" @click="reviewSuggestion(row, false)">驳回</el-button>
            </template>
            <span v-else class="c-muted">{{ row.reviewer || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="sug-tip">说明：采纳为人工闸口确认，沉淀为规则优化项反哺规则知识库；规则自动改写（自学习）列入后期版本规划。</div>
    </el-dialog>

    <!-- 处理工单弹框 -->
    <el-dialog v-model="handleVisible" title="处理工单" width="420px">
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="当前状态">
          <el-tag :type="statusTagType(handleRow.status)">{{ handleRow.status }}</el-tag>
        </el-form-item>
        <el-form-item label="更新状态">
          <el-select v-model="handleForm.status" style="width:100%">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="handleForm.handler" placeholder="处理人姓名" />
        </el-form-item>
        <el-form-item label="满意度" v-if="['已解决','已关闭'].includes(handleForm.status)">
          <el-rate v-model="handleForm.satisfaction" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="submitHandle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { feedbackApi, ruleSuggestionApi } from '../api/index'
import FeedbackDialog from '../components/FeedbackDialog.vue'
import { currentRole } from '../roles'

const typeOptions = ['问题反馈', '功能建议', '误判申诉', '规则优化', '投诉', '表扬']
const statusOptions = ['待处理', '处理中', '已解决', '已关闭', '已驳回']
const typeColors = ['#FA541C', '#1890ff', '#722ED1', '#13c2c2', '#eb2f96', '#52c41a']
const statusColors = ['#fa8c16', '#1890ff', '#52c41a', '#8c8c8c', '#ff4d4f']

const stats = ref({})
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const handleVisible = ref(false)
const handleSubmitting = ref(false)
const handleRow = ref({})
const handleForm = reactive({ status: '', handler: '', satisfaction: 0 })

const query = reactive({ type: '', status: '', priority: '', keyword: '' })

// 环形图计算
const donutSegments = computed(() => {
  if (!stats.value.byType) return []
  const total = typeOptions.reduce((s, t) => s + (stats.value.byType[t] || 0), 0)
  if (total === 0) return []
  const r = 55
  const C = 2 * Math.PI * r
  let cumPrev = 0
  return typeOptions.map((type, i) => {
    const count = stats.value.byType[type] || 0
    const len = (count / total) * C
    const gapLen = len > 2 ? len - 1.5 : len
    const seg = {
      type, color: typeColors[i], count,
      len: gapLen,
      rest: C - gapLen,
      startAngle: (cumPrev / C) * 360
    }
    cumPrev += len
    return seg
  }).filter(s => s.count > 0)
})

// 状态条形图
const statusBars = computed(() => {
  if (!stats.value.byStatus) return []
  const maxCount = Math.max(...statusOptions.map(s => stats.value.byStatus[s] || 0), 1)
  return statusOptions.map(s => ({
    status: s,
    count: stats.value.byStatus[s] || 0,
    pct: ((stats.value.byStatus[s] || 0) / maxCount) * 100
  }))
})

function typeTagType(type) {
  const map = { '问题反馈': 'danger', '功能建议': 'primary', '误判申诉': '', '规则优化': 'info', '投诉': 'warning', '表扬': 'success' }
  return map[type] ?? ''
}
function statusTagType(status) {
  const map = { '待处理': 'warning', '处理中': 'primary', '已解决': 'success', '已关闭': 'info', '已驳回': 'danger' }
  return map[status] ?? ''
}

async function loadList(page) {
  if (page) pagination.page = page
  loading.value = true
  try {
    const res = await feedbackApi.list({
      page: pagination.page, size: pagination.size,
      type: query.type, status: query.status,
      priority: query.priority, keyword: query.keyword
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await feedbackApi.stats()
  stats.value = res.data
}

function resetQuery() {
  Object.assign(query, { type: '', status: '', priority: '', keyword: '' })
  loadList(1)
}

function openHandle(row) {
  handleRow.value = row
  handleForm.status = row.status
  handleForm.handler = row.handler || ''
  handleForm.satisfaction = row.satisfaction || 0
  handleVisible.value = true
}

async function submitHandle() {
  handleSubmitting.value = true
  try {
    const patch = {
      status: handleForm.status,
      handler: handleForm.handler || null,
      satisfaction: ['已解决', '已关闭'].includes(handleForm.status) && handleForm.satisfaction > 0
        ? handleForm.satisfaction : null
    }
    await feedbackApi.update(handleRow.value.id, patch)
    ElMessage.success('工单已更新')
    handleVisible.value = false
    loadList()
    loadStats()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    handleSubmitting.value = false
  }
}

function onSubmitted() {
  loadList(1)
  loadStats()
}

// ===== 创新点⑤：反馈驱动的规则优化闭环 =====
const suggestVisible = ref(false)
const suggestions = ref([])
const sugStats = ref({})
const convertedIds = ref(new Set())   // 已转过规则建议的反馈 id，用于隐藏按钮

async function loadConvertedIds() {
  try {
    const res = await ruleSuggestionApi.feedbackIds()
    convertedIds.value = new Set(res.data || [])
  } catch { /* 忽略，最多是按钮多显示 */ }
}

// 「误判申诉 / 规则优化」类反馈可沉淀为规则建议（管理员操作）；已转过的不再显示
function canSuggest(row) {
  return ['误判申诉', '规则优化'].includes(row.type)
    && currentRole().name === '管理员'
    && !convertedIds.value.has(row.id)
}

async function toRuleSuggestion(row) {
  try {
    await ruleSuggestionApi.fromFeedback(row.id, {
      title: row.title,
      content: row.description || '',
      suggestedChange: ''
    })
    ElMessage.success(`已由 ${row.id} 生成规则优化建议，待确认`)
    convertedIds.value = new Set([...convertedIds.value, row.id])  // 立即隐藏该行按钮
    if (suggestVisible.value) await loadSuggestions()
    else await loadSugStats()
  } catch { ElMessage.error('生成规则建议失败') }
}

async function openSuggestions() {
  suggestVisible.value = true
  await loadSuggestions()
}

async function loadSuggestions() {
  const [listRes] = await Promise.all([ruleSuggestionApi.list({}), loadSugStats()])
  suggestions.value = listRes.data || []
}

async function loadSugStats() {
  const res = await ruleSuggestionApi.stats()
  sugStats.value = res.data || {}
}

async function reviewSuggestion(row, adopt) {
  try {
    await ruleSuggestionApi.review(row.id, { adopt, reviewer: currentRole().name })
    ElMessage.success(adopt ? '已采纳，将反哺规则知识库' : '已驳回')
    await loadSuggestions()
  } catch { ElMessage.error('操作失败') }
}

function sugTagType(s) {
  return { '待确认': 'warning', '已采纳': 'success', '已驳回': 'info' }[s] || 'info'
}

onMounted(() => {
  loadStats()
  loadList()
  loadConvertedIds()
})
</script>

<style scoped>
.subtitle { font-size: 13px; font-weight: 400; color: #a0a4ab; margin-left: 8px; }
.page-desc { font-size: 12px; color: #888; margin: -10px 0 16px; line-height: 1.6; }

.stat-row { margin-bottom: 14px; }
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.stat-label { font-size: 12px; color: #888; margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1f2329; }
.stat-unit { font-size: 14px; font-weight: 400; color: #aaa; }
.stat-card.pending .stat-value { color: #fa8c16; }
.stat-card.resolved .stat-value { color: #52c41a; }
.stat-card.satisfaction .stat-value { color: #eb2f96; }

.chart-row { margin-bottom: 14px; }
.chart-card { min-height: 200px; }
.chart-title { font-size: 13px; font-weight: 600; color: #1f2329; margin-bottom: 16px; }

.donut-wrap { display: flex; align-items: center; gap: 24px; }
.donut-svg { flex-shrink: 0; }
.donut-legend { flex: 1; }
.legend-item { display: flex; align-items: center; font-size: 12px; margin-bottom: 8px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; flex-shrink: 0; }
.legend-type { color: #555; }
.legend-count { font-weight: 600; color: #1f2329; margin-left: 2px; }

.status-bars { padding: 4px 0; }
.bar-row { display: flex; align-items: center; margin-bottom: 16px; }
.bar-label { width: 56px; font-size: 12px; color: #555; flex-shrink: 0; }
.bar-track { flex: 1; height: 18px; background: #f0f0f0; border-radius: 4px; overflow: hidden; margin: 0 10px; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.5s ease; }
.bar-count { width: 24px; font-size: 12px; font-weight: 600; color: #1f2329; text-align: right; }

.pagination-wrap { margin-top: 14px; display: flex; justify-content: flex-end; }

.priority { font-size: 12px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.p-高 { background: #fff1f0; color: #ff4d4f; }
.p-中 { background: #fffbe6; color: #fa8c16; }
.p-低 { background: #f6ffed; color: #52c41a; }

.stars { color: #fa8c16; font-size: 13px; letter-spacing: 1px; }
.no-star { color: #ccc; }

.sug-stats { display: flex; gap: 22px; font-size: 13px; color: #555; margin-bottom: 12px; }
.sug-stats b { font-size: 15px; color: #1f2329; margin-left: 2px; }
.c-warn { color: #fa8c16 !important; }
.c-ok { color: #52c41a !important; }
.c-muted { color: #999 !important; }
.sug-tip { font-size: 12px; color: #999; margin-top: 12px; line-height: 1.6; }
</style>
