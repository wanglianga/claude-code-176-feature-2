<template>
  <div>
    <!-- 1. 现场筛查 -->
    <el-dialog v-model="visible.screen" title="现场筛查记录" width="680px">
      <el-form :model="f.screen" label-width="130px">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="筛查日期"><el-date-picker v-model="f.screen.screenDate" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="设备校准编号"><el-input v-model="f.screen.deviceCalibNo" placeholder="如 DEV-2026-0091"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="裸眼视力 左"><el-input v-model="f.screen.nakedVisionLeft" placeholder="如 4.6"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="裸眼视力 右"><el-input v-model="f.screen.nakedVisionRight" placeholder="如 4.7"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="球镜 左(D)"><el-input v-model="f.screen.sphereLeft" placeholder="如 -1.50"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="球镜 右(D)"><el-input v-model="f.screen.sphereRight"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="散光柱镜 左"><el-input v-model="f.screen.cylinderLeft"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="散光柱镜 右"><el-input v-model="f.screen.cylinderRight"/></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="是否戴镜"><el-switch v-model="f.screen.wearingGlasses"/></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需要复测"><el-switch v-model="f.screen.retestNeeded"/></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用眼习惯问卷"><el-input v-model="f.screen.habitSummary" type="textarea"
              :rows="2" placeholder="每日近距离用眼时长/电子产品/户外活动/读写姿势"/></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="筛查机构/操作人">
              <el-input v-model="f.screen.orgName" placeholder="机构" style="width:48%"/>
              <el-input v-model="f.screen.operatorName" placeholder="操作人" style="width:48%;margin-left:4%"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-alert type="warning" :closable="false"
              title="若学生当日请假未检，请勾选下方——平台将登记“筛查请假”异常事项并进入失访跟进，待补检后闭环。" />
            <el-form-item label="请假未检" style="margin-top:8px"><el-switch v-model="f.screen.absent"/></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible.screen=false">取消</el-button>
        <el-button type="primary" @click="submit('screen', api.screen)">提交筛查</el-button>
      </template>
    </el-dialog>

    <!-- 2. 学校复核 -->
    <el-dialog v-model="visible.review" title="学校健康老师复核" width="520px">
      <el-form label-width="90px">
        <el-form-item label="复核结论">
          <el-radio-group v-model="f.review.result">
            <el-radio value="CONFIRM">确认异常</el-radio>
            <el-radio value="REJECT">误判排除</el-radio>
            <el-radio value="RETEST">建议复测</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="复核意见"><el-input v-model="f.review.opinion" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.review=false">取消</el-button>
        <el-button type="primary" @click="submit('review', api.schoolReview)">提交复核</el-button>
      </template>
    </el-dialog>

    <!-- 3. 家长告知 -->
    <el-dialog v-model="visible.notify" title="家长告知" width="520px">
      <el-form label-width="110px">
        <el-form-item label="告知方式">
          <el-select v-model="f.notify.channel" style="width:100%">
            <el-option label="短信" value="短信" /><el-option label="电话" value="电话" />
            <el-option label="APP" value="APP" /><el-option label="短信+APP" value="短信+APP" />
            <el-option label="家长会告知" value="家长会告知" />
          </el-select>
        </el-form-item>
        <el-form-item label="本次未接通">
          <el-switch v-model="f.notify.unreachable" />
          <span class="muted" style="margin-left:8px">勾选后累计无法接通次数并登记异常事项</span>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="f.notify.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.notify=false">取消</el-button>
        <el-button type="primary" @click="submit('notify', api.notify)">记录告知</el-button>
      </template>
    </el-dialog>

    <!-- 4. 家长确认选择 -->
    <el-dialog v-model="visible.choice" title="家长确认与处置选择" width="600px">
      <el-form label-width="110px">
        <el-form-item label="处置选择">
          <el-radio-group v-model="f.choice.choice">
            <el-radio value="COUNTY_HOSPITAL">县医院复诊</el-radio>
            <el-radio value="PARTNER_CLINIC">合作眼科门诊复诊</el-radio>
            <el-radio value="DEFER">暂缓/拒绝复诊</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="f.choice.choice !== 'DEFER'" label="复诊机构">
          <el-input v-model="f.choice.targetOrg" placeholder="如 阳光县人民医院眼科 / 爱眼眼科门诊"/>
        </el-form-item>
        <template v-if="f.choice.choice === 'DEFER'">
          <el-alert type="warning" :closable="false" style="margin-bottom:10px"
            title="家长拒绝/暂缓复诊不能简单标为“已处理”：平台将建立拒诊跟进节点，记录拒绝原因、风险告知、沟通人员，并安排健康老师回访。" />
          <el-form-item label="拒绝原因归类">
            <el-select v-model="f.choice.reasonType" style="width:100%">
              <el-option v-for="(t,k) in reasonTypes" :key="k" :label="t" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="家长原话/原因">
            <el-input v-model="f.choice.deferReason" type="textarea" :rows="2" placeholder="必填，如：孩子就是最近作业多看手机多，休息一下就好，不用去医院"/>
          </el-form-item>
          <el-form-item label="风险告知内容">
            <el-input v-model="f.choice.riskNotice" type="textarea" :rows="4"/>
          </el-form-item>
          <el-form-item label="沟通人员">
            <el-input v-model="f.choice.communicator" placeholder="与家长沟通的健康老师/班主任姓名"/>
          </el-form-item>
          <el-form-item label="约定回访日期">
            <el-date-picker v-model="f.choice.plannedCallbackDate" value-format="YYYY-MM-DD" style="width:100%"/>
          </el-form-item>
          <el-form-item label="家长知情确认">
            <el-checkbox v-model="f.choice.riskAcknowledged">家长已知悉上述风险，仍坚持暂不带孩子复诊</el-checkbox>
          </el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="f.choice.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.choice=false">取消</el-button>
        <el-button type="primary" @click="submit('choice', api.parentChoice)">提交家长选择</el-button>
      </template>
    </el-dialog>

    <!-- 4b. 登记家长拒绝复诊（再次拒绝/手工登记，形成新节点） -->
    <el-dialog v-model="visible.refusal" title="登记家长拒绝复诊（跟进节点）" width="620px">
      <el-alert type="warning" :closable="false" style="margin-bottom:10px"
        title="每次拒绝都会形成一条新的跟进节点，不会覆盖上一次记录。请完整记录原因、风险告知与沟通人员。" />
      <el-form label-width="120px">
        <el-form-item label="节点来源">
          <el-select v-model="f.refusal.triggerSource" style="width:100%">
            <el-option label="首次告知后拒绝" value="INITIAL_NOTIFY" />
            <el-option label="后续筛查下降重新提醒" value="RESCREEN_DROP" />
            <el-option label="回访后仍拒绝" value="CALLBACK_AFTER" />
            <el-option label="学校手工登记" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="拒绝原因归类">
          <el-select v-model="f.refusal.reasonType" style="width:100%">
            <el-option v-for="(t,k) in reasonTypes" :key="k" :label="t" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="家长原话/原因">
          <el-input v-model="f.refusal.reasonDetail" type="textarea" :rows="2" placeholder="必填"/>
        </el-form-item>
        <el-form-item label="风险告知内容">
          <el-input v-model="f.refusal.riskNotice" type="textarea" :rows="4"/>
        </el-form-item>
        <el-form-item label="沟通方式">
          <el-input v-model="f.refusal.contactChannel" placeholder="电话/面访/家长会/APP"/>
        </el-form-item>
        <el-form-item label="沟通人员">
          <el-input v-model="f.refusal.communicator" placeholder="必填"/>
        </el-form-item>
        <el-form-item label="约定回访日期">
          <el-date-picker v-model="f.refusal.plannedCallbackDate" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="家长知情确认">
          <el-checkbox v-model="f.refusal.riskAcknowledged">家长已知悉上述风险，仍坚持暂不带孩子复诊</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.refusal=false">取消</el-button>
        <el-button type="danger" @click="submit('refusal', api.refusalRecord)">登记拒绝节点</el-button>
      </template>
    </el-dialog>

    <!-- 4c. 健康老师约定时间回访 -->
    <el-dialog v-model="visible.callback" title="健康回访（约定时间跟进）" width="640px">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        title="回访内容与历次筛查结果保存在同一份学生档案中比对。家长仍拒绝时，将形成新的拒诊节点而非覆盖原记录。" />
      <el-form label-width="120px">
        <el-form-item label="回访人">
          <el-input v-model="f.callback.callbackStaff" placeholder="健康老师姓名"/>
        </el-form-item>
        <el-form-item label="孩子座位情况">
          <el-input v-model="f.callback.seatSituation" type="textarea" :rows="2"
            placeholder="如：现坐第4排靠窗边，居中，每月轮换；看黑板偶尔眯眼"/>
        </el-form-item>
        <el-form-item label="作业/用眼情况">
          <el-input v-model="f.callback.homeworkEyeUse" type="textarea" :rows="2"
            placeholder="如：每晚作业约2.5小时，姿势偏近，周末看手机约1小时"/>
        </el-form-item>
        <el-form-item label="户外活动">
          <el-input v-model="f.callback.outdoorActivity" type="textarea" :rows="2"
            placeholder="如：在校大课间+体育课约1小时，周末基本不外出"/>
        </el-form-item>
        <el-form-item label="家长当前态度">
          <el-select v-model="f.callback.parentAttitude" style="width:100%">
            <el-option v-for="(t,k) in attitudes" :key="k" :label="t" :value="k" />
          </el-select>
        </el-form-item>
        <template v-if="f.callback.parentAttitude==='STILL_REFUSE'">
          <el-alert type="error" :closable="false" style="margin-bottom:10px"
            title="家长回访时仍拒绝复诊，请再次记录风险告知、拒绝原因并取得知情确认。" />
          <el-form-item label="拒绝原因归类">
            <el-select v-model="f.callback.reasonType" style="width:100%">
              <el-option v-for="(t,k) in reasonTypes" :key="k" :label="t" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="本次拒绝原话">
            <el-input v-model="f.callback.reasonDetail" type="textarea" :rows="2"/>
          </el-form-item>
          <el-form-item label="再次风险告知">
            <el-input v-model="f.callback.riskNotice" type="textarea" :rows="3"/>
          </el-form-item>
          <el-form-item label="家长知情确认">
            <el-checkbox v-model="f.callback.riskAcknowledged">家长已知悉风险仍坚持拒绝</el-checkbox>
          </el-form-item>
        </template>
        <el-form-item v-if="f.callback.parentAttitude!=='AGREED'" label="下次回访日期">
          <el-date-picker v-model="f.callback.nextCallbackDate" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="f.callback.remark" type="textarea" :rows="2"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.callback=false">取消</el-button>
        <el-button type="primary" @click="submit('callback', api.refusalCallback)">提交回访</el-button>
      </template>
    </el-dialog>

    <!-- 4d. 申请班主任/社区医生共同沟通 -->
    <el-dialog v-model="visible.joint" title="申请多方共同沟通" width="560px">
      <el-alert type="warning" :closable="false" style="margin-bottom:10px"
        title="适用于家长连续拒绝复诊且风险持续升高。沟通结果与学校干预措施将记录并可追溯到该学生。" />
      <el-form label-width="120px">
        <el-form-item label="共同沟通对象">
          <el-radio-group v-model="f.joint.commType">
            <el-radio value="HEAD_TEACHER">班主任</el-radio>
            <el-radio value="COMMUNITY_DOCTOR">社区医生</el-radio>
            <el-radio value="BOTH">班主任+社区医生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="班主任姓名">
          <el-input v-model="f.joint.headTeacherName"/>
        </el-form-item>
        <el-form-item label="社区医生/机构">
          <el-input v-model="f.joint.communityDoctorName" placeholder="如：城东社区卫生服务中心 王医生"/>
        </el-form-item>
        <el-form-item label="计划沟通时间">
          <el-date-picker v-model="f.joint.plannedTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择日期时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="沟通方式">
          <el-input v-model="f.joint.channel" placeholder="家访/电话/三方会谈"/>
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="f.joint.applyReason" type="textarea" :rows="3"
            placeholder="如：家长已连续2次拒绝，本次筛查视力继续下降，需班主任与社区医生共同动员"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.joint=false">取消</el-button>
        <el-button type="warning" @click="submit('joint', api.jointApply)">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 5. 复诊处方 -->
    <el-dialog v-model="visible.visit" title="复诊结果与处方回写" width="720px">
      <el-form :model="f.visit" label-width="130px">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="复诊机构"><el-input v-model="f.visit.orgName"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="医生"><el-input v-model="f.visit.doctor"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="复诊日期"><el-date-picker v-model="f.visit.visitDate" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="瞳距(mm)"><el-input v-model="f.visit.pd"/></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="真性近视"><el-switch v-model="f.visit.trueMyopia"/></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需要散瞳"><el-switch v-model="f.visit.needMydriasis"/></el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="球镜左"><el-input v-model="f.visit.rxSphereLeft"/></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="球镜右"><el-input v-model="f.visit.rxSphereRight"/></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="柱镜左"><el-input v-model="f.visit.rxCylinderLeft"/></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="柱镜右"><el-input v-model="f.visit.rxCylinderRight"/></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="轴位左"><el-input v-model="f.visit.rxAxisLeft"/></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="轴位右"><el-input v-model="f.visit.rxAxisRight"/></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="复查周期(月)"><el-input-number v-model="f.visit.reviewCycleMonths" :min="1" :max="24"/></el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="诊断结论"><el-input v-model="f.visit.diagnosis" type="textarea" :rows="2"/></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="符合配镜补助"><el-switch v-model="f.visit.subsidyEligible"/></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="与筛查差异大"><el-switch v-model="f.visit.bigDifference"/></el-form-item>
          </el-col>
          <el-col :span="24" v-if="f.visit.bigDifference">
            <el-form-item label="差异说明"><el-input v-model="f.visit.differenceNote" type="textarea" :rows="2"
              placeholder="平台将登记“复诊与筛查差异较大”异常事项，供卫健核对设备与判定"/></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible.visit=false">取消</el-button>
        <el-button type="success" @click="submit('visit', api.visit)">回写复诊结果</el-button>
      </template>
    </el-dialog>

    <!-- 6. 配镜 -->
    <el-dialog v-model="visible.glasses" title="配镜登记" width="580px">
      <el-form label-width="120px">
        <el-form-item label="配镜机构"><el-input v-model="f.glasses.opticianOrg"/></el-form-item>
        <el-form-item label="配镜日期"><el-date-picker v-model="f.glasses.glassesDate" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="机构性质">
          <el-radio-group v-model="f.glasses.partnerOptician">
            <el-radio :value="true">合作机构</el-radio>
            <el-radio :value="false">家长自行外配</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额(元)"><el-input-number v-model="f.glasses.amount" :min="0" :precision="2"/></el-form-item>
        <el-form-item label="镜片/处方信息"><el-input v-model="f.glasses.lensInfo" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="配镜后不适"><el-switch v-model="f.glasses.discomfort"/></el-form-item>
        <el-form-item v-if="f.glasses.discomfort" label="不适描述">
          <el-input v-model="f.glasses.discomfortDetail" type="textarea" :rows="2"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.glasses=false">取消</el-button>
        <el-button type="success" @click="submit('glasses', api.glasses)">登记配镜</el-button>
      </template>
    </el-dialog>

    <!-- 7. 补助申请 -->
    <el-dialog v-model="visible.subsidy" title="配镜补助申请" width="480px">
      <el-form label-width="110px">
        <el-form-item label="申请金额(元)"><el-input-number v-model="f.subsidy.applyAmount" :min="0" :precision="2"/></el-form-item>
        <el-form-item label="说明"><el-input v-model="f.subsidy.opinion" type="textarea" :rows="3"
          placeholder="外配眼镜需在此说明验光凭证情况"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.subsidy=false">取消</el-button>
        <el-button type="warning" @click="submit('subsidy', api.subsidyApply)">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 8. 复查随访 -->
    <el-dialog v-model="visible.followup" title="复查随访登记" width="520px">
      <el-form label-width="100px">
        <el-form-item label="处理">
          <el-radio-group v-model="f.followup.action">
            <el-radio value="DONE">已完成复查</el-radio>
            <el-radio value="NO_SHOW">未到场（催办）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="f.followup.action==='DONE'" label="实际复查日期">
          <el-date-picker v-model="f.followup.actualDate" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item v-else label="计划日期">
          <el-date-picker v-model="f.followup.plannedDate" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="复查机构"><el-input v-model="f.followup.orgName"/></el-form-item>
        <el-form-item v-if="f.followup.action==='DONE'" label="视力结果"><el-input v-model="f.followup.visionResult"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="f.followup.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.followup=false">取消</el-button>
        <el-button type="warning" @click="submit('followup', api.followup)">提交</el-button>
      </template>
    </el-dialog>

    <!-- 9. 学校干预 -->
    <el-dialog v-model="visible.intervention" title="学校日常干预登记" width="520px">
      <el-form label-width="100px">
        <el-form-item label="干预类型">
          <el-select v-model="f.intervention.type" style="width:100%">
            <el-option label="座位调整" value="SEAT_ADJUST" />
            <el-option label="户外活动" value="OUTDOOR_ACTIVITY" />
            <el-option label="家长提醒" value="PARENT_REMIND" />
            <el-option label="用眼行为指导" value="BEHAVIOR_GUIDE" />
            <el-option label="其他干预" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行日期"><el-date-picker v-model="f.intervention.execDate" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="干预内容"><el-input v-model="f.intervention.content" type="textarea" :rows="3"
          placeholder="如：调整至前排座位 / 每日2小时户外活动 / 向家长推送用眼提醒"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible.intervention=false">取消</el-button>
        <el-button type="info" @click="submit('intervention', api.intervention)">登记干预</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const props = defineProps({ studentId: [String, Number] })
const emit = defineEmits(['done'])

const visible = reactive({})
const today = new Date().toISOString().slice(0, 10)

const standardRisk = '已告知家长：孩子裸眼视力/屈光已异常，若只是疲劳用眼或假性近视，需经医院散瞳等检查才能区分；拖延复诊可能错过最佳干预期，导致近视快速加深及高度近视、视网膜病变等风险；建议尽快到县医院眼科或合作眼科门诊复诊确认。'

const reasonTypes = {
  FATIGUE: '认为只是疲劳用眼/假性近视',
  TEMPORARY: '认为过段时间会自行恢复',
  NO_TIME: '家长工作忙、没时间',
  COST: '担心费用',
  DISTRUST: '不信任筛查结果或医院',
  ALREADY_GLASSES: '已自行配镜，认为无需复诊',
  OTHER: '其他原因'
}
const attitudes = {
  STILL_REFUSE: '仍拒绝复诊',
  HESITATE: '犹豫，继续动员',
  WILL_OBSERVE: '愿意先调整用眼习惯再观察',
  AGREED: '已同意复诊',
  UNREACHABLE: '仍联系不上'
}
// 两周后的约定回访日期
const inTwoWeeks = () => new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10)

const blank = () => ({
  screen: { screenDate: today, orgName: '阳光县视光中心', operatorName: '', deviceCalibNo: '',
    nakedVisionLeft: '', nakedVisionRight: '', sphereLeft: '', sphereRight: '',
    cylinderLeft: '', cylinderRight: '', wearingGlasses: false, habitSummary: '',
    retestNeeded: false, absent: false },
  review: { result: 'CONFIRM', opinion: '' },
  notify: { channel: '电话', unreachable: false, remark: '' },
  choice: { choice: 'COUNTY_HOSPITAL', targetOrg: '阳光县人民医院眼科', deferReason: '',
    reasonType: 'FATIGUE', riskNotice: standardRisk, communicator: '', plannedCallbackDate: inTwoWeeks(),
    riskAcknowledged: false, remark: '' },
  refusal: { triggerSource: 'MANUAL', reasonType: 'FATIGUE', reasonDetail: '',
    riskNotice: standardRisk, contactChannel: '电话', communicator: '',
    plannedCallbackDate: inTwoWeeks(), riskAcknowledged: false, remark: '' },
  callback: { callbackStaff: '', seatSituation: '', homeworkEyeUse: '', outdoorActivity: '',
    parentAttitude: 'HESITATE', reasonType: 'FATIGUE', reasonDetail: '', riskNotice: standardRisk,
    riskAcknowledged: false, nextCallbackDate: inTwoWeeks(), remark: '' },
  joint: { commType: 'HEAD_TEACHER', headTeacherName: '', communityDoctorName: '', plannedTime: '',
    channel: '家访', applyReason: '' },
  visit: { visitDate: today, orgName: '阳光县人民医院眼科', doctor: '', trueMyopia: true,
    needMydriasis: false, rxSphereLeft: '', rxSphereRight: '', rxCylinderLeft: '', rxCylinderRight: '',
    rxAxisLeft: '', rxAxisRight: '', pd: '', diagnosis: '', reviewCycleMonths: 6,
    subsidyEligible: true, bigDifference: false, differenceNote: '' },
  glasses: { glassesDate: today, opticianOrg: '', partnerOptician: true, amount: 600,
    lensInfo: '', discomfort: false, discomfortDetail: '' },
  subsidy: { applyAmount: 300, opinion: '' },
  followup: { action: 'DONE', plannedDate: today, actualDate: today, orgName: '', visionResult: '', remark: '' },
  intervention: { type: 'SEAT_ADJUST', execDate: today, content: '' }
})
const f = reactive(blank())

const open = (name) => {
  // 重置为默认值
  const d = blank()
  Object.keys(f).forEach(k => { Object.assign(f[k], d[k]) })
  visible[name] = true
}
defineExpose({ open })

const submit = async (name, call) => {
  try {
    const payload = { studentId: props.studentId, ...f[name] }
    await call(payload)
    visible[name] = false
    ElMessage.success('提交成功')
    emit('done')
  } catch (e) { /* 拦截器已提示 */ }
}
</script>
