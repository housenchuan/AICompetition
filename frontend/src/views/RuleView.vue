<template>
  <div v-loading="loading">
    <div class="page-title">风险计分规则（核保评级标准）</div>

    <el-alert v-if="rules" :closable="false" type="warning" class="mb"
      :title="'规则版本 ' + rules.version + '：' + rules.versionNote" />

    <template v-if="rules">
      <div class="page-card mb">
        <div class="sec-title">① BMI 指数评分</div>
        <el-table :data="rules.bmi" border size="small">
          <el-table-column label="BMI 范围">
            <template #default="{ row }">{{ row.range || fmtRange(row.min, row.max) }}</template>
          </el-table-column>
          <el-table-column prop="label" label="分类" />
          <el-table-column label="风险加分"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
        </el-table>
      </div>

      <div class="page-card mb">
        <div class="sec-title">② 血压评分</div>
        <el-table :data="rules.bloodPressure" border size="small">
          <el-table-column prop="label" label="分类" />
          <el-table-column label="风险加分"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
        </el-table>
      </div>

      <el-row :gutter="16" class="mb">
        <el-col :span="12">
          <div class="page-card">
            <div class="sec-title">③ 吸烟评分</div>
            <el-table :data="rules.smoking" border size="small">
              <el-table-column prop="status" label="状况" />
              <el-table-column label="风险加分"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="page-card">
            <div class="sec-title">③ 饮酒评分</div>
            <el-table :data="rules.drinking" border size="small">
              <el-table-column prop="status" label="状况" />
              <el-table-column label="风险加分"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <div class="page-card mb">
        <div class="sec-title">④ 个人病史评分</div>
        <el-table :data="rules.personalHistory" border size="small">
          <el-table-column prop="category" label="疾病类别" width="180" />
          <el-table-column label="具体疾病"><template #default="{ row }">{{ row.diseasesText || row.diseases.join('、') }}</template></el-table-column>
          <el-table-column label="风险加分" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.reject" type="danger" size="small">拒保（不计分）</el-tag>
              <b v-else class="score">+{{ row.score }}</b>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-card mb">
        <div class="sec-title">⑤ 年龄评分</div>
        <el-table :data="rules.age" border size="small">
          <el-table-column label="年龄范围"><template #default="{ row }">{{ row.range || fmtAge(row.min, row.max) }}</template></el-table-column>
          <el-table-column prop="label" label="分类" />
          <el-table-column label="风险加分"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
        </el-table>
      </div>

      <div class="page-card mb">
        <div class="sec-title">⑥ 职业风险评分 <el-tag size="small" type="info">{{ rules.occupationNote }}</el-tag></div>
        <el-table :data="rules.occupation" border size="small">
          <el-table-column prop="level" label="职业风险等级" width="130" />
          <el-table-column label="典型职业"><template #default="{ row }">{{ row.occupations.join('、') }}</template></el-table-column>
          <el-table-column label="风险加分" width="100"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
        </el-table>
      </div>

      <div class="page-card mb">
        <div class="sec-title">⑦ 家族病史评分 <el-tag size="small" type="info">{{ rules.familyHistoryNote }}</el-tag></div>
        <el-table :data="rules.familyHistory" border size="small">
          <el-table-column prop="level" label="家族病史类别" width="130" />
          <el-table-column label="具体病史"><template #default="{ row }">{{ row.diseasesText || (row.diseases.length ? row.diseases.join('、') : '（待历史数据挖掘填充）') }}</template></el-table-column>
          <el-table-column label="风险加分" width="100"><template #default="{ row }"><b class="score">+{{ row.score }}</b></template></el-table-column>
        </el-table>
      </div>

      <div class="page-card mb">
        <div class="sec-title">⑧ 风险等级分类标准 <el-tag size="small" type="info">{{ rules.levelsNote }}</el-tag></div>
        <div class="sec-sub">核保风险总分 = 各维度风险加分之和。根据总分确定最终风险等级与核保结论如下表：</div>
        <el-table :data="rules.levels" border size="small">
          <el-table-column label="风险总分范围（需要划分分数范围）" width="240"><template #default="{ row }">{{ scoreRange(row) }}</template></el-table-column>
          <el-table-column prop="level" label="风险等级" width="110">
            <template #default="{ row }"><el-tag :type="levelType(row.level)" size="small">{{ row.level }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="result" label="核保结论" width="220" />
          <el-table-column prop="note" label="说明" />
        </el-table>
        <div v-if="rules.levelsFootnote" class="footnote">{{ rules.levelsFootnote }}</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ruleApi } from '../api'

const rules = ref(null)
const loading = ref(false)

function fmtRange(min, max) {
  if (max >= 999) return `≥ ${min}`
  if (min <= 0) return `< ${max}`
  return `${min} ~ ${max}`
}
function fmtAge(min, max) {
  if (max >= 200) return `${min} 岁以上`
  if (min <= 0) return `${max} 岁以下`
  return `${min} ~ ${max} 岁`
}
function scoreRange(row) {
  if (row.scoreRangeText) return row.scoreRangeText
  if (row.minScore < 0) return '直接判定'
  if (row.maxScore >= 999) return `≥ ${row.minScore}`
  return `${row.minScore} ~ ${row.maxScore}`
}
function levelType(l) {
  return { '标准体': 'success', '次标体A级': 'warning', '次标体B级': 'warning', '高风险体': 'danger', '拒保体': 'danger' }[l] || ''
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await ruleApi.list()
    rules.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.sec-title { font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.sec-sub { font-size: 13px; color: #606266; margin-bottom: 10px; }
.footnote { font-size: 12px; color: #909399; line-height: 1.7; margin-top: 10px; }
.score { color: var(--brand); }
</style>
