<template>
  <div>
    <div class="page-title">历史客户风险画像</div>

    <div class="filter-bar">
      <el-form :model="query" label-width="76px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="客户编号">
              <el-input v-model="query.customerId" placeholder="如 C001" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="性别">
              <el-select v-model="query.gender" placeholder="全部" clearable style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="职业">
              <el-input v-model="query.occupation" placeholder="职业类别" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="吸烟">
              <el-select v-model="query.smokingStatus" placeholder="全部" clearable style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
                <el-option label="已戒烟" value="已戒烟" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="创建时间">
              <el-date-picker v-model="createdRange" type="daterange" value-format="YYYY-MM-DD"
                range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
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
      <el-table :data="rows" v-loading="loading" border stripe size="small" height="calc(100vh - 320px)">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="customerId" label="客户编号" width="90" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="occupation" label="职业" width="110" show-overflow-tooltip />
        <el-table-column prop="annualIncome" label="年收入(元)" width="110" />
        <el-table-column label="社保" width="60">
          <template #default="{ row }">{{ row.hasSocialInsurance ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="smokingStatus" label="吸烟" width="80" />
        <el-table-column prop="drinkingStatus" label="饮酒" width="80" />
        <el-table-column prop="bmi" label="BMI" width="70" />
        <el-table-column prop="bloodPressure" label="血压" width="90" />
        <el-table-column label="是否理赔" width="90">
          <template #default="{ row }">
            <el-tag :type="row.target === 1 ? 'danger' : 'success'" size="small">
              {{ row.target === 1 ? '有理赔' : '无理赔' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scoreV1" label="首版评分" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
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
        <el-descriptions-item label="画像ID">{{ current.profileId }}</el-descriptions-item>
        <el-descriptions-item label="客户编号">{{ current.customerId }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ current.age }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ current.gender }}</el-descriptions-item>
        <el-descriptions-item label="职业">{{ current.occupation }}</el-descriptions-item>
        <el-descriptions-item label="年收入(元)">{{ current.annualIncome }}</el-descriptions-item>
        <el-descriptions-item label="社保">{{ current.hasSocialInsurance ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="吸烟">{{ current.smokingStatus }}</el-descriptions-item>
        <el-descriptions-item label="饮酒">{{ current.drinkingStatus }}</el-descriptions-item>
        <el-descriptions-item label="BMI">{{ current.bmi }}</el-descriptions-item>
        <el-descriptions-item label="血压">{{ current.bloodPressure }}</el-descriptions-item>
        <el-descriptions-item label="是否理赔">{{ current.target === 1 ? '有理赔' : '无理赔' }}</el-descriptions-item>
        <el-descriptions-item label="家族病史" :span="2">{{ current.familyMedicalHistory }}</el-descriptions-item>
        <el-descriptions-item label="个人病史" :span="2">{{ current.personalMedicalHistory }}</el-descriptions-item>
        <el-descriptions-item label="首版评分">{{ current.scoreV1 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ current.createdAt }}</el-descriptions-item>
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

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  customerId: '',
  gender: '',
  occupation: '',
  smokingStatus: ''
})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    params.createdFrom = createdRange.value?.[0]
    params.createdTo = createdRange.value?.[1]
    params.updatedFrom = updatedRange.value?.[0]
    params.updatedTo = updatedRange.value?.[1]
    const res = await customerRiskApi.page(params)
    rows.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}
function reset() {
  query.customerId = ''
  query.gender = ''
  query.occupation = ''
  query.smokingStatus = ''
  createdRange.value = []
  updatedRange.value = []
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
</style>
