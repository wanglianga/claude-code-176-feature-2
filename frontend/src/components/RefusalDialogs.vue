<template>
  <div>
    <!-- 1. 记录家长拒绝复诊 -->
    <el-dialog v-model="visible.refusal" title="记录家长拒绝复诊" width="620px">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px"
        title="拒绝不是“已处理”：必须留痕拒绝原因、风险告知内容、确认时间与沟通人员，并在约定时间回访。" />
      <el-form :model="f.refusal" label-width="120px">
        <el-form-item label="拒绝原因归类">
          <el-select v-model="f.refusal.refusalReason" style="width:100%">
            <el-option v-for="r in reasons" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="家长原话/补充">
          <el-input v-model="f.refusal.refusalReasonDetail" type="textarea" :rows="2"
            placeholder="如：孩子只是最近作业多、用眼疲劳，休息几天就好，不用去医院" />
        </el-form-item>
        <el-form-item label="风险告知内容">
          <el-input v-model="f.refusal.riskNotice" type="textarea" :rows="4"
            placeholder="本次已向家长告知的风险内容（留痕）；留空则使用平台标准风险提示" />
        </el-form-item>
        <el-form-item label="沟通人员">
          <el-input v-model="f.refusal.communicator" placeholder="如：健康老师 李校医（留空取当前登录人）" />
        </el-form-item>
        <el-form-item label="约定回访日期">
          <el-date-picker v-model="f.refusal.plannedReturnDate" value-format="YYYY-MM-DD" style="width:100%"
            placeholder="与家长约定的回访时间，到期会出现在“到期待回访”" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="f.refusal.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.refusal=false">取消</el-button>
        <el-button type="danger" @click="submit('refusal', api.refusalRecord)">保存拒诊记录</el-button>
      </template>
    </el-dialog>

    <!-- 2. 约定时间回访 -->
    <el-dialog v-model="visible.returnVisit" title="拒诊后约定回访" width="640px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="回访内容会与前一次筛查结果放在同一份学生档案中比较，用于观察视力变化与家长态度转变。" />
      <el-form :model="f.returnVisit" label-width="120px">
        <el-form-item label="实际回访日期">
          <el-date-picker v-model="f.returnVisit.actualReturnDate" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="孩子座位情况">
          <el-input v-model="f.returnVisit.seatSituation" type="textarea" :rows="2"
            placeholder="如：第5排靠窗边，座位轮换较少 / 已调至第3排正中" />
        </el-form-item>
        <el-form-item label="作业/用眼情况">
          <el-input v-model="f.returnVisit.homeworkEyeUse" type="textarea" :rows="2"
            placeholder="每日作业时长、近距离用眼、电子产品、读写姿势等" />
        </el-form-item>
        <el-form-item label="户外活动情况">
          <el-input v-model="f.returnVisit.outdoorActivity" type="textarea" :rows="2"
            placeholder="每日户外时长、课间远眺、体育课等" />
        </el-form-item>
        <el-form-item label="家长当前态度">
          <el-select v-model="f.returnVisit.parentAttitude" style="width:100%">
            <el-option v-for="a in attitudes" :key="a.value" :label="a.label" :value="a.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="['STILL_REFUSE','HESITATE','UNREACHABLE'].includes(f.returnVisit.parentAttitude)"
                      label="下次回访约定">
          <el-date-picker v-model="f.returnVisit.nextPlannedReturnDate" value-format="YYYY-MM-DD" style="width:100%"
            placeholder="家长仍拒绝/犹豫时，约定下一次回访日期" />
        </el-form-item>
        <el-form-item label="沟通人员">
          <el-input v-model="f.returnVisit.communicator" placeholder="留空取当前登录人" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="f.returnVisit.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.returnVisit=false">取消</el-button>
        <el-button type="primary" @click="submit('returnVisit', api.refusalReturnVisit)">提交回访记录</el-button>
      </template>
    </el-dialog>

    <!-- 3. 班主任/社区医生共同沟通 -->
    <el-dialog v-model="visible.joint" title="申请多方共同沟通" width="640px">
      <el-alert type="error" :closable="false" style="margin-bottom:12px"
        title="适用于家长连续拒绝复诊、且筛查结果持续下降（风险升高）的学生；沟通结果与学校干预措施都要可追溯。" />
      <el-form :model="f.joint" label-width="120px">
        <el-form-item label="申请人">
          <el-input v-model="f.joint.jointApplicant" placeholder="如：学校（健康老师 李校医），留空取当前登录人" />
        </el-form-item>
        <el-form-item label="共同沟通参与方">
          <el-input v-model="f.joint.participants" type="textarea" :rows="2"
            placeholder="如：三班班主任 陈老师、阳光社区卫生服务中心 社区医生吴医生" />
        </el-form-item>
        <el-form-item label="沟通时间">
          <el-date-picker v-model="f.joint.jointCommTime" type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%"/>
        </el-form-item>
        <el-form-item label="沟通结果">
          <el-input v-model="f.joint.jointResult" type="textarea" :rows="3"
            placeholder="家长是否转变态度、是否同意复诊及时间安排等（必填）" />
        </el-form-item>
        <el-form-item label="学校干预措施">
          <el-input v-model="f.joint.schoolMeasures" type="textarea" :rows="3"
            placeholder="座位调整、户外活动安排、用眼指导、持续提醒等；同时写入学校干预台账" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="f.joint.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.joint=false">取消</el-button>
        <el-button type="warning" @click="submit('joint', api.refusalJointComm)">登记共同沟通</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const props = defineProps({ studentId: [String, Number] })
const emit = defineEmits(['done'])

const reasons = [
  { value: 'EYE_FATIGUE', label: '只是疲劳用眼，休息即可' },
  { value: 'PSEUDO_MYOPIA', label: '认为是假性近视，无需就诊' },
  { value: 'TOO_YOUNG', label: '孩子年龄小，不急着看' },
  { value: 'TIME_CONFLICT', label: '家长工作忙/没时间陪同' },
  { value: 'COST', label: '担心就诊/配镜费用' },
  { value: 'DISTANCE', label: '医院距离远、就诊不便' },
  { value: 'ALREADY_WEARING', label: '已配镜/在外面看过，无需复诊' },
  { value: 'OTHER', label: '其他原因' }
]
const attitudes = [
  { value: 'STILL_REFUSE', label: '仍坚持拒绝' },
  { value: 'HESITATE', label: '犹豫、开始重视' },
  { value: 'WILLING', label: '愿意安排复诊' },
  { value: 'ALREADY_VISITED', label: '已自行复诊' },
  { value: 'UNREACHABLE', label: '本次未能联系上' }
]

const today = () => new Date().toISOString().slice(0, 10)
const blank = () => ({
  refusal: { refusalReason: 'EYE_FATIGUE', refusalReasonDetail: '', riskNotice: '',
    communicator: '', plannedReturnDate: '', remark: '' },
  returnVisit: { actualReturnDate: today(), seatSituation: '', homeworkEyeUse: '', outdoorActivity: '',
    parentAttitude: 'STILL_REFUSE', nextPlannedReturnDate: '', communicator: '', remark: '' },
  joint: { jointApplicant: '', participants: '', jointCommTime: today() + 'T16:00:00',
    jointResult: '', schoolMeasures: '', remark: '' }
})
const f = reactive(blank())
const visible = reactive({})
const targetNodeId = ref(null)

const open = (name, nodeId = null) => {
  targetNodeId.value = nodeId
  const d = blank()
  Object.keys(f).forEach(k => Object.assign(f[k], d[k]))
  visible[name] = true
}
defineExpose({ open })

const submit = async (name, call) => {
  if (!props.studentId) { ElMessage.error('缺少学生信息'); return }
  const payload = { studentId: props.studentId, ...f[name] }
  if (name === 'returnVisit' && targetNodeId.value) payload.nodeId = targetNodeId.value
  try {
    await call(payload)
    visible[name] = false
    ElMessage.success('已保存到该学生拒诊跟进档案')
    emit('done')
  } catch (e) { /* 拦截器已提示 */ }
}
</script>
