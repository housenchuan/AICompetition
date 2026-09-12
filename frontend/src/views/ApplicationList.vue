<template>
  <div>
    <div class="page-title">投保申请记录</div>

    <div class="filter-bar">
      <el-form :model="query" label-width="76px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="客户编号">
              <el-input v-model="query.customerId" placeholder="如 C001" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="产品类型">
              <el-select v-model="query.productType" placeholder="全部" clearable style="width: 100%">
                <el-option label="寿险" value="寿险" />
                <el-option label="医疗险" value="医疗险" />
                <el-option label="意外险" value="意外险" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="申请状态">
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 100%">
                <el-option label="待核保" value="待核保" />
                <el-option label="核保中" value="核保中" />
                <el-option label="已通过" value="已通过" />
                <el-option label="已拒保" value="已拒保" />
                <el-option label="已撤单" value="已撤单" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="申请日期">
              <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
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
        <el-button type="primary" @click="openCreate">新增申请</el-button>
        <span class="tip">先填投保申请，保存后可继续填写核保决策画像；仅人工创建的记录可编辑。</span>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small" height="calc(100vh - 360px)">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="applicationId" label="申请编号" width="120" show-overflow-tooltip />
        <el-table-column prop="customerId" label="客户编号" width="90" />
        <el-table-column prop="productType" label="产品类型" width="90" />
        <el-table-column prop="productName" label="产品名称" width="130" show-overflow-tooltip />
        <el-table-column prop="coverageAmount" label="保额(元)" width="110" />
        <el-table-column prop="premium" label="保费(元)" width="90" />
        <el-table-column prop="paymentFrequency" label="缴费频率" width="90" />
        <el-table-column prop="insurancePeriod" label="保障期限" width="90" />
        <el-table-column label="申请状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicationDate" label="申请日期" width="110" />
        <el-table-column prop="createdBy" label="创建人" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" @click="openDecision(row)">核保结果</el-button>
            <el-button v-if="row.createdBy === '人工'" link type="success" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="query.pageNum" :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]" @current-change="onPage" @size-change="onSize" />
    </div>

    <el-dialog v-model="detailVisible" title="投保申请明细" width="720px">
      <el-descriptions :column="2" border v-if="current">
        <el-descriptions-item label="申请编号">{{ current.applicationId }}</el-descriptions-item>
        <el-descriptions-item label="客户编号">{{ current.customerId }}</el-descriptions-item>
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
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="decisionVisible" title="关联核保决策结果" width="720px">
      <el-empty v-if="!decision" description="该申请暂无核保决策结果" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="决策编号">{{ decision.decisionId }}</el-descriptions-item>
        <el-descriptions-item label="申请编号">{{ decision.applicationId }}</el-descriptions-item>
        <el-descriptions-item label="风险评分">{{ decision.riskScore ?? '待预测' }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">
          <el-tag v-if="decision.riskLevel" size="small">{{ decision.riskLevel }}</el-tag>
          <span v-else>待预测</span>
        </el-descriptions-item>
        <el-descriptions-item label="核保结论">{{ decision.underwritingResult ?? '待预测' }}</el-descriptions-item>
        <el-descriptions-item label="加费比例">{{ decision.premiumAdjustment ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="关键风险因子" :span="2">{{ decision.keyFactors ?? '待预测' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="form.applicationId ? '编辑投保申请' : '新增投保申请'" width="720px">
      <el-form :model="form" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="客户编号"><el-input v-model="form.customerId" placeholder="如 C010" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产品类型">
            <el-select v-model="form.productType" placeholder="请选择" style="width:100%">
              <el-option label="寿险" value="寿险" /><el-option label="医疗险" value="医疗险" /><el-option label="意外险" value="意外险" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="产品名称"><el-input v-model="form.productName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="保额(元)"><el-input-number v-model="form.coverageAmount" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="保费(元)"><el-input-number v-model="form.premium" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="缴费频率">
            <el-select v-model="form.paymentFrequency" placeholder="请选择" style="width:100%">
              <el-option label="年缴" value="年缴" /><el-option label="半年缴" value="半年缴" /><el-option label="季缴" value="季缴" /><el-option label="月缴" value="月缴" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="保障期限"><el-input v-model="form.insurancePeriod" placeholder="如 终身/20年/1年" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="等待期(天)"><el-input-number v-model="form.waitingPeriod" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="受益人关系"><el-input v-model="form.beneficiaryRelationship" placeholder="如 配偶/子女" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="申请日期"><el-date-picker v-model="form.applicationDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="申请状态">
            <el-select v-model="form.status" placeholder="请选择" style="width:100%">
              <el-option label="待核保" value="待核保" /><el-option label="核保中" value="核保中" /><el-option label="已通过" value="已通过" /><el-option label="已拒保" value="已拒保" /><el-option label="已撤单" value="已撤单" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="decFormVisible" title="填写核保决策画像（新增第 2 步）" width="760px">
      <el-alert :closable="false" type="info" class="dec-tip"
        :title="'关联投保申请：' + decForm.applicationId + '（AI 字段将在预测时生成，此处只填画像）'" />
      <el-form :model="decForm" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="客户编号"><el-input v-model="decForm.customerId" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年龄"><el-input-number v-model="decForm.age" :min="0" :max="120" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="性别">
            <el-select v-model="decForm.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="职业"><el-input v-model="decForm.occupation" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年收入(元)"><el-input-number v-model="decForm.annualIncome" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="是否有社保"><el-switch v-model="decForm.hasSocialInsurance" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="吸烟状况">
            <el-select v-model="decForm.smokingStatus" style="width:100%"><el-option label="是" value="是" /><el-option label="否" value="否" /><el-option label="已戒烟" value="已戒烟" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="饮酒状况">
            <el-select v-model="decForm.drinkingStatus" style="width:100%"><el-option label="是" value="是" /><el-option label="否" value="否" /><el-option label="偶尔" value="偶尔" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="BMI"><el-input-number v-model="decForm.bmi" :min="0" :precision="1" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="血压"><el-input v-model="decForm.bloodPressure" placeholder="如 120/80" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="家族病史"><el-input v-model="decForm.familyMedicalHistory" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="个人病史"><el-input v-model="decForm.personalMedicalHistory" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="decFormVisible = false">稍后再填</el-button>
        <el-button type="primary" :loading="saving" @click="saveDecision">保存画像</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applicationApi, decisionApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref([])
const detailVisible = ref(false)
const decisionVisible = ref(false)
const current = ref(null)
const decision = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  customerId: '',
  productType: '',
  status: ''
})

function statusType(s) {
  return { '已通过': 'success', '已拒保': 'danger', '核保中': 'warning', '待核保': 'info', '已撤单': 'info' }[s] || ''
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    params.dateFrom = dateRange.value?.[0]
    params.dateTo = dateRange.value?.[1]
    const res = await applicationApi.page(params)
    rows.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.pageNum = 1; load() }
function reset() {
  query.customerId = ''
  query.productType = ''
  query.status = ''
  dateRange.value = []
  query.pageNum = 1
  load()
}
function onPage(p) { query.pageNum = p; load() }
function onSize(s) { query.pageSize = s; query.pageNum = 1; load() }

async function openDetail(row) {
  const res = await applicationApi.detail(row.applicationId)
  current.value = res.data
  detailVisible.value = true
}
async function openDecision(row) {
  const res = await applicationApi.withDecision(row.applicationId)
  decision.value = res.data.decision
  decisionVisible.value = true
}

// ===== 新增 / 编辑 =====
const formVisible = ref(false)
const decFormVisible = ref(false)
const saving = ref(false)
const form = ref({})
const decForm = ref({})

function emptyForm() {
  return {
    applicationId: null, customerId: '', productType: '', productName: '',
    coverageAmount: null, premium: null, paymentFrequency: '', insurancePeriod: '',
    waitingPeriod: null, beneficiaryRelationship: '', applicationDate: '', status: '待核保'
  }
}

function openCreate() {
  form.value = emptyForm()
  formVisible.value = true
}
function openEdit(row) {
  form.value = { ...row }
  formVisible.value = true
}

async function saveForm() {
  saving.value = true
  try {
    if (form.value.applicationId) {
      await applicationApi.update(form.value.applicationId, form.value)
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } else {
      const res = await applicationApi.create(form.value)
      const app = res.data
      formVisible.value = false
      load()
      try {
        await ElMessageBox.confirm('投保申请已创建，是否继续填写核保决策画像？', '下一步', {
          confirmButtonText: '继续填写', cancelButtonText: '稍后再填', type: 'info'
        })
        decForm.value = {
          applicationId: app.applicationId, customerId: app.customerId,
          age: null, gender: '', occupation: '', annualIncome: null,
          hasSocialInsurance: true, smokingStatus: '否', drinkingStatus: '否',
          bmi: null, bloodPressure: '', familyMedicalHistory: '', personalMedicalHistory: ''
        }
        decFormVisible.value = true
      } catch (e) { /* 用户选择稍后再填 */ }
    }
  } finally {
    saving.value = false
  }
}

async function saveDecision() {
  saving.value = true
  try {
    await decisionApi.create(decForm.value)
    ElMessage.success('核保决策画像已保存，可在「核保决策结果」页发起预测')
    decFormVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.toolbar .tip { font-size: 12px; color: #8a8f99; }
.dec-tip { margin-bottom: 14px; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
