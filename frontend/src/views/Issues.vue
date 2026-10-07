<template>
  <div class="page-container">
    <el-card>
      <el-alert type="error" :closable="false" style="margin-bottom:10px"
        title="异常事项是“筛完无人管”的防线：请假、电话不通、导入错误、复诊差异、外配眼镜、补助争议、配镜不适、复查未到场、家长拒绝复诊、连续拒绝风险升高，每一项都必须跟进到闭环——拒绝复诊绝不能简单标成“已处理”。" />
      <el-radio-group v-model="status" style="margin-bottom:10px" @change="load">
        <el-radio-button value="OPEN">待处理</el-radio-button>
        <el-radio-button value="RESOLVED">已闭环</el-radio-button>
        <el-radio-button value="ALL">全部</el-radio-button>
      </el-radio-group>
      <el-table :data="rows" stripe size="small">
        <el-table-column prop="issueTypeLabel" label="异常类型" width="150" />
        <el-table-column prop="studentName" label="学生" width="90" />
        <el-table-column prop="schoolName" label="学校" min-width="160" />
        <el-table-column prop="description" label="情况描述" min-width="220" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status==='RESOLVED'?'success':'danger'">
              {{ row.status === 'RESOLVED' ? '已闭环' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提出/闭环" min-width="200">
          <template #default="{ row }">
            <div class="muted">{{ row.raisedBy }} @ {{ fmt(row.raisedAt) }}</div>
            <div v-if="row.status==='RESOLVED'" class="muted">
              {{ row.resolvedBy }} @ {{ fmt(row.resolvedAt) }}：{{ row.resolveNote }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="row.status==='OPEN'" type="primary" size="small" @click="resolve(row)">闭环处理</el-button>
            <el-button link size="small" @click="detail(row)">学生档案</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { api } from '../api'

const router = useRouter()
const rows = ref([])
const status = ref('OPEN')

const load = async () => { rows.value = await api.issues(status.value) }
const fmt = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''
const resolve = (row) => {
  ElMessageBox.prompt('请填写处理/闭环说明', '闭环异常事项', {
    confirmButtonText: '确认闭环', cancelButtonText: '取消', inputType: 'textarea'
  }).then(async ({ value }) => {
    await api.resolveIssue({ issueId: row.id, resolveNote: value || '已处理' })
    ElMessage.success('事项已闭环')
    load()
  }).catch(() => {})
}
const detail = (row) => router.push(`/students/${row.studentId}`)
onMounted(load)
</script>
