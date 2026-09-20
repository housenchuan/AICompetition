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
        <el-table-column label="AI置信度" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.confidence != null" :type="confTagType(row.confidence)" size="small" effect="dark">{{ row.confidence }}</el-tag>
            <span v-else class="muted">待预测</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" min-width="90" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="188" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" :loading="predictingId === row.decisionId" :disabled="batchRunning" @click="doPredictOne(row)">预测</el-button>
            <el-button v-if="canReview && row.adjustStatus === '待审批'" link type="danger" @click="openReview(row)">审核</el-button>
            <el-button v-if="canAdjust && row.adjustStatus !== '待审批'" link type="success" :disabled="row.riskLevel == null"
              @click="openAdjust(row)">人工修整</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="query.pageNum" :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]" @current-change="onPage" @size-change="onSize" />
    </div>

    <!-- 详情 / 审核：详情=投保申请+核保决策；审核=核保决策+人工修整审计（不含投保申请） -->
    <el-dialog v-model="detailVisible" :title="dialogMode === 'review' ? ('人工修整审核 · ' + (detailDec?.decisionId || '')) : '投保核保关联详情'" width="820px">
      <template v-if="detailApp || detailDec">
        <template v-if="dialogMode === 'detail'">
          <div class="sec-title">投保申请信息</div>
          <el-empty v-if="!detailApp" description="无关联投保申请" :image-size="60" />
          <el-descriptions v-else :column="2" border size="small">
            <el-descriptions-item label="申请编号">{{ detailApp.profileId }}</el-descriptions-item>
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
        </template>

        <!-- 核保决策信息：仅详情视图 -->
        <template v-if="dialogMode === 'detail'">
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
              <el-descriptions-item label="年收入(元)">{{ detailDec.annualIncome != null ? Number(detailDec.annualIncome).toFixed(2) : '—' }}</el-descriptions-item>
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
              <el-descriptions-item label="AI 置信度">
                <el-tag v-if="detailDec.confidence != null" :type="confTagType(detailDec.confidence)" size="small" effect="dark">{{ detailDec.confidence }} 分</el-tag>
                <span v-else class="muted">待预测</span>
              </el-descriptions-item>
              <el-descriptions-item label="关键风险因子">{{ detailDec.keyFactors ?? '待预测' }}</el-descriptions-item>
            </el-descriptions>
          </template>
        </template>

        <!-- 人工修整审计：仅审核视图（不含投保申请/核保决策信息） -->
        <template v-if="dialogMode === 'review' && detailDec && audit && (audit.records?.length || audit.aiBaseline)">
            <div class="sec-title">人工修整审计</div>

            <!-- 待审批：核保主管可对比 AI vs 人工 并审批 -->
            <div v-if="pendingRecord" class="review-box">
              <div class="review-hd">
                <span>本条修整待审批</span>
                <el-tag size="small" type="warning">审批路径：{{ approvalPathText(pendingRecord) }}</el-tag>
              </div>
              <el-table :data="compareRows(pendingRecord)" border size="small" class="cmp-table">
                <el-table-column prop="label" label="字段" width="110" />
                <el-table-column prop="ai" label="AI 预测值" />
                <el-table-column label="人工修改值">
                  <template #default="{ row }">
                    <span :class="{ diff: row.changed }">{{ row.human }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <div class="review-reason">修整原因：{{ pendingRecord.reason }}　·　提交人：{{ pendingRecord.submitRole }}</div>
              <div v-if="canReview" class="review-act">
                <el-input v-model="reviewComment" type="textarea" :rows="2" placeholder="审批意见（可选）" style="margin-bottom:10px" />
                <el-button type="success" :loading="reviewing" @click="doReview(true)">✓ 审批通过</el-button>
                <el-button type="danger" plain :loading="reviewing" @click="doReview(false)">✗ 驳回</el-button>
              </div>
              <div v-else class="muted" style="margin-top:8px">仅核保主管可审批</div>
            </div>

            <!-- 时间线：AI 核保 → 人工提交 → 审批结果 -->
            <div class="timeline">
              <div class="tl-item">
                <b>AI 自动核保 · 规则引擎 + LLM 双引擎</b>
                <el-tag v-if="audit.confidence" :type="confTagType(audit.confidence.confidence)" size="small" effect="dark" style="margin-left:6px">置信度 {{ audit.confidence.confidence }}</el-tag>
                <span class="tl-time">{{ detailDec.createdAt }}</span>
                <div v-if="audit.aiBaseline" class="tl-body">
                  结论 {{ audit.aiBaseline.underwritingResult }} / {{ audit.aiBaseline.riskLevel }} / 加费{{ pct(audit.aiBaseline.premiumAdjustment) }} / 评分{{ audit.aiBaseline.riskScore }}
                  <br />规则命中：{{ audit.aiBaseline.keyFactors || '无显著风险因素' }}
                  <template v-if="audit.confidence && audit.confidence.signals && audit.confidence.signals.length">
                    <br />置信度扣分：<span v-for="(s, si) in audit.confidence.signals" :key="si" class="conf-signal">{{ s.factor }}（{{ s.delta }}）</span>
                  </template>
                </div>
              </div>
              <div v-for="(rec, i) in (audit.records || [])" :key="i" class="tl-item">
                <b>人工修整 · {{ rec.submitRole }}</b>
                <el-tag size="small" :type="adjustTagType(statusTag(rec.status))" style="margin-left:6px">{{ rec.status }}</el-tag>
                <span class="tl-time">{{ rec.submitAt }}</span>
                <div class="tl-body">
                  {{ recSummary(rec) }}<br />原因：{{ rec.reason }}
                  <template v-if="rec.reviewAt"><br />审批：{{ rec.reviewRole }} · {{ rec.reviewAt }}<span v-if="rec.reviewComment"> · {{ rec.reviewComment }}</span></template>
                </div>
              </div>
            </div>
          </template>
      </template>
    </el-dialog>

    <!-- 人工修整弹窗 -->
    <el-dialog v-model="adjustVisible" :title="`人工修整 · ${adjustRow?.customerId || ''}`" width="620px">
      <div v-if="adjustRow" class="adjust-cur">
        <span>当前结论</span>
        <b>{{ adjustRow.underwritingResult }}</b>
        <el-tag :type="riskType(adjustRow.riskLevel)" size="small">{{ adjustRow.riskLevel }}</el-tag>
        <span>· 加费 {{ pct(adjustRow.premiumAdjustment) }} · 评分 {{ adjustRow.riskScore }}</span>
      </div>
      <el-form :model="adjustForm" label-width="96px" class="adjust-form">
        <el-form-item label="核保结论">
          <el-select v-model="adjustForm.underwritingResult" style="width:100%" @change="onResultChange">
            <el-option v-for="r in RESULT_OPTS" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="adjustForm.riskLevel" style="width:100%">
            <el-option v-for="l in LEVEL_OPTS" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="加费比例(%)">
          <el-input-number v-model="adjustForm.premiumPct" :min="0" :max="300" :step="5" :disabled="zeroPremium"
            controls-position="right" style="width:180px" />
          <span v-if="zeroPremium" class="muted" style="margin-left:10px">拒保/延期自动归零</span>
        </el-form-item>
        <el-form-item label="风险评分">
          <el-input-number v-model="adjustForm.riskScore" :min="0" :max="999" controls-position="right" style="width:180px" />
        </el-form-item>
        <el-form-item label="关键风险因子">
          <el-input v-model="adjustForm.keyFactors" type="textarea" :rows="3"
            placeholder="AI 语义生成的关键风险因子，可在此人工修整；留空则保留原值" />
        </el-form-item>
        <el-form-item label="修整原因" required>
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3"
            placeholder="必填：请说明人工修整依据（复核材料 / 规则例外 / 客户申诉等）" />
        </el-form-item>
      </el-form>
      <div class="adjust-path">审批路径：核保主管审批（按修整严重程度自动标注 普通→一级 / 重大→二级）· {{ isSupervisor ? '当前为核保主管，提交即生效' : '提交后进入待审批' }}</div>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSaving" @click="submitAdjust">
          {{ isSupervisor ? '提交（直接生效）' : '提交（待审批）' }}
        </el-button>
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
import { currentRole } from '../roles'

// 角色权限：核保专员/核保主管可发起修整；仅核保主管可审批；管理员只读
const role = computed(() => currentRole())
const roleName = computed(() => role.value.name)
const isSupervisor = computed(() => roleName.value === '核保主管')
const canAdjust = computed(() => ['核保专员', '核保主管'].includes(roleName.value))
// 分级审批：统一由核保主管审批（按严重程度标注一级/二级）
const canReview = computed(() => roleName.value === '核保主管')

// 覆写下拉选项（与规则 levels 的 result/level 一致）
const RESULT_OPTS = ['标保（标准费率承保）', '加费承保（加费10%~20%）', '加费承保（加费20%~50%）或除外责任', '延期承保并加费50%以上', '拒保']
const LEVEL_OPTS = ['标准体', '次标体A级', '次标体B级', '高风险体', '拒保体']

function pct(coeff) { if (coeff == null) return '—'; return Math.round((Number(coeff) - 1) * 100) + '%' }
// 置信度分档配色（仅展示得分，不再展示审核优先级路由）
function confTagType(c) { if (c == null) return 'info'; if (c >= 85) return 'success'; if (c >= 70) return 'warning'; return 'danger' }
// 创新点②：按记录严重程度显示分级审批路径（统一核保主管审批）
function approvalPathText(rec) {
  if (!rec) return '—'
  const sev = rec.adjSeverity || (rec.level === 2 ? '重大修整' : '普通修整')
  const approver = rec.approverRole || '核保主管'
  return `${sev} · ${rec.level === 2 ? '二级' : '一级'}审批（${approver}）`
}
function adjustTagType(s) { return { '待审批': 'warning', '已生效': 'success', '已驳回': 'info' }[s] || 'info' }
function statusTag(s) { if (s === '待审批') return '待审批'; if (s === '已通过' || s === '已生效') return '已生效'; if (s === '已驳回') return '已驳回'; return '' }

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
const dialogMode = ref('detail')   // 'detail' 详情 | 'review' 审核
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
// 详情：投保申请 + 核保决策（不含审核内容）
async function openDetail(row) {
  dialogMode.value = 'detail'
  audit.value = null
  const res = await applicationApi.withDecision(row.applicationId)
  detailApp.value = res.data.application
  detailDec.value = res.data.decision
  detailVisible.value = true
}

// 审核：核保决策 + 人工修整审计（不含投保申请信息）
async function openReview(row) {
  dialogMode.value = 'review'
  audit.value = null
  const res = await applicationApi.withDecision(row.applicationId)
  detailApp.value = res.data.application
  detailDec.value = res.data.decision
  detailVisible.value = true
  if (detailDec.value?.decisionId) loadAudit(detailDec.value.decisionId)
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

// ===== 人工修整 =====
const adjustVisible = ref(false)
const adjustRow = ref(null)
const adjustSaving = ref(false)
const adjustForm = reactive({ underwritingResult: '', riskLevel: '', premiumPct: 0, riskScore: 0, keyFactors: '', reason: '' })
const zeroPremium = computed(() => /拒保|延期/.test(adjustForm.underwritingResult))

function onResultChange() { if (zeroPremium.value) adjustForm.premiumPct = 0 }

function openAdjust(row) {
  adjustRow.value = row
  adjustForm.underwritingResult = row.underwritingResult || ''
  adjustForm.riskLevel = row.riskLevel || ''
  adjustForm.premiumPct = row.premiumAdjustment != null ? Math.round((Number(row.premiumAdjustment) - 1) * 100) : 0
  adjustForm.riskScore = row.riskScore ?? 0
  adjustForm.keyFactors = row.keyFactors || ''
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!adjustForm.reason.trim()) { ElMessage.warning('修整原因必填'); return }
  adjustSaving.value = true
  try {
    const premiumAdjustment = zeroPremium.value ? 1 : Number((1 + adjustForm.premiumPct / 100).toFixed(2))
    await decisionApi.adjust(adjustRow.value.decisionId, {
      underwritingResult: adjustForm.underwritingResult,
      riskLevel: adjustForm.riskLevel,
      premiumAdjustment,
      riskScore: adjustForm.riskScore,
      keyFactors: adjustForm.keyFactors,
      reason: adjustForm.reason.trim(),
      role: roleName.value
    })
    ElMessage.success(isSupervisor.value ? '修整已提交并直接生效' : '修整已提交，待核保主管审批')
    adjustVisible.value = false
    await load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    adjustSaving.value = false
  }
}

// ===== 审计留痕 / 审批 =====
const audit = ref(null)
const reviewComment = ref('')
const reviewing = ref(false)
const pendingRecord = computed(() => (audit.value?.records || []).find(r => r.status === '待审批') || null)

function compareRows(rec) {
  const ai = rec.aiPrediction || {}, h = rec.humanValue || {}
  return [
    { label: '核保结论', ai: ai.underwritingResult, human: h.underwritingResult, changed: ai.underwritingResult !== h.underwritingResult },
    { label: '风险等级', ai: ai.riskLevel, human: h.riskLevel, changed: ai.riskLevel !== h.riskLevel },
    { label: '加费比例', ai: pct(ai.premiumAdjustment), human: pct(h.premiumAdjustment), changed: Number(ai.premiumAdjustment) !== Number(h.premiumAdjustment) },
    { label: '风险评分', ai: ai.riskScore, human: h.riskScore, changed: ai.riskScore !== h.riskScore },
    { label: '关键风险因子', ai: ai.keyFactors || '—', human: h.keyFactors || '—', changed: (ai.keyFactors || '') !== (h.keyFactors || '') }
  ]
}
function recSummary(rec) {
  const h = rec.humanValue || {}
  return `→ ${h.underwritingResult} / ${h.riskLevel} / 加费${pct(h.premiumAdjustment)} / 评分${h.riskScore}`
}
async function loadAudit(decisionId) {
  try { const res = await decisionApi.audit(decisionId); audit.value = res.data } catch (e) { audit.value = null }
}
async function doReview(pass) {
  reviewing.value = true
  try {
    await decisionApi.review(detailDec.value.decisionId, { pass, comment: reviewComment.value, role: roleName.value })
    ElMessage.success(pass ? '审批通过，已覆写生效' : '已驳回')
    reviewComment.value = ''
    await loadAudit(detailDec.value.decisionId)
    await load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    reviewing.value = false
  }
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
.review-box { border: 1px solid #ffe0b2; background: #fffdf5; border-radius: 8px; padding: 12px 14px; margin-bottom: 14px; }
.review-hd { display: flex; align-items: center; justify-content: space-between; font-size: 13px; font-weight: 600; color: #1f2329; margin-bottom: 10px; }
.cmp-table { margin-bottom: 10px; }
.cmp-table .diff { color: #f56c6c; font-weight: 600; }
.review-reason { font-size: 12px; color: #6b7280; margin-bottom: 10px; }
.timeline { padding: 6px 0 2px; }
.tl-item { position: relative; padding: 0 0 14px 16px; border-left: 2px solid #eef0f3; }
.tl-item::before { content: ''; position: absolute; left: -5px; top: 4px; width: 8px; height: 8px; border-radius: 50%; background: var(--brand); }
.tl-item:last-child { border-left-color: transparent; }
.tl-time { font-size: 11px; color: #a0a4ab; margin-left: 8px; }
.tl-body { font-size: 12px; color: #4a4f57; margin-top: 4px; line-height: 1.6; }
.conf-signal { display: inline-block; margin: 0 6px 2px 0; padding: 0 6px; background: #fef0f0; color: #f56c6c; border-radius: 4px; }
.adjust-cur { font-size: 13px; color: #4a4f57; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.adjust-cur b { color: #1f2329; }
.adjust-path { font-size: 12px; color: #8a8f99; margin-top: 4px; }
.progress-box { padding: 6px 4px 10px; }
.progress-text { margin-top: 14px; text-align: center; font-size: 13px; color: #4a4f57; }
.progress-text .fail { color: #f56c6c; margin-left: 6px; }
:deep(.el-scrollbar__bar.is-horizontal) { height: 10px; }
:deep(.el-scrollbar__bar.is-horizontal > .el-scrollbar__thumb) { background: #b4b8bf; }
/* 常显横向滚动条不遮挡最后一行数据 */
:deep(.el-table__body-wrapper .el-scrollbar__view) { padding-bottom: 12px; }
</style>
