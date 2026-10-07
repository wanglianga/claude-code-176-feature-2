<template>
  <div class="page-container">
    <el-alert type="warning" :closable="false" style="margin-bottom:12px"
      title="家长以“只是疲劳用眼”等理由拒绝复诊时，拒绝不能简单标成“已处理”：要留痕原因、风险告知、确认时间与沟通人员，在约定时间回访登记座位/作业用眼/户外/家长态度；筛查继续下降会带出原拒诊记录重新提醒，再次拒绝形成新节点，连续拒绝可申请班主任/社区医生共同沟通。" />
    <el-card>
      <el-tabs v-model="tab" @tab-change="load">
        <!-- 1. 拒诊跟进中 -->
        <el-tab-pane name="active">
          <template #label>拒诊跟进中 <el-badge :value="activeRows.length" type="danger" style="margin-left:6px" :hidden="!activeRows.length" /></template>
          <el-table :data="activeRows" stripe size="small">
            <el-table-column prop="name" label="姓名" width="80" />
            <el-table-column label="学校/班级" min-width="200">
              <template #default="{ row }">{{ row.schoolName }} {{ row.grade }}{{ row.className }}</template>
            </el-table-column>
            <el-table-column label="家长/电话" min-width="150">
              <template #default="{ row }">{{ row.parentName }} {{ row.parentPhone }}</template>
            </el-table-column>
            <el-table-column label="拒绝次数" width="90">
              <template #default="{ row }">
                <el-tag :type="row.refusalCount>=2?'danger':'warning'" size="small">{{ row.refusalCount }} 次</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.highRisk" type="danger" size="small">连续拒绝·风险升高</el-tag>
                <span v-else class="muted">跟进中</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastNodeTypeLabel" label="最近节点" width="110" />
            <el-table-column label="约定回访" width="110">
              <template #default="{ row }">
                <span v-if="row.dueDate">{{ row.dueDate }}</span>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="pick(row,'returnVisit')">发起回访</el-button>
                <el-button size="small" type="danger" plain @click="pick(row,'refusal')">记录再次拒绝</el-button>
                <el-button size="small" type="warning" plain @click="pick(row,'joint')">共同沟通</el-button>
                <el-button link size="small" @click="detail(row)">档案</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!activeRows.length" description="暂无拒诊跟进中的学生" :image-size="70" />
        </el-tab-pane>

        <!-- 2. 到约定时间/逾期待回访 -->
        <el-tab-pane name="due">
          <template #label>到期待回访 <el-badge :value="dueRows.length" type="danger" style="margin-left:6px" :hidden="!dueRows.length" /></template>
          <el-alert type="info" :closable="false" style="margin-bottom:10px"
            title="以下学生已到（或超过）与家长约定的回访时间，请健康老师及时发起回访，并与前一次筛查结果比较。" />
          <el-table :data="dueRows" stripe size="small">
            <el-table-column prop="studentName" label="姓名" width="80" />
            <el-table-column prop="schoolName" label="学校" min-width="170" />
            <el-table-column label="家长/电话" min-width="150">
              <template #default="{ row }">{{ row.parentName }} {{ row.parentPhone }}</template>
            </el-table-column>
            <el-table-column prop="nodeTypeLabel" label="约定来源节点" width="120" />
            <el-table-column label="应回访日期" width="130">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.nextPlannedReturnDate || row.plannedReturnDate }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="pickDue(row)">发起回访</el-button>
                <el-button link size="small" @click="detail({id:row.studentId})">档案</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!dueRows.length" description="暂无到期待回访" :image-size="70" />
        </el-tab-pane>

        <!-- 3. 连续拒绝风险升高：共同沟通 -->
        <el-tab-pane name="joint">
          <template #label>共同沟通候选 <el-badge :value="jointRows.length" type="warning" style="margin-left:6px" :hidden="!jointRows.length" /></template>
          <el-alert type="error" :closable="false" style="margin-bottom:10px"
            title="这些学生家长已连续拒绝复诊且筛查持续下降（风险升高），学校可申请班主任或社区医生共同沟通，沟通结果与学校干预措施都记录在该生档案中。" />
          <el-table :data="jointRows" stripe size="small">
            <el-table-column prop="name" label="姓名" width="80" />
            <el-table-column label="学校/班级" min-width="200">
              <template #default="{ row }">{{ row.schoolName }} {{ row.grade }}{{ row.className }}</template>
            </el-table-column>
            <el-table-column label="家长/电话" min-width="150">
              <template #default="{ row }">{{ row.parentName }} {{ row.parentPhone }}</template>
            </el-table-column>
            <el-table-column label="拒绝次数" width="90">
              <template #default="{ row }"><el-tag type="danger" size="small">{{ row.refusalCount }} 次</el-tag></template>
            </el-table-column>
            <el-table-column label="已共同沟通" width="100">
              <template #default="{ row }">{{ row.jointCommCount }} 次</template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" type="warning" @click="pick(row,'joint')">申请/登记共同沟通</el-button>
                <el-button link size="small" @click="detail(row)">档案</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!jointRows.length" description="暂无连续拒绝且风险升高的学生" :image-size="70" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <refusal-dialogs ref="dialogs" :student-id="sid" @done="load" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import RefusalDialogs from '../components/RefusalDialogs.vue'

const router = useRouter()
const tab = ref('active')
const activeRows = ref([])
const dueRows = ref([])
const jointRows = ref([])
const sid = ref(null)
const dialogs = ref()

const load = async () => {
  if (tab.value === 'active') activeRows.value = await api.refusalStudents()
  if (tab.value === 'due') dueRows.value = await api.refusalDue()
  if (tab.value === 'joint') jointRows.value = await api.refusalJointCandidates()
}
const loadAll = async () => {
  const [a, d, j] = await Promise.all([
    api.refusalStudents(), api.refusalDue(), api.refusalJointCandidates()
  ])
  activeRows.value = a; dueRows.value = d; jointRows.value = j
}
const pick = (row, name) => { sid.value = row.studentId || row.id; dialogs.value.open(name) }
const pickDue = (row) => { sid.value = row.studentId; dialogs.value.open('returnVisit', row.id) }
const detail = (row) => router.push(`/students/${row.studentId || row.id}`)

onMounted(loadAll)
</script>
