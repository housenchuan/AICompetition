<template>
  <el-dialog
    v-model="visible"
    title="提交反馈"
    width="560px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="hint">
      您的反馈将进入工单池，由对应团队处理并跟踪；涉及核保结论的误判申诉可关联具体决策编号。
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" class="fb-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="反馈类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择" style="width:100%">
              <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源渠道" prop="source">
            <el-select v-model="form.source" placeholder="请选择" style="width:100%">
              <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" placeholder="请选择" style="width:100%">
              <el-option label="高" value="高" />
              <el-option label="中" value="中" />
              <el-option label="低" value="低" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联核保编号">
            <el-input v-model="form.relatedNo" placeholder="可选，如 D3005" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="一句话概括反馈" maxlength="80" show-word-limit />
      </el-form-item>

      <el-form-item label="详细描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请描述问题、建议或申诉依据"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="联系方式">
        <el-input v-model="form.contact" placeholder="选填，便于回访（手机/企微）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交反馈</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { feedbackApi } from '../api/index'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'submitted'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

const typeOptions = ['问题反馈', '功能建议', '误判申诉', '规则优化', '投诉', '表扬']
const sourceOptions = ['核保员', '代理人', '客户', '内部质检', '系统巡检']

const formRef = ref(null)
const submitting = ref(false)
const form = reactive({
  type: '',
  source: '',
  priority: '中',
  relatedNo: '',
  title: '',
  description: '',
  contact: ''
})

const rules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  source: [{ required: true, message: '请选择来源渠道', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }]
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await feedbackApi.create({ ...form })
      ElMessage.success('反馈提交成功，工单已进入处理队列')
      emit('submitted')
      handleClose()
    } catch (e) {
      ElMessage.error('提交失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

function handleClose() {
  formRef.value?.resetFields()
  Object.assign(form, { type: '', source: '', priority: '中', relatedNo: '', title: '', description: '', contact: '' })
  visible.value = false
}
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #888;
  background: #f6f7f9;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 18px;
  line-height: 1.6;
}
.fb-form :deep(.el-form-item__label) {
  font-size: 13px;
}
</style>
