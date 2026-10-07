<template>
  <div class="page-container">
    <el-card>
      <el-radio-group v-model="tab" style="margin-bottom:10px" @change="load">
        <el-radio-button value="due">到约定回访时间</el-radio-button>
        <el-radio-button value="all">全部拒诊节点</el-radio-button>
        <el-radio-button value="joint">多方共同沟通</el-radio-button>
      </el-radio-group>

      <!-- 拒诊跟进节点 -->
      <template v-if="tab !== 'joint'">
        <el-alert type="warning" :closable="false" style="margin-bottom:10px"
          title="家长拒绝复诊不会被标成“已处理”：每个节点都记录拒绝原因、风险告知、确认时间、沟通人员；回访在约定时间发起，内容与历次筛查同档比较；再次拒绝形成新节点而不覆盖原记录。" />
        <el-table :data="rows" stripe size="small">
          <el-table-column label="序次" width="60">
            <template #default="{ row }">#{{ row.seqNo }}</template>
          </el-table-column>
          <el-table-column prop="studentName" label="学生" width="80" />
          <el-table-column label="学校/班级" min-width="180">
            <template #default="{ row }">{{ row.schoolName }} {{ row.gradeClass }}</template>
          </el-table-column>
          <el-table-column prop="triggerLabel" label="来源" width="140" />
          <el-table-column label="结果" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="outcomeType(row.outcome)">{{ row.outcomeLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reasonTypeLabel" label="拒绝归因" width="160" />
          <el-table-column label="拒绝原因/原话" min-width="200">
            <template #default="{ row }">
              <el-tooltip :content="row.riskNotice || ''" placement="top" :disabled="!row.riskNotice">
                <span>{{ row.reasonDetail || '—' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="communicator" label="沟通人员" width="120" />
          <el-table-column prop="plannedCallbackDate" label="约定回访" width="110" />
          <el-table-column label="家长态度" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.parentAttitudeLabel" size="small"
                :type="row.parentAttitude==='STILL_REFUSE'?'danger':row.parentAttitude==='AGREED'?'success':'info'">
                {{ row.parentAttitudeLabel }}
              </el-tag>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="270" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="pick(row,'callback')">发起回访</el-button>
              <el-button size="small" type="danger" plain @click="pick(row,'refusal')">登记再次拒绝</el-button>
              <el-button size="small" type="warning" plain @click="pick(row,'joint')">申请共同沟通</el-button>
              <el-button link size="small" @click="detail(row)">档案</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 多方共同沟通台账 -->
      <template v-else>
        <el-table :data="jointRows" stripe size="small">
          <el-table-column prop="studentName" label="学生" width="80" />
          <el-table-column label="学校/班级" min-width="180">
            <template #default="{ row }">{{ row.schoolName }} {{ row.gradeClass }}</template>
          </el-table-column>
          <el-table-column prop="commTypeLabel" label="沟通对象" width="170" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status==='DONE'?'success':row.status==='REJECTED'?'info':'warning'">
                {{ row.statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="参与人" min-width="170">
            <template #default="{ row }">
              班主任：{{ row.headTeacherName || '—' }} ｜ 社区医生：{{ row.communityDoctorName || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="applicant" label="申请人" width="150" />
          <el-table-column label="沟通结果/学校措施" min-width="220">
            <template #default="{ row }">
              <div v-if="row.commResult || row.schoolMeasure">
                <div>结果：{{ row.commResult || '—' }}</div>
                <div class="muted">措施：{{ row.schoolMeasure || '—' }}</div>
              </div>
              <span v-else class="muted">待沟通</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status==='APPLIED'" size="small" type="success"
                         @click="review(row,'ACCEPTED')">受理</el-button>
              <el-button v-if="['APPLIED','ACCEPTED'].includes(row.status)" size="small" type="primary"
                         @click="openResult(row)">登记沟通结果</el-button>
              <el-button link size="small" @click="goDetail(row.studentId)">档案</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <action-dialogs ref="dialogs" :student-id="sid" @done="load" />

    <!-- 共同沟通结果登记 -->
    <el-dialog v-model="resultVisible" title="共同沟通结果与学校干预措施" width="580px">
      <el-form label-width="120px">
        <el-form-item label="沟通方式"><el-input v-model="resultForm.channel" placeholder="家访/电话/三方会谈"/></el-form-item>
        <el-form-item label="沟通结果">
          <el-input v-model="resultForm.commResult" type="textarea" :rows="3"
            placeholder="家长反馈、是否转变、复诊约定等"/>
        </el-form-item>
        <el-form-item label="学校干预措施">
          <el-input v-model="resultForm.schoolMeasure" type="textarea" :rows="3"
            placeholder="如：座位调至前排居中、每日2小时户外、班主任每周提醒用眼；将同时写入学校干预记录，可追溯"/>
        </el-form-item>
        <el-form-item label="家长态度">
          <el-checkbox v-model="resultForm.parentAgreed">经共同沟通，家长已同意带孩子复诊</el-checkbox>
        </el-form-item>
        <el-form-item label="记录人"><el-input v-model="resultForm.recorder"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultVisible=false">取消</el-button>
        <el-button type="primary" @click="submitResult">保存沟通结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import ActionDialogs from '../components/ActionDialogs.vue'

const router = useRouter()
const tab = ref('due')
const rows = ref([])
const jointRows = ref([])
const sid = ref(null)
const dialogs = ref()

const load = async () => {
  if (tab.value === 'joint') {
    jointRows.value = await api.jointComms('ALL')
  } else {
    rows.value = await api.refusals(tab.value === 'due')
  }
}

const pick = (row, name) => {
  sid.value = row.studentId
  dialogs.value.open(name)
}
const detail = (row) => router.push(`/students/${row.studentId}`)
const goDetail = (id) => router.push(`/students/${id}`)

const outcomeType = (o) => ({
  REFUSED: 'danger', RE_REMINDED: 'warning', CALLBACK_DONE: 'info',
  AGREED_VISIT: 'success', JOINT_COMM: 'warning', JOINT_DONE: 'success'
}[o] || 'info')

const review = async (row, action) => {
  await api.jointReview(row.id, action, null)
  ElMessage.success('已受理')
  load()
}

const resultVisible = ref(false)
const resultForm = ref({})
const openResult = (row) => {
  resultForm.value = { jointCommId: row.id, channel: row.channel || '家访', commResult: '',
    schoolMeasure: '', parentAgreed: false, recorder: '' }
  resultVisible.value = true
}
const submitResult = async () => {
  if (!resultForm.value.commResult) {
    ElMessage.warning('请填写沟通结果')
    return
  }
  await api.jointResult(resultForm.value)
  resultVisible.value = false
  ElMessage.success('沟通结果与学校干预措施已记录')
  load()
}

onMounted(load)
</script>
