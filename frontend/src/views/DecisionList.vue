<template>
  <div>
    <div class="page-title">核保决策结果</div>

    <div class="filter-bar">
      <el-form :model="query" label-width="118px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="核保决策唯一标识">
              <el-input v-model="query.decisionId" placeholder="如 D001" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="投保申请编号">
              <el-input v-model="query.applicationId" placeholder="如 A001" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="投保人编号">
              <el-input v-model="query.customerId" placeholder="如 C001" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="创建人">
              <el-input v-model="query.createdBy" placeholder="如 人工/系统" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="创建时间">
              <el-date-picker v-model="createdRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="更新时间">
              <el-date-picker v-model="updatedRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-button type="warning" plain :disabled="!selected.length || batchRunning" :loading="batchRunning" @click="doPredictBatch">
          批量预测（{{ selected.length }}）
        </el-button>
        <span class="tip">勾选记录后可批量预测；或点单行「预测」。规则引擎给出风险评分/等级/结论/加费/关键因子；如需修改画像请到「投保申请记录」页编辑。</span>
      </div>
      <el-table ref="tableRef" :data="rows" v-loading="loading" border stripe size="small" max-height="calc(100vh - 330px)"
        scrollbar-always-on @selection-change="onSelect" @filter-change="onFilter">
        <el-table-column type="selection" width="42" fixed="left" />
        <el-table-column prop="decisionId" label="核保决策唯一标识" width="140" fixed="left" show-overflow-tooltip />
        <el-table-column prop="customerId" label="投保人编号" min-width="100" />
        <el-table-column prop="applicationId" label="投保申请编号" min-width="110" show-overflow-tooltip />
        <el-table-column prop="age" label="年龄" min-width="70" />
        <el-table-column prop="gender" label="性别" min-width="80" column-key="gender"
          :filters="genderFilters" :filter-multiple="false" />
        <el-table-column prop="occupation" label="职业类别" min-width="100" show-overflow-tooltip />
        <el-table-column label="是否有社保" min-width="110" column-key="social"
          :filters="socialFilters" :filter-multiple="false">
          <template #default="{ row }">{{ row.hasSocialInsurance ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="smokingStatus" label="吸烟状况" min-width="100" column-key="smoking"
          :filters="smokingFilters" :filter-multiple="false" />
        <el-table-column prop="drinkingStatus" label="饮酒状况" min-width="100" column-key="drinking"
          :filters="drinkingFilters" :filter-multiple="false" />
        <el-table-column label="风险评分" min-width="90">
          <template #default="{ row }">
            <span v-if="row.riskScore != null" class="score">{{ row.riskScore }}</span>
            <span v-else class="muted">待预测</span>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" min-width="110" column-key="riskLevel"
          :filters="riskLevelFilters" :filter-multiple="false">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" :type="riskType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
            <span v-else class="muted">待预测</span>
          </template>
        </el-table-column>
        <el-table-column prop="underwritingResult" label="核保结论" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.underwritingResult">{{ row.underwritingResult }}</span>
            <span v-else class="muted">待预测</span>
          </template>
        </el-table-column>
        <el-table-column label="加费比例" min-width="90">
          <template #default="{ row }">
            <span v-if="row.premiumAdjustment != null">{{ row.premiumAdjustment }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="keyFactors" label="关键风险因子" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.keyFactors">{{ row.keyFactors }}</span>
            <span v-else class="muted">待预测</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" min-width="90" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="132" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" :loading="predictingId === row.decisionId" :disabled="batchRunning" @click="doPredictOne(row)">预测</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="query.pageNum" :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]" @current-change="onPage" @size-change="onSize" />
    </div>

    <!-- 详情：投保申请 + 核保决策 两张表全字段（与投保申请记录页一致） -->
    <el-dialog v-model="detailVisible" title="投保核保关联详情" width="820px">
      <template v-if="detailApp || detailDec">
        <div class="sec-title">投保申请信息</div>
        <el-empty v-if="!detailApp" description="无关联投保申请" :image-size="60" />
        <el-descriptions v-else :column="2" border size="small">
          <el-descriptions-item label="申请编号">{{ detailApp.applicationId }}</el-descriptions-item>
          <el-descriptions-item label="投保人编号">{{ detailApp.customerId }}</el-descriptions-item>
          <el-descriptions-item label="产品类型">{{ detailApp.productType }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailApp.productName }}</el-descriptions-item>
          <el-descriptions-item label="保额(元)">{{ detailApp.coverageAmount }}</el-descriptions-item>
          <el-descriptions-item label="保费(元)">{{ detailApp.premium }}</el-descriptions-item>
          <el-descriptions-item label="缴费频率">{{ detailApp.paymentFrequency }}</el-descriptions-item>
          <el-descriptions-item label="保障期限">{{ detailApp.insurancePeriod }}</el-descriptions-item>
          <el-descriptions-item label="等待期(天)">{{ detailApp.waitingPeriod }}</el-descriptions-item>
          <el-descriptions-item label="受益人关系">{{ detailApp.beneficiaryRelationship }}</el-descriptions-item>
          <el-descriptions-item label="申请日期">{{ detailApp.applicationDate }}</el-descriptions-item>
          <el-descriptions-item label="申请状态">{{ detailApp.status }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detailApp.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailApp.createdAt }}</el-descriptions-item>
        </el-descriptions>

        <div class="sec-title">核保决策信息</div>
        <el-empty v-if="!detailDec" description="该申请暂无核保决策结果" :image-size="60" />
        <template v-else>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="核保决策唯一标识">{{ detailDec.decisionId }}</el-descriptions-item>
            <el-descriptions-item label="投保申请编号">{{ detailDec.applicationId }}</el-descriptions-item>
            <el-descriptions-item label="投保人编号">{{ detailDec.customerId }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ detailDec.age }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ detailDec.gender }}</el-descriptions-item>
            <el-descriptions-item label="职业类别">{{ detailDec.occupation }}</el-descriptions-item>
            <el-descriptions-item label="年收入(元)">{{ detailDec.annualIncome }}</el-descriptions-item>
            <el-descriptions-item label="社保">{{ detailDec.hasSocialInsurance ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="吸烟">{{ detailDec.smokingStatus }}</el-descriptions-item>
            <el-descriptions-item label="饮酒">{{ detailDec.drinkingStatus }}</el-descriptions-item>
            <el-descriptions-item label="BMI">{{ detailDec.bmi }}</el-descriptions-item>
            <el-descriptions-item label="血压">{{ detailDec.bloodPressure }}</el-descriptions-item>
            <el-descriptions-item label="个人病史">{{ detailDec.personalMedicalHistory || '无' }}</el-descriptions-item>
            <el-descriptions-item label="家族病史" :span="2">{{ detailDec.familyMedicalHistory || '无' }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detailDec.createdBy }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detailDec.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ detailDec.updatedAt }}</el-descriptions-item>
          </el-descriptions>
          <el-descriptions class="ai-desc" :column="2" border size="small">
            <el-descriptions-item label="风险评分">
              <span class="score">{{ detailDec.riskScore ?? '待预测' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag v-if="detailDec.riskLevel" :type="riskType(detailDec.riskLevel)" size="small">{{ detailDec.riskLevel }}</el-tag>
              <span v-else>待预测</span>
            </el-descriptions-item>
            <el-descriptions-item label="核保结论">{{ detailDec.underwritingResult ?? '待预测' }}</el-descriptions-item>
            <el-descriptions-item label="加费比例">{{ detailDec.premiumAdjustment ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="关键风险因子" :span="2">{{ detailDec.keyFactors ?? '待预测' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </template>
    </el-dialog>

    <!-- 批量预测进度 -->
    <el-dialog v-model="progressVisible" title="批量预测中" width="420px"
      :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
      <div class="progress-box">
        <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="14" />
        <div class="progress-text">
          正在预测 {{ progress.done }} / {{ progress.total }}
          <span v-if="progress.fail" class="fail">· 失败 {{ progress.fail }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { decisionApi, predictApi, applicationApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const predictingId = ref(null)          // 单行预测：仅该行按钮 loading
const batchRunning = ref(false)         // 批量预测运行中
const progressVisible = ref(false)
const progress = reactive({ done: 0, total: 0, fail: 0 })
const progressPercent = computed(() => progress.total ? Math.round(progress.done / progress.total * 100) : 0)
const progressStatus = computed(() => {
  if (progress.total === 0 || progress.done < progress.total) return ''
  return progress.fail ? 'exception' : 'success'
})
const detailVisible = ref(false)
const detailApp = ref(null)
const detailDec = ref(null)
const tableRef = ref(null)

const genderFilters = [ { text: '男', value: '男' }, { text: '女', value: '女' } ]
const socialFilters = [ { text: '是', value: true }, { text: '否', value: false } ]
const smokingFilters = [ { text: '是', value: '是' }, { text: '否', value: '否' }, { text: '已戒烟', value: '已戒烟' } ]
const drinkingFilters = [ { text: '是', value: '是' }, { text: '否', value: '否' }, { text: '偶尔', value: '偶尔' } ]
const riskLevelFilters = [
  { text: '标准体', value: '标准体' }, { text: '次标体A级', value: '次标体A级' }, { text: '次标体B级', value: '次标体B级' },
  { text: '高风险体', value: '高风险体' }, { text: '拒保体', value: '拒保体' }
]

const createdRange = ref([])
const updatedRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  decisionId: '',
  applicationId: '',
  customerId: '',
  createdBy: '',
  gender: '',
  smokingStatus: '',
  drinkingStatus: '',
  hasSocialInsurance: null,
  riskLevel: ''
})

function riskType(l) {
  return { '标准体': 'success', '次标体A级': 'warning', '次标体B级': 'warning', '高风险体': 'danger', '拒保体': 'danger' }[l] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    params.createdFrom = createdRange.value?.[0]
    params.createdTo = createdRange.value?.[1]
    params.updatedFrom = updatedRange.value?.[0]
    params.updatedTo = updatedRange.value?.[1]
    const res = await decisionApi.page(params)
    rows.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 列头筛选（性别/社保/吸烟/饮酒/风险等级）→ 服务端重查
function onFilter(filters) {
  if ('gender' in filters) query.gender = filters.gender?.[0] ?? ''
  if ('smoking' in filters) query.smokingStatus = filters.smoking?.[0] ?? ''
  if ('drinking' in filters) query.drinkingStatus = filters.drinking?.[0] ?? ''
  if ('social' in filters) query.hasSocialInsurance = filters.social?.length ? filters.social[0] : null
  if ('riskLevel' in filters) query.riskLevel = filters.riskLevel?.[0] ?? ''
  query.pageNum = 1
  load()
}

function search() { query.pageNum = 1; load() }
function reset() {
  query.decisionId = ''
  query.applicationId = ''
  query.customerId = ''
  query.createdBy = ''
  query.gender = ''
  query.smokingStatus = ''
  query.drinkingStatus = ''
  query.hasSocialInsurance = null
  query.riskLevel = ''
  createdRange.value = []
  updatedRange.value = []
  tableRef.value?.clearFilter()
  query.pageNum = 1
  load()
}
function onPage(p) { query.pageNum = p; load() }
function onSize(s) { query.pageSize = s; query.pageNum = 1; load() }

// 详情：联合展示投保申请 + 核保决策
async function openDetail(row) {
  const res = await applicationApi.withDecision(row.applicationId)
  detailApp.value = res.data.application
  detailDec.value = res.data.decision
  detailVisible.value = true
}

function onSelect(rows) { selected.value = rows }

async function doPredictOne(row) {
  predictingId.value = row.decisionId
  try {
    const res = await predictApi.single(row.decisionId)
    const d = res.data
    ElMessage.success(`预测完成：${d.riskLevel} · ${d.riskScore} 分`)
    await load()
  } finally {
    predictingId.value = null
  }
}

// 批量预测：仅预测勾选记录，逐条预测并显示进度条
async function doPredictBatch() {
  if (!selected.value.length) return
  const ids = selected.value.map(r => r.decisionId)
  batchRunning.value = true
  progress.done = 0
  progress.fail = 0
  progress.total = ids.length
  progressVisible.value = true
  for (const id of ids) {
    try {
      await predictApi.single(id)
    } catch (e) {
      progress.fail++
    }
    progress.done++
  }
  await load()
  batchRunning.value = false
  const ok = progress.total - progress.fail
  setTimeout(() => {
    progressVisible.value = false
    ElMessage.success(`批量预测完成，成功 ${ok} 条${progress.fail ? `，失败 ${progress.fail} 条` : ''}`)
  }, 600)
}

onMounted(load)
</script>

<style scoped>
.filter-bar :deep(.el-form-item__label) { white-space: nowrap; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.toolbar .tip { font-size: 12px; color: #8a8f99; }
.pager { margin-top: 14px; justify-content: flex-end; }
.muted { color: #b7bcc4; }
.score { color: var(--brand); font-weight: 600; }
.sec-title { font-size: 13px; font-weight: 600; color: #1f2329; margin: 6px 0 12px; padding-left: 8px; border-left: 3px solid var(--brand); }
.ai-desc { margin-top: 10px; }
.progress-box { padding: 6px 4px 10px; }
.progress-text { margin-top: 14px; text-align: center; font-size: 13px; color: #4a4f57; }
.progress-text .fail { color: #f56c6c; margin-left: 6px; }
:deep(.el-scrollbar__bar.is-horizontal) { height: 10px; }
:deep(.el-scrollbar__bar.is-horizontal > .el-scrollbar__thumb) { background: #b4b8bf; }
/* 常显横向滚动条不遮挡最后一行数据 */
:deep(.el-table__body-wrapper .el-scrollbar__view) { padding-bottom: 12px; }
</style>
