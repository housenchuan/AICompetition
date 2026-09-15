<template>
  <div>
    <div class="page-title">投保申请记录</div>

    <div class="filter-bar">
      <el-form :model="query" label-width="140px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="投保申请人唯一标识">
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
              <el-input v-model="query.createdBy" placeholder="如 人工" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="申请日期">
              <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
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
        <el-button type="primary" @click="openCreate">新增</el-button>
        <span class="tip">投保申请记录列表；新增/编辑为「先投保申请→再核保决策画像」两段填写；点「详情」查看该申请及其关联核保决策结果；预测在「核保决策结果」页发起。</span>
      </div>
      <el-table ref="tableRef" :data="rows" v-loading="loading" border stripe size="small"
        max-height="calc(100vh - 330px)" scrollbar-always-on @filter-change="onFilter">
        <el-table-column prop="applicationId" label="投保申请人唯一标识" width="150" fixed="left" show-overflow-tooltip />
        <el-table-column prop="customerId" label="投保人编号" min-width="100" />
        <el-table-column prop="productType" label="产品类型" min-width="100" column-key="productType"
          :filters="productTypeFilters" :filter-multiple="false" />
        <el-table-column prop="productName" label="产品名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="coverageAmount" label="保额(元)" min-width="110" />
        <el-table-column prop="premium" label="保费(元)" min-width="100" />
        <el-table-column prop="paymentFrequency" label="缴费频率" min-width="100" />
        <el-table-column prop="insurancePeriod" label="保障期限" min-width="100" />
        <el-table-column label="申请状态" min-width="110" column-key="status"
          :filters="statusFilters" :filter-multiple="false">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicationDate" label="申请日期" min-width="120" />
        <el-table-column prop="createdBy" label="创建人" min-width="90" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.createdBy === '人工'" link type="success" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="query.pageNum" :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]" @current-change="onPage" @size-change="onSize" />
    </div>

    <!-- 详情：投保申请 + 核保决策 两张表全字段 -->
    <el-dialog v-model="detailVisible" title="投保核保关联详情" width="820px">
      <template v-if="current">
        <div class="sec-title">投保申请信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="投保申请人唯一标识">{{ current.applicationId }}</el-descriptions-item>
          <el-descriptions-item label="投保人编号">{{ current.customerId }}</el-descriptions-item>
          <el-descriptions-item label="产品类型">{{ current.productType }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ current.productName }}</el-descriptions-item>
          <el-descriptions-item label="保额(元)">{{ current.coverageAmount }}</el-descriptions-item>
          <el-descriptions-item label="保费(元)">{{ current.premium }}</el-descriptions-item>
          <el-descriptions-item label="缴费频率">{{ current.paymentFrequency }}</el-descriptions-item>
          <el-descriptions-item label="保障期限">{{ current.insurancePeriod }}</el-descriptions-item>
          <el-descriptions-item label="等待期(天)">{{ current.waitingPeriod }}</el-descriptions-item>
          <el-descriptions-item label="受益人关系">{{ current.beneficiaryRelationship }}</el-descriptions-item>
          <el-descriptions-item label="申请日期">{{ current.applicationDate }}</el-descriptions-item>
          <el-descriptions-item label="申请状态">{{ current.status }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ current.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ current.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ current.updatedAt }}</el-descriptions-item>
        </el-descriptions>

        <div class="sec-title">核保决策信息</div>
        <el-empty v-if="!decision" description="该申请暂无核保决策结果" :image-size="60" />
        <template v-else>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="核保决策唯一标识">{{ decision.decisionId }}</el-descriptions-item>
            <el-descriptions-item label="投保申请编号">{{ decision.applicationId }}</el-descriptions-item>
            <el-descriptions-item label="投保人编号">{{ decision.customerId }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ decision.age }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ decision.gender }}</el-descriptions-item>
            <el-descriptions-item label="职业类别">{{ decision.occupation }}</el-descriptions-item>
            <el-descriptions-item label="年收入(元)">{{ decision.annualIncome }}</el-descriptions-item>
            <el-descriptions-item label="社保">{{ decision.hasSocialInsurance ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="吸烟">{{ decision.smokingStatus }}</el-descriptions-item>
            <el-descriptions-item label="饮酒">{{ decision.drinkingStatus }}</el-descriptions-item>
            <el-descriptions-item label="BMI">{{ decision.bmi }}</el-descriptions-item>
            <el-descriptions-item label="血压">{{ decision.bloodPressure }}</el-descriptions-item>
            <el-descriptions-item label="个人病史">{{ decision.personalMedicalHistory || '无' }}</el-descriptions-item>
            <el-descriptions-item label="家族病史" :span="2">{{ decision.familyMedicalHistory || '无' }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ decision.createdBy }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ decision.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ decision.updatedAt }}</el-descriptions-item>
          </el-descriptions>
          <el-descriptions class="ai-desc" :column="2" border size="small">
            <el-descriptions-item label="风险评分">
              <span class="score">{{ decision.riskScore ?? '待预测' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag v-if="decision.riskLevel" :type="riskType(decision.riskLevel)" size="small">{{ decision.riskLevel }}</el-tag>
              <span v-else>待预测</span>
            </el-descriptions-item>
            <el-descriptions-item label="核保结论">{{ decision.underwritingResult ?? '待预测' }}</el-descriptions-item>
            <el-descriptions-item label="加费比例">{{ decision.premiumAdjustment ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="关键风险因子" :span="2">{{ decision.keyFactors ?? '待预测' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑：两步向导（① 投保申请信息 → ② 核保决策画像） -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑投保核保记录' : '新增投保核保记录'" width="860px" top="6vh"
      class="edit-dialog" @open="step = 1">
      <el-steps :active="step - 1" finish-status="success" align-center class="dlg-steps">
        <el-step title="投保申请信息" />
        <el-step title="核保决策画像" />
      </el-steps>
      <div class="dlg-body">
        <div class="form-sec" v-show="step === 1">
          <div class="sec-hd"><span class="sec-no">1</span>投保申请信息</div>
          <el-form :model="appForm" label-width="92px">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="投保人编号"><el-input v-model="appForm.customerId" placeholder="如 C010" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="产品类型">
                <el-select v-model="appForm.productType" placeholder="请选择" style="width:100%">
                  <el-option label="年金险" value="年金险" /><el-option label="寿险" value="寿险" /><el-option label="医疗险" value="医疗险" /><el-option label="意外险" value="意外险" /><el-option label="重疾险" value="重疾险" />
                </el-select>
              </el-form-item></el-col>
              <el-col :span="12"><el-form-item label="产品名称"><el-input v-model="appForm.productName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="保额(元)"><el-input-number v-model="appForm.coverageAmount" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="保费(元)"><el-input-number v-model="appForm.premium" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="缴费频率">
                <el-select v-model="appForm.paymentFrequency" placeholder="请选择" style="width:100%">
                  <el-option label="年缴" value="年缴" /><el-option label="半年缴" value="半年缴" /><el-option label="季缴" value="季缴" /><el-option label="月缴" value="月缴" />
                </el-select>
              </el-form-item></el-col>
              <el-col :span="12"><el-form-item label="保障期限"><el-input v-model="appForm.insurancePeriod" placeholder="如 终身/20年/1年" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="等待期(天)"><el-input-number v-model="appForm.waitingPeriod" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="受益人关系"><el-input v-model="appForm.beneficiaryRelationship" placeholder="如 配偶/子女" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="申请日期"><el-date-picker v-model="appForm.applicationDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="申请状态">
                <el-select v-model="appForm.status" placeholder="请选择" style="width:100%">
                  <el-option label="待核保" value="待核保" /><el-option label="核保中" value="核保中" /><el-option label="已通过" value="已通过" /><el-option label="已拒保" value="已拒保" /><el-option label="已撤单" value="已撤单" />
                </el-select>
              </el-form-item></el-col>
            </el-row>
          </el-form>
        </div>

        <div class="form-sec" v-show="step === 2">
          <div class="sec-hd"><span class="sec-no">2</span>核保决策画像<span class="sec-note">（AI 字段：风险评分/等级/结论/加费/关键因子 在「预测」时生成，此处只填画像）</span></div>
          <el-form :model="decForm" label-width="92px">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="年龄"><el-input-number v-model="decForm.age" :min="0" :max="120" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="性别">
                <el-select v-model="decForm.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
              </el-form-item></el-col>
              <el-col :span="12"><el-form-item label="职业类别"><el-input v-model="decForm.occupation" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="年收入(元)"><el-input-number v-model="decForm.annualIncome" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="是否有社保"><el-switch v-model="decForm.hasSocialInsurance" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="吸烟状况">
                <el-select v-model="decForm.smokingStatus" style="width:100%"><el-option label="是" value="是" /><el-option label="否" value="否" /><el-option label="已戒烟" value="已戒烟" /></el-select>
              </el-form-item></el-col>
              <el-col :span="12"><el-form-item label="饮酒状况">
                <el-select v-model="decForm.drinkingStatus" style="width:100%"><el-option label="是" value="是" /><el-option label="否" value="否" /><el-option label="偶尔" value="偶尔" /></el-select>
              </el-form-item></el-col>
              <el-col :span="12"><el-form-item label="BMI"><el-input-number v-model="decForm.bmi" :min="0" :precision="2" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="血压">
                <el-select v-model="decForm.bloodPressure" placeholder="请选择血压情况" style="width:100%">
                  <el-option label="正常" value="正常" />
                  <el-option label="正常高值" value="正常高值" />
                  <el-option label="临界高血压" value="临界高血压" />
                  <el-option label="轻度高血压" value="轻度高血压" />
                  <el-option label="中度高血压" value="中度高血压" />
                </el-select>
              </el-form-item></el-col>
              <el-col :span="24"><el-form-item label="家族病史"><el-input v-model="decForm.familyMedicalHistory" type="textarea" :rows="2" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="个人病史"><el-input v-model="decForm.personalMedicalHistory" type="textarea" :rows="2" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </div>
      </div>

      <template #footer>
        <template v-if="step === 1">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" @click="step = 2">下一步</el-button>
        </template>
        <template v-else>
          <el-button @click="step = 1">上一步</el-button>
          <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { applicationApi, decisionApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref([])
const createdRange = ref([])
const updatedRange = ref([])
const detailVisible = ref(false)
const current = ref(null)
const decision = ref(null)
const tableRef = ref(null)

const productTypeFilters = [ { text: '年金险', value: '年金险' }, { text: '寿险', value: '寿险' }, { text: '医疗险', value: '医疗险' }, { text: '意外险', value: '意外险' }, { text: '重疾险', value: '重疾险' } ]
const statusFilters = [
  { text: '待核保', value: '待核保' }, { text: '核保中', value: '核保中' }, { text: '已通过', value: '已通过' },
  { text: '已拒保', value: '已拒保' }, { text: '已撤单', value: '已撤单' }
]

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  applicationId: '',
  customerId: '',
  productType: '',
  status: '',
  createdBy: ''
})

function statusType(s) {
  return { '已通过': 'success', '已拒保': 'danger', '核保中': 'warning', '待核保': 'info', '已撤单': 'info' }[s] || ''
}
function riskType(l) {
  return { '标准体': 'success', '次标体A级': 'warning', '次标体B级': 'warning', '高风险体': 'danger', '拒保体': 'danger' }[l] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    params.dateFrom = dateRange.value?.[0]
    params.dateTo = dateRange.value?.[1]
    params.createdFrom = createdRange.value?.[0]
    params.createdTo = createdRange.value?.[1]
    params.updatedFrom = updatedRange.value?.[0]
    params.updatedTo = updatedRange.value?.[1]
    const res = await applicationApi.page(params)
    rows.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 列头筛选（产品类型 / 申请状态）→ 服务端重查
function onFilter(filters) {
  if ('productType' in filters) query.productType = filters.productType?.[0] ?? ''
  if ('status' in filters) query.status = filters.status?.[0] ?? ''
  query.pageNum = 1
  load()
}

function search() { query.pageNum = 1; load() }
function reset() {
  query.applicationId = ''
  query.customerId = ''
  query.productType = ''
  query.status = ''
  query.createdBy = ''
  dateRange.value = []
  createdRange.value = []
  updatedRange.value = []
  tableRef.value?.clearFilter()
  query.pageNum = 1
  load()
}
function onPage(p) { query.pageNum = p; load() }
function onSize(s) { query.pageSize = s; query.pageNum = 1; load() }

// 详情：一次拉回投保申请 + 关联核保决策（联合展示）
async function openDetail(row) {
  const res = await applicationApi.withDecision(row.applicationId)
  current.value = res.data.application
  decision.value = res.data.decision
  detailVisible.value = true
}

// ===== 新增 / 编辑：单弹窗两段 =====
const formVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const step = ref(1)
const appForm = ref({})
const decForm = ref({})

function emptyApp() {
  return {
    applicationId: null, customerId: '', productType: '', productName: '',
    coverageAmount: null, premium: null, paymentFrequency: '', insurancePeriod: '',
    waitingPeriod: null, beneficiaryRelationship: '', applicationDate: '', status: '待核保'
  }
}
function emptyDec() {
  return {
    decisionId: null, age: null, gender: '', occupation: '', annualIncome: null,
    hasSocialInsurance: true, smokingStatus: '否', drinkingStatus: '否',
    bmi: null, bloodPressure: '', familyMedicalHistory: '', personalMedicalHistory: ''
  }
}

function openCreate() {
  editing.value = false
  step.value = 1
  appForm.value = emptyApp()
  decForm.value = emptyDec()
  formVisible.value = true
}

async function openEdit(row) {
  editing.value = true
  step.value = 1
  const res = await applicationApi.withDecision(row.applicationId)
  appForm.value = { ...res.data.application }
  decForm.value = res.data.decision ? { ...res.data.decision } : emptyDec()
  formVisible.value = true
}

async function saveForm() {
  saving.value = true
  try {
    if (editing.value) {
      await applicationApi.update(appForm.value.applicationId, appForm.value)
      const dec = { ...decForm.value, applicationId: appForm.value.applicationId, customerId: appForm.value.customerId }
      if (dec.decisionId) {
        await decisionApi.update(dec.decisionId, dec)
      } else {
        await decisionApi.create(dec)
      }
      ElMessage.success('保存成功，可在「核保决策结果」页发起预测')
    } else {
      const appRes = await applicationApi.create(appForm.value)
      const app = appRes.data
      await decisionApi.create({ ...decForm.value, applicationId: app.applicationId, customerId: app.customerId })
      ElMessage.success('新增成功，可在「核保决策结果」页发起预测')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
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
/* 新增/编辑弹窗美化 */
.dlg-steps { margin: 4px 0 18px; padding: 0 40px; }
.dlg-body { max-height: 66vh; overflow-y: auto; padding: 2px 10px 2px 2px; }
.form-sec { background: #fafbfc; border: 1px solid #eef0f3; border-radius: 10px; padding: 18px 20px 4px; margin-bottom: 14px; }
.sec-hd { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #1f2329; margin-bottom: 18px; }
.sec-no {
  width: 20px; height: 20px; border-radius: 6px; flex-shrink: 0;
  background: var(--brand); color: #fff; font-size: 12px; font-weight: 600;
  display: inline-flex; align-items: center; justify-content: center;
}
.sec-note { font-size: 12px; font-weight: 400; color: #a0a4ab; margin-left: 4px; }
.form-sec :deep(.el-form-item) { margin-bottom: 16px; }
.form-sec :deep(.el-form-item__label) { color: #4a4f57; font-weight: 500; }
.edit-dialog :deep(.el-dialog__body) { padding: 12px 20px 4px; }
.form-sec :deep(.el-input),
.form-sec :deep(.el-input-number),
.form-sec :deep(.el-select),
.form-sec :deep(.el-date-editor.el-input) { max-width: 250px; width: 100%; }
.form-sec :deep(.el-input__inner) { text-align: left; }
:deep(.el-scrollbar__bar.is-horizontal) { height: 10px; }
:deep(.el-scrollbar__bar.is-horizontal > .el-scrollbar__thumb) { background: #b4b8bf; }
/* 常显横向滚动条不遮挡最后一行数据 */
:deep(.el-table__body-wrapper .el-scrollbar__view) { padding-bottom: 12px; }
</style>
