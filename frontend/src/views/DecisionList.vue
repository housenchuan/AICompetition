<template>
  <div>
    <div class="page-title">核保决策结果</div>

    <div class="filter-bar">
      <el-form :model="query" label-width="76px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="客户编号">
              <el-input v-model="query.customerId" placeholder="如 C001" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="风险等级">
              <el-select v-model="query.riskLevel" placeholder="全部" clearable style="width: 100%">
                <el-option label="标准体" value="标准体" />
                <el-option label="次标体A级" value="次标体A级" />
                <el-option label="次标体B级" value="次标体B级" />
                <el-option label="高风险体" value="高风险体" />
                <el-option label="拒保体" value="拒保体" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="核保结论">
              <el-input v-model="query.underwritingResult" placeholder="如 标保/加费/拒保" clearable />
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
        <el-button type="primary" :disabled="!selected.length" :loading="predicting" @click="doPredictBatch">
          批量预测（{{ selected.length }}）
        </el-button>
        <span class="tip">勾选记录后可批量预测；或点击单条「预测」。规则引擎给出评分/等级/结论/加费/关键因子。</span>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe size="small" height="calc(100vh - 340px)"
        @selection-change="onSelect">
        <el-table-column type="selection" width="46" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="customerId" label="客户编号" width="90" />
        <el-table-column prop="applicationId" label="申请编号" width="120" show-overflow-tooltip />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="occupation" label="职业" width="100" show-overflow-tooltip />
        <el-table-column label="风险评分" width="90">
          <template #default="{ row }">
            <span v-if="row.riskScore != null" class="score">{{ row.riskScore }}</span>
            <el-tag v-else type="info" size="small">待预测</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" :type="levelType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="underwritingResult" label="核保结论" width="150" show-overflow-tooltip />
        <el-table-column prop="premiumAdjustment" label="加费比例" width="90" />
        <el-table-column prop="keyFactors" label="关键风险因子" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="warning" @click="doPredictOne(row)">预测</el-button>
            <el-button v-if="row.createdBy === '人工'" link type="success" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="query.pageNum" :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]" @current-change="onPage" @size-change="onSize" />
    </div>

    <el-dialog v-model="detailVisible" title="核保决策明细" width="760px">
      <el-descriptions :column="2" border v-if="current">
        <el-descriptions-item label="决策编号">{{ current.decisionId }}</el-descriptions-item>
        <el-descriptions-item label="申请编号">{{ current.applicationId }}</el-descriptions-item>
        <el-descriptions-item label="客户编号">{{ current.customerId }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ current.age }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ current.gender }}</el-descriptions-item>
        <el-descriptions-item label="职业">{{ current.occupation }}</el-descriptions-item>
        <el-descriptions-item label="BMI">{{ current.bmi }}</el-descriptions-item>
        <el-descriptions-item label="血压">{{ current.bloodPressure }}</el-descriptions-item>
        <el-descriptions-item label="吸烟">{{ current.smokingStatus }}</el-descriptions-item>
        <el-descriptions-item label="饮酒">{{ current.drinkingStatus }}</el-descriptions-item>
        <el-descriptions-item label="家族病史" :span="2">{{ current.familyMedicalHistory }}</el-descriptions-item>
        <el-descriptions-item label="个人病史" :span="2">{{ current.personalMedicalHistory }}</el-descriptions-item>
        <el-descriptions-item label="风险评分（AI）">{{ current.riskScore ?? '待预测' }}</el-descriptions-item>
        <el-descriptions-item label="风险等级（AI）">{{ current.riskLevel ?? '待预测' }}</el-descriptions-item>
        <el-descriptions-item label="核保结论（AI）">{{ current.underwritingResult ?? '待预测' }}</el-descriptions-item>
        <el-descriptions-item label="加费比例（AI）">{{ current.premiumAdjustment ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="关键风险因子（AI）" :span="2">{{ current.keyFactors ?? '待预测' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="formVisible" title="编辑核保决策画像（人工填写部分）" width="760px">
      <el-alert :closable="false" type="info" class="dec-tip"
        title="仅编辑画像字段；风险评分/等级/结论/加费/关键因子为 AI 生成，请通过「预测」更新。" />
      <el-form :model="form" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="客户编号"><el-input v-model="form.customerId" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年龄"><el-input-number v-model="form.age" :min="0" :max="120" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="性别">
            <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="职业"><el-input v-model="form.occupation" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年收入(元)"><el-input-number v-model="form.annualIncome" :min="0" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="是否有社保"><el-switch v-model="form.hasSocialInsurance" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="吸烟状况">
            <el-select v-model="form.smokingStatus" style="width:100%"><el-option label="是" value="是" /><el-option label="否" value="否" /><el-option label="已戒烟" value="已戒烟" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="饮酒状况">
            <el-select v-model="form.drinkingStatus" style="width:100%"><el-option label="是" value="是" /><el-option label="否" value="否" /><el-option label="偶尔" value="偶尔" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="BMI"><el-input-number v-model="form.bmi" :min="0" :precision="1" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="血压"><el-input v-model="form.bloodPressure" placeholder="如 120/80" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="家族病史"><el-input v-model="form.familyMedicalHistory" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="个人病史"><el-input v-model="form.personalMedicalHistory" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { decisionApi, predictApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const current = ref(null)
const selected = ref([])
const predicting = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  customerId: '',
  riskLevel: '',
  underwritingResult: ''
})

function levelType(l) {
  return { '标准体': 'success', '次标体A级': 'warning', '次标体B级': 'warning', '高风险体': 'danger', '拒保体': 'danger' }[l] || ''
}

async function load() {
  loading.value = true
  try {
    const res = await decisionApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.pageNum = 1; load() }
function reset() {
  query.customerId = ''
  query.riskLevel = ''
  query.underwritingResult = ''
  query.pageNum = 1
  load()
}
function onPage(p) { query.pageNum = p; load() }
function onSize(s) { query.pageSize = s; query.pageNum = 1; load() }

async function openDetail(row) {
  const res = await decisionApi.detail(row.decisionId)
  current.value = res.data
  detailVisible.value = true
}

function onSelect(rows) { selected.value = rows }

const formVisible = ref(false)
const saving = ref(false)
const form = ref({})
function openEdit(row) {
  form.value = { ...row }
  formVisible.value = true
}
async function saveForm() {
  saving.value = true
  try {
    await decisionApi.update(form.value.decisionId, form.value)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doPredictOne(row) {
  predicting.value = true
  try {
    const res = await predictApi.single(row.decisionId)
    const d = res.data
    ElMessage.success(`预测完成：${d.riskLevel} · ${d.riskScore} 分`)
    await load()
  } finally {
    predicting.value = false
  }
}

async function doPredictBatch() {
  if (!selected.value.length) return
  predicting.value = true
  try {
    const ids = selected.value.map(r => r.decisionId)
    const res = await predictApi.batch(ids)
    ElMessage.success(`批量预测完成，共 ${res.data.length} 条`)
    await load()
  } finally {
    predicting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.toolbar .tip { font-size: 12px; color: #8a8f99; }
.dec-tip { margin-bottom: 14px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.score { font-weight: 600; color: var(--brand); }
</style>
