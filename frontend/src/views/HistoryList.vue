<template>
  <div>
    <div class="page-title">历史客户风险画像</div>

    <div class="filter-bar">
      <el-form :model="query" label-width="96px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="画像唯一标识">
              <el-input v-model="query.profileId" placeholder="如 P001" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="投保人编号">
              <el-input v-model="query.customerId" placeholder="如 C001" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="职业类别">
              <el-input v-model="query.occupation" placeholder="如 司机" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="血压情况">
              <el-select v-model="bpCategory" placeholder="全部" clearable style="width: 100%">
                <el-option label="正常" value="正常" />
                <el-option label="正常高值" value="正常高值" />
                <el-option label="临界高血压" value="临界高血压" />
                <el-option label="轻度高血压" value="轻度高血压" />
                <el-option label="中度高血压" value="中度高血压" />
              </el-select>
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
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="年龄">
              <div class="range-input">
                <el-input-number v-model="ageMin" :controls="false" :precision="0" :step="1" placeholder="最小" style="width: 100%" />
                <span class="range-sep">~</span>
                <el-input-number v-model="ageMax" :controls="false" :precision="0" :step="1" placeholder="最大" style="width: 100%" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="年收入(元)">
              <div class="range-input">
                <el-input-number v-model="incomeMin" :controls="false" placeholder="最小" style="width: 100%" />
                <span class="range-sep">~</span>
                <el-input-number v-model="incomeMax" :controls="false" placeholder="最大" style="width: 100%" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="BMI">
              <div class="range-input">
                <el-input-number v-model="bmiMin" :controls="false" :precision="2" placeholder="最小" style="width: 100%" />
                <span class="range-sep">~</span>
                <el-input-number v-model="bmiMax" :controls="false" :precision="2" placeholder="最大" style="width: 100%" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :xl="6">
            <el-form-item label="首版评分">
              <div class="range-input">
                <el-input-number v-model="scoreMin" :controls="false" placeholder="最小" style="width: 100%" />
                <span class="range-sep">~</span>
                <el-input-number v-model="scoreMax" :controls="false" placeholder="最大" style="width: 100%" />
              </div>
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
      <el-table ref="tableRef" :data="rows" v-loading="loading" border stripe size="small"
        max-height="calc(100vh - 320px)" scrollbar-always-on @filter-change="onFilter">
        <el-table-column prop="profileId" label="画像唯一标识" min-width="120" />
        <el-table-column prop="customerId" label="投保人编号" min-width="100" />
        <el-table-column prop="age" label="年龄" min-width="70" />
        <el-table-column prop="gender" label="性别" min-width="80" column-key="gender"
          :filters="genderFilters" :filter-multiple="false" />
        <el-table-column prop="occupation" label="职业类别" min-width="110" show-overflow-tooltip />
        <el-table-column prop="annualIncome" label="年收入(元)" min-width="110" :formatter="(r,c,v) => v != null ? Number(v).toFixed(2) : '—'" />
        <el-table-column label="社保" min-width="80" column-key="social"
          :filters="socialFilters" :filter-multiple="false">
          <template #default="{ row }">{{ row.hasSocialInsurance ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="smokingStatus" label="吸烟" min-width="90" column-key="smoking"
          :filters="smokingFilters" :filter-multiple="false" />
        <el-table-column prop="drinkingStatus" label="饮酒" min-width="90" column-key="drinking"
          :filters="drinkingFilters" :filter-multiple="false" />
        <el-table-column prop="bmi" label="BMI" min-width="80" />
        <el-table-column prop="bloodPressure" label="血压" min-width="90" />
        <el-table-column label="是否理赔" min-width="100" column-key="target"
          :filters="targetFilters" :filter-multiple="false">
          <template #default="{ row }">
            <el-tag :type="row.target === 1 ? 'danger' : 'success'" size="small">
              {{ row.target === 1 ? '有理赔' : '无理赔' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scoreV1" label="首版评分" min-width="90" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :current-page="query.pageNum" :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]" @current-change="onPage" @size-change="onSize" />
    </div>

    <el-dialog v-model="detailVisible" title="客户画像明细" width="720px">
      <el-descriptions :column="2" border v-if="current">
        <el-descriptions-item label="画像唯一标识">{{ current.profileId }}</el-descriptions-item>
        <el-descriptions-item label="投保人编号">{{ current.customerId }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ current.age }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ current.gender }}</el-descriptions-item>
        <el-descriptions-item label="职业类别">{{ current.occupation }}</el-descriptions-item>
        <el-descriptions-item label="年收入(元)">{{ current.annualIncome != null ? Number(current.annualIncome).toFixed(2) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="社保">{{ current.hasSocialInsurance ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="吸烟">{{ current.smokingStatus }}</el-descriptions-item>
        <el-descriptions-item label="饮酒">{{ current.drinkingStatus }}</el-descriptions-item>
        <el-descriptions-item label="BMI">{{ current.bmi }}</el-descriptions-item>
        <el-descriptions-item label="血压">{{ current.bloodPressure }}</el-descriptions-item>
        <el-descriptions-item label="是否理赔">{{ current.target === 1 ? '有理赔' : '无理赔' }}</el-descriptions-item>
        <el-descriptions-item label="家族病史" :span="2">{{ current.familyMedicalHistory }}</el-descriptions-item>
        <el-descriptions-item label="个人病史" :span="2">{{ current.personalMedicalHistory }}</el-descriptions-item>
        <el-descriptions-item label="第一版风险评分" :span="2">{{ current.scoreV1 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ current.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ current.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { customerRiskApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const createdRange = ref([])
const updatedRange = ref([])
const detailVisible = ref(false)
const current = ref(null)
const tableRef = ref(null)
const bpCategory = ref('')
const ageMin = ref(null)
const ageMax = ref(null)
const incomeMin = ref(null)
const incomeMax = ref(null)
const bmiMin = ref(null)
const bmiMax = ref(null)
const scoreMin = ref(null)
const scoreMax = ref(null)

// 列头筛选选项
const genderFilters = [ { text: '男', value: '男' }, { text: '女', value: '女' } ]
const socialFilters = [ { text: '是', value: true }, { text: '否', value: false } ]
const smokingFilters = [ { text: '是', value: '是' }, { text: '否', value: '否' }, { text: '已戒烟', value: '已戒烟' } ]
const drinkingFilters = [ { text: '是', value: '是' }, { text: '否', value: '否' }, { text: '偶尔', value: '偶尔' } ]
const targetFilters = [ { text: '有理赔', value: 1 }, { text: '无理赔', value: 0 } ]

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  profileId: '',
  customerId: '',
  occupation: '',
  gender: '',
  smokingStatus: '',
  drinkingStatus: '',
  hasSocialInsurance: null,
  target: null
})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    params.createdFrom = createdRange.value?.[0]
    params.createdTo = createdRange.value?.[1]
    params.updatedFrom = updatedRange.value?.[0]
    params.updatedTo = updatedRange.value?.[1]
    params.bloodPressure = bpCategory.value || null
    params.ageMin = ageMin.value ?? null
    params.ageMax = ageMax.value ?? null
    params.incomeMin = incomeMin.value ?? null
    params.incomeMax = incomeMax.value ?? null
    params.bmiMin = bmiMin.value ?? null
    params.bmiMax = bmiMax.value ?? null
    params.scoreMin = scoreMin.value ?? null
    params.scoreMax = scoreMax.value ?? null
    const res = await customerRiskApi.page(params)
    rows.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 列头筛选变化 → 映射到查询条件并服务端重查
function onFilter(filters) {
  if ('gender' in filters) query.gender = filters.gender?.[0] ?? ''
  if ('smoking' in filters) query.smokingStatus = filters.smoking?.[0] ?? ''
  if ('drinking' in filters) query.drinkingStatus = filters.drinking?.[0] ?? ''
  if ('social' in filters) query.hasSocialInsurance = filters.social?.length ? filters.social[0] : null
  if ('target' in filters) query.target = filters.target?.length ? filters.target[0] : null
  query.pageNum = 1
  load()
}

function search() {
  query.pageNum = 1
  load()
}
function reset() {
  query.profileId = ''
  query.customerId = ''
  query.occupation = ''
  query.gender = ''
  query.smokingStatus = ''
  query.drinkingStatus = ''
  query.hasSocialInsurance = null
  query.target = null
  bpCategory.value = ''
  ageMin.value = null
  ageMax.value = null
  incomeMin.value = null
  incomeMax.value = null
  bmiMin.value = null
  bmiMax.value = null
  scoreMin.value = null
  scoreMax.value = null
  createdRange.value = []
  updatedRange.value = []
  tableRef.value?.clearFilter()
  query.pageNum = 1
  load()
}
function onPage(p) { query.pageNum = p; load() }
function onSize(s) { query.pageSize = s; query.pageNum = 1; load() }

async function openDetail(row) {
  const res = await customerRiskApi.detail(row.profileId)
  current.value = res.data
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.pager { margin-top: 14px; justify-content: flex-end; }
.filter-bar :deep(.el-form-item__label) { white-space: nowrap; }
.range-input { display: flex; align-items: center; gap: 6px; width: 100%; }
.range-input :deep(.el-input-number) { flex: 1; min-width: 0; }
.range-input :deep(.el-input-number .el-input__wrapper) { width: 100%; }
.range-sep { color: var(--el-text-color-placeholder); flex-shrink: 0; font-size: 14px; }
/* 常显横向滚动条不遮挡最后一行数据 */
:deep(.el-table__body-wrapper .el-scrollbar__view) { padding-bottom: 12px; }
</style>
