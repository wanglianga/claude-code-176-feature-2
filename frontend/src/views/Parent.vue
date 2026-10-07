<template>
  <div class="page-container">
    <el-card>
      <el-radio-group v-model="stage" style="margin-bottom:10px" @change="load">
        <el-radio-button value="AWAIT_PARENT">待家长确认</el-radio-button>
        <el-radio-button value="LOST_FOLLOW">电话不通/失访</el-radio-button>
        <el-radio-button value="REFUSAL_FOLLOW">拒绝复诊跟进中</el-radio-button>
        <el-radio-button value="DEFERRED">家长暂缓(旧)</el-radio-button>
      </el-radio-group>
      <el-table :data="rows" stripe size="small">
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column label="学校/班级" min-width="190">
          <template #default="{ row }">{{ row.schoolName }} {{ row.grade }}{{ row.className }}</template>
        </el-table-column>
        <el-table-column label="家长电话" width="150">
          <template #default="{ row }">
            {{ row.parentPhone }}
            <el-icon v-if="row.flagLabels.includes('家长电话无法接通')" color="#f56c6c"><Warning /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="异常标记" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="(f,i) in row.flagLabels" :key="i" type="danger" size="small" style="margin:2px">{{ f }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="pick(row,'notify')">记录告知</el-button>
            <el-button size="small" type="primary" @click="pick(row,'choice')">家长确认选择</el-button>
            <el-button link size="small" @click="detail(row)">档案</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <action-dialogs ref="dialogs" :student-id="sid" @done="load" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import ActionDialogs from '../components/ActionDialogs.vue'

const router = useRouter()
const rows = ref([])
const stage = ref('AWAIT_PARENT')
const sid = ref(null)
const dialogs = ref()
const load = async () => { rows.value = await api.students({ stage: stage.value }) }
const pick = (row, name) => { sid.value = row.id; dialogs.value.open(name) }
const detail = (row) => router.push(`/students/${row.id}`)
onMounted(load)
</script>
