package com.county.vision.service;

import com.county.vision.common.BusinessException;
import com.county.vision.domain.entity.*;
import com.county.vision.domain.enums.*;
import com.county.vision.domain.repository.*;
import com.county.vision.dto.RecordDtos;
import com.county.vision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 家长拒绝复诊后的持续跟进：
 *
 * <ol>
 *   <li>拒绝登记：记录拒绝原因（归因+原话）、风险告知内容、家长知情确认、确认时间、沟通人员，
 *       绝不能简单标成“已处理”；每次拒绝都是一条 append-only 新节点。</li>
 *   <li>健康回访：健康老师在约定时间回访，记录座位、作业用眼、户外活动、家长当前态度，
 *       与上一次筛查结果放在同一档案比较。</li>
 *   <li>后续筛查继续下降：带出原拒诊记录与风险提示，重新生成复诊提醒，形成新节点。</li>
 *   <li>连续拒绝且风险升高：学校申请班主任/社区医生共同沟通，沟通结果与学校干预措施可追溯。</li>
 * </ol>
 */
@Service
public class RefusalService {

    /** 标准风险告知模板（健康老师可在此基础上增减） */
    public static final String STANDARD_RISK_NOTICE =
            "已告知家长：孩子裸眼视力/屈光已异常，若只是疲劳用眼或假性近视，需经医院散瞳等检查才能区分；"
            + "拖延复诊可能错过最佳干预期，导致近视快速加深、高度近视及视网膜病变等并发症风险；"
            + "建议尽快到县医院眼科或合作眼科门诊复诊确认。";

    private final StudentRepository studentRepo;
    private final ScreenRecordRepository screenRepo;
    private final ParentConfirmationRepository confirmRepo;
    private final RefusalFollowUpRepository refusalRepo;
    private final JointCommunicationRepository jointRepo;
    private final InterventionRepository interventionRepo;
    private final TimelineService timeline;

    public RefusalService(StudentRepository studentRepo, ScreenRecordRepository screenRepo,
                         ParentConfirmationRepository confirmRepo, RefusalFollowUpRepository refusalRepo,
                         JointCommunicationRepository jointRepo, InterventionRepository interventionRepo,
                         TimelineService timeline) {
        this.studentRepo = studentRepo;
        this.screenRepo = screenRepo;
        this.confirmRepo = confirmRepo;
        this.refusalRepo = refusalRepo;
        this.jointRepo = jointRepo;
        this.interventionRepo = interventionRepo;
        this.timeline = timeline;
    }

    private Student requireStudent(Long id) {
        return studentRepo.findById(id).orElseThrow(() -> new BusinessException("学生不存在"));
    }

    /* ================= 1. 家长拒绝复诊登记（append-only 新节点） ================= */

    @Transactional
    public RefusalFollowUp recordRefusal(RecordDtos.RefusalRequest req) {
        Student s = requireStudent(req.getStudentId());

        RefusalReasonType reasonType = parseReason(req.getReasonType());
        if ((req.getReasonDetail() == null || req.getReasonDetail().isBlank())
                && reasonType == null) {
            throw new BusinessException("必须记录家长拒绝原因（归因或原话至少填一项）");
        }
        if (req.getRiskNotice() == null || req.getRiskNotice().isBlank()) {
            throw new BusinessException("必须记录向家长告知的风险内容");
        }
        if (!Boolean.TRUE.equals(req.getRiskAcknowledged())) {
            throw new BusinessException("需确认家长已知悉风险仍坚持拒绝复诊");
        }
        if (req.getCommunicator() == null || req.getCommunicator().isBlank()) {
            throw new BusinessException("必须记录与家长沟通的工作人员");
        }

        RefusalTrigger trigger = parseTrigger(req.getTriggerSource(), RefusalTrigger.INITIAL_NOTIFY);
        ScreenRecord linked = req.getLinkedScreenId() == null ? null
                : screenRepo.findById(req.getLinkedScreenId()).orElse(null);

        RefusalFollowUp node = baseNode(s, trigger, linked);
        node.setOutcome(RefusalOutcome.REFUSED);
        node.setReasonType(reasonType);
        node.setReasonDetail(req.getReasonDetail());
        node.setRiskNotice(req.getRiskNotice());
        node.setRiskAcknowledged(true);
        node.setConfirmTime(LocalDateTime.now());
        node.setCommunicator(req.getCommunicator());
        node.setContactChannel(blank(req.getContactChannel(), "电话"));
        node.setPlannedCallbackDate(req.getPlannedCallbackDate() != null
                ? req.getPlannedCallbackDate() : LocalDate.now().plusWeeks(2));
        fillPrevVision(node);
        node.setRemark(req.getRemark());
        RefusalFollowUp saved = refusalRepo.save(node);

        // 同步风险告知与沟通人员到家长确认记录
        confirmRepo.findFirstByStudentIdOrderByIdDesc(s.getId()).ifPresent(c -> {
            c.setRiskNotice(req.getRiskNotice());
            c.setCommunicator(req.getCommunicator());
            confirmRepo.save(c);
        });

        int refusalCount = countRefusals(s.getId());
        timeline.raiseIssue(s, ScreenFlag.REFUSAL_FOLLOWUP,
                "家长第" + refusalCount + "次拒绝复诊（归因："
                        + (reasonType != null ? reasonType.getLabel() : "其他") + "），已告知风险并约定回访，需持续跟进");
        timeline.event(s, EventType.REFUSAL,
                "家长拒绝复诊（第" + refusalCount + "次，来源：" + trigger.getLabel() + "），原因："
                        + nz(req.getReasonDetail())
                        + "；已进行风险告知并取得家长知情确认；沟通人员：" + req.getCommunicator()
                        + "；约定回访：" + node.getPlannedCallbackDate());
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
        return saved;
    }

    /* ================= 2. 健康老师约定时间回访 ================= */

    @Transactional
    public RefusalFollowUp healthCallback(RecordDtos.CallbackRequest req) {
        Student s = requireStudent(req.getStudentId());
        ParentAttitude attitude;
        try {
            attitude = ParentAttitude.valueOf(req.getParentAttitude());
        } catch (Exception e) {
            throw new BusinessException("家长当前态度不合法");
        }

        RefusalFollowUp node = baseNode(s, RefusalTrigger.CALLBACK_AFTER, null);
        node.setCallbackTime(req.getCallbackTime() != null ? req.getCallbackTime() : LocalDateTime.now());
        node.setCallbackStaff(blank(req.getCallbackStaff(), SecurityUtils.actorLabel()));
        node.setSeatSituation(req.getSeatSituation());
        node.setHomeworkEyeUse(req.getHomeworkEyeUse());
        node.setOutdoorActivity(req.getOutdoorActivity());
        node.setParentAttitude(attitude);
        fillPrevVision(node);
        node.setPlannedCallbackDate(req.getNextCallbackDate());
        node.setContactChannel("回访");
        node.setRemark(req.getRemark());

        String compare = "；与上次筛查同档比较：上次裸眼视力 左" + nz(node.getPrevVisionLeft())
                + "/右" + nz(node.getPrevVisionRight());

        switch (attitude) {
            case AGREED -> {
                node.setOutcome(RefusalOutcome.AGREED_VISIT);
                refusalRepo.save(node);
                timeline.event(s, EventType.HEALTH_CALLBACK,
                        "健康老师回访：座位【" + nz(req.getSeatSituation()) + "】、作业用眼【"
                                + nz(req.getHomeworkEyeUse()) + "】、户外活动【" + nz(req.getOutdoorActivity())
                                + "】，家长已同意复诊" + compare);
                closeRefusalIssue(s, "家长经回访已同意复诊");
                timeline.changeStage(s, StudentStage.AWAIT_VISIT);
            }
            case STILL_REFUSE -> {
                // 回访现场再次拒绝 → 本节点同时承载回访信息与新一次拒绝，绝不覆盖第一次记录
                if (req.getRiskNotice() == null || req.getRiskNotice().isBlank()
                        || !Boolean.TRUE.equals(req.getRiskAcknowledged())) {
                    throw new BusinessException("家长回访时仍拒绝复诊，必须再次记录风险告知与知情确认");
                }
                RefusalReasonType reasonType = parseReason(req.getReasonType());
                node.setOutcome(RefusalOutcome.REFUSED);
                node.setReasonType(reasonType);
                node.setReasonDetail(req.getReasonDetail());
                node.setRiskNotice(req.getRiskNotice());
                node.setRiskAcknowledged(true);
                node.setConfirmTime(node.getCallbackTime());
                node.setCommunicator(node.getCallbackStaff());
                refusalRepo.save(node);
                int refusalCount = countRefusals(s.getId());
                timeline.raiseIssue(s, ScreenFlag.REFUSAL_FOLLOWUP,
                        "回访后家长第" + refusalCount + "次仍拒绝复诊，风险持续，需加强跟进");
                timeline.event(s, EventType.HEALTH_CALLBACK,
                        "健康老师回访：座位【" + nz(req.getSeatSituation()) + "】、作业用眼【"
                                + nz(req.getHomeworkEyeUse()) + "】、户外活动【" + nz(req.getOutdoorActivity())
                                + "】；家长仍拒绝复诊（第" + refusalCount + "次），已再次风险告知并知情确认" + compare);
                timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
            }
            default -> {
                node.setOutcome(RefusalOutcome.CALLBACK_DONE);
                refusalRepo.save(node);
                timeline.event(s, EventType.HEALTH_CALLBACK,
                        "健康老师回访：座位【" + nz(req.getSeatSituation()) + "】、作业用眼【"
                                + nz(req.getHomeworkEyeUse()) + "】、户外活动【" + nz(req.getOutdoorActivity())
                                + "】，家长态度：" + attitude.getLabel() + compare
                                + (req.getNextCallbackDate() == null ? "" : "；下次回访 " + req.getNextCallbackDate()));
                timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
            }
        }
        return node;
    }

    /* ================= 3. 后续筛查继续下降：重新生成复诊提醒（新节点） ================= */

    /**
     * 由筛查提交流程调用：当该生存在历史拒诊节点、且本次异常筛查视力较上次继续下降时，
     * 带出原拒诊记录与风险提示，重新生成复诊提醒。返回是否生成了提醒。
     */
    @Transactional
    public boolean remindIfDroppedAfterRefusal(Student s, ScreenRecord latest) {
        if (latest == null || !Boolean.TRUE.equals(latest.getAbnormal())) return false;
        List<RefusalFollowUp> history = refusalRepo.findByStudentIdOrderBySeqNoAscIdAsc(s.getId());
        if (history.isEmpty()) return false;

        ScreenRecord previous = screenRepo.findByStudentIdOrderByScreenDateDescIdDesc(s.getId()).stream()
                .filter(r -> !r.getId().equals(latest.getId()) && !Boolean.TRUE.equals(r.getAbsent()))
                .findFirst().orElse(null);
        if (previous != null && !isWorse(latest, previous)) {
            return false;
        }

        RefusalFollowUp last = history.get(history.size() - 1);
        RefusalFollowUp node = baseNode(s, RefusalTrigger.RESCREEN_DROP, latest);
        node.setOutcome(RefusalOutcome.RE_REMINDED);
        node.setRiskNotice(last.getRiskNotice() != null ? last.getRiskNotice() : STANDARD_RISK_NOTICE);
        node.setCommunicator(SecurityUtils.actorLabel());
        node.setContactChannel("系统/健康老师");
        node.setRevisitReminded(true);
        node.setPrevVisionLeft(previous == null ? null : previous.getNakedVisionLeft());
        node.setPrevVisionRight(previous == null ? null : previous.getNakedVisionRight());
        String reminder = String.format(
                "后续筛查视力继续下降：本次裸眼视力 左%s/右%s（上次 左%s/右%s）。"
                + "该生已有 %d 次拒绝复诊记录（最近原因：%s），原风险告知仍然有效，请尽快带孩子到医院复诊确认。",
                nz(latest.getNakedVisionLeft()), nz(latest.getNakedVisionRight()),
                previous == null ? "—" : nz(previous.getNakedVisionLeft()),
                previous == null ? "—" : nz(previous.getNakedVisionRight()),
                countRefusals(s.getId()),
                last.getReasonType() != null ? last.getReasonType().getLabel() : nz(last.getReasonDetail()));
        node.setReminderContent(reminder);
        node.setPlannedCallbackDate(LocalDate.now().plusWeeks(2));
        refusalRepo.save(node);

        timeline.raiseIssue(s, ScreenFlag.REFUSAL_FOLLOWUP,
                "后续筛查视力继续下降，已带出第" + history.size() + "次拒诊记录并重新生成复诊提醒");
        timeline.event(s, EventType.REVISIT_REMIND, reminder);
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
        return true;
    }

    /* ================= 4. 连续拒绝：申请班主任/社区医生共同沟通 ================= */

    @Transactional
    public JointCommunication applyJointComm(RecordDtos.JointCommRequest req) {
        Student s = requireStudent(req.getStudentId());
        long refusalCount = refusalRepo.findByStudentIdOrderBySeqNoAscIdAsc(s.getId()).stream()
                .filter(n -> n.getOutcome() == RefusalOutcome.REFUSED).count();
        if (refusalCount < 1) {
            throw new BusinessException("该生尚无家长拒绝复诊记录，不能发起共同沟通");
        }

        JointCommType type;
        try {
            type = JointCommType.valueOf(req.getCommType());
        } catch (Exception e) {
            throw new BusinessException("共同沟通对象只能为 HEAD_TEACHER/COMMUNITY_DOCTOR/BOTH");
        }
        RefusalFollowUp linked = req.getRefusalId() != null
                ? refusalRepo.findById(req.getRefusalId()).orElse(null)
                : refusalRepo.findFirstByStudentIdOrderBySeqNoDescIdDesc(s.getId()).orElse(null);

        JointCommunication jc = new JointCommunication();
        jc.setStudent(s);
        jc.setRefusalFollowUp(linked);
        jc.setCommType(type);
        jc.setStatus(JointCommStatus.APPLIED);
        jc.setApplicant(SecurityUtils.actorLabel());
        jc.setApplyTime(LocalDateTime.now());
        jc.setApplyReason(req.getApplyReason());
        jc.setHeadTeacherName(req.getHeadTeacherName());
        jc.setCommunityDoctorName(req.getCommunityDoctorName());
        jc.setPlannedTime(req.getPlannedTime());
        jc.setChannel(req.getChannel());
        JointCommunication saved = jointRepo.save(jc);

        RefusalFollowUp node = baseNode(s, RefusalTrigger.MANUAL,
                linked == null ? null : linked.getLinkedScreen());
        node.setOutcome(RefusalOutcome.JOINT_COMM);
        node.setCommunicator(jc.getApplicant());
        node.setContactChannel(type.getLabel());
        node.setRemark("已申请共同沟通（申请ID=" + saved.getId() + "）");
        refusalRepo.save(node);

        timeline.raiseIssue(s, ScreenFlag.JOINT_COMM,
                "家长连续" + refusalCount + "次拒绝复诊且风险持续升高，学校申请" + type.getLabel()
                        + "（申请ID=" + saved.getId() + "）");
        timeline.event(s, EventType.JOINT_COMM,
                "学校申请" + type.getLabel() + "：班主任 " + nz(req.getHeadTeacherName())
                        + "、社区医生 " + nz(req.getCommunityDoctorName())
                        + "；理由：" + nz(req.getApplyReason()));
        return saved;
    }

    /** 受理/退回共同沟通申请 */
    @Transactional
    public JointCommunication reviewJointComm(Long jointCommId, String action, String opinion) {
        JointCommunication jc = jointRepo.findById(jointCommId)
                .orElseThrow(() -> new BusinessException("共同沟通申请不存在"));
        switch (action) {
            case "ACCEPTED" -> jc.setStatus(JointCommStatus.ACCEPTED);
            case "REJECTED" -> {
                jc.setStatus(JointCommStatus.REJECTED);
                jc.setCommResult(opinion);
            }
            default -> throw new BusinessException("动作只能为 ACCEPTED/REJECTED");
        }
        return jointRepo.save(jc);
    }

    /** 登记共同沟通结果与学校干预措施（可追溯到学生） */
    @Transactional
    public JointCommunication recordJointCommResult(RecordDtos.JointCommResultRequest req) {
        JointCommunication jc = jointRepo.findById(req.getJointCommId())
                .orElseThrow(() -> new BusinessException("共同沟通申请不存在"));
        Student s = jc.getStudent();
        jc.setStatus(JointCommStatus.DONE);
        jc.setActualTime(req.getActualTime() != null ? req.getActualTime() : LocalDateTime.now());
        if (req.getChannel() != null) jc.setChannel(req.getChannel());
        jc.setCommResult(req.getCommResult());
        jc.setSchoolMeasure(req.getSchoolMeasure());
        jc.setRecorder(blank(req.getRecorder(), SecurityUtils.actorLabel()));
        jointRepo.save(jc);

        // 学校据此采取的干预措施落到 intervention 表，可追溯到学生
        if (req.getSchoolMeasure() != null && !req.getSchoolMeasure().isBlank()) {
            Intervention iv = new Intervention();
            iv.setStudent(s);
            iv.setType(InterventionType.BEHAVIOR_GUIDE);
            iv.setExecDate(LocalDate.now());
            iv.setExecutor(jc.getRecorder());
            iv.setContent("多方共同沟通后学校干预：" + req.getSchoolMeasure());
            iv.setExecuted(true);
            interventionRepo.save(iv);
        }

        timeline.event(s, EventType.JOINT_COMM,
                jc.getCommType().getLabel() + "完成：沟通结果【" + nz(req.getCommResult()) + "】"
                        + (req.getSchoolMeasure() == null ? "" : "；学校干预措施【" + req.getSchoolMeasure() + "】"));
        timeline.resolveIssueByFlag(s, ScreenFlag.JOINT_COMM,
                "共同沟通已完成（申请ID=" + jc.getId() + "）");

        RefusalFollowUp doneNode = baseNode(s, RefusalTrigger.MANUAL, null);
        doneNode.setOutcome(RefusalOutcome.JOINT_DONE);
        doneNode.setCommunicator(jc.getRecorder());
        doneNode.setContactChannel(jc.getCommType().getLabel());
        doneNode.setRemark("共同沟通结果：" + req.getCommResult());

        if (Boolean.TRUE.equals(req.getParentAgreed())) {
            doneNode.setOutcome(RefusalOutcome.AGREED_VISIT);
            doneNode.setRemark("共同沟通后家长同意复诊；沟通结果：" + req.getCommResult());
            refusalRepo.save(doneNode);
            timeline.resolveIssueByFlag(s, ScreenFlag.REFUSAL_FOLLOWUP,
                    "经" + jc.getCommType().getLabel() + "，家长同意复诊");
            timeline.event(s, EventType.REFUSAL, "经多方共同沟通，家长同意带孩子复诊");
            timeline.changeStage(s, StudentStage.AWAIT_VISIT);
        } else {
            refusalRepo.save(doneNode);
            timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
        }
        return jc;
    }

    /* ---------------- 家长选择“暂缓”时联动建立首个拒诊节点 ---------------- */

    @Transactional
    public void recordInitialDefer(Student s, String deferReason, String riskNotice,
                                   String communicator, String reasonTypeName,
                                   Boolean riskAcknowledged, LocalDate plannedCallbackDate) {
        RefusalFollowUp node = baseNode(s, RefusalTrigger.INITIAL_NOTIFY, null);
        node.setOutcome(RefusalOutcome.REFUSED);
        RefusalReasonType reasonType = parseReason(reasonTypeName);
        if (reasonType == null) reasonType = RefusalReasonType.FATIGUE;
        node.setReasonType(reasonType);
        node.setReasonDetail(deferReason);
        node.setRiskNotice(blank(riskNotice, STANDARD_RISK_NOTICE));
        node.setRiskAcknowledged(Boolean.TRUE.equals(riskAcknowledged));
        node.setConfirmTime(LocalDateTime.now());
        node.setCommunicator(blank(communicator, SecurityUtils.actorLabel()));
        node.setContactChannel("家长确认");
        node.setPlannedCallbackDate(plannedCallbackDate != null
                ? plannedCallbackDate : LocalDate.now().plusWeeks(2));
        fillPrevVision(node);
        refusalRepo.save(node);

        timeline.raiseIssue(s, ScreenFlag.REFUSAL_FOLLOWUP,
                "家长选择暂缓/拒绝复诊（归因：" + reasonType.getLabel() + "），已记录风险告知与沟通人员，需健康老师持续回访跟进");
        timeline.event(s, EventType.REFUSAL,
                "家长确认时选择暂缓/拒绝复诊，原因：" + nz(deferReason) + "；已记录风险告知内容与沟通人员 "
                        + node.getCommunicator() + "，进入拒诊持续跟进，约定回访 " + node.getPlannedCallbackDate());
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
    }

    /* ---------------- 公共辅助 ---------------- */

    private RefusalFollowUp baseNode(Student s, RefusalTrigger trigger, ScreenRecord linked) {
        RefusalFollowUp node = new RefusalFollowUp();
        node.setStudent(s);
        node.setSeqNo((int) refusalRepo.countByStudentId(s.getId()) + 1);
        node.setTriggerSource(trigger);
        node.setLinkedScreen(linked);
        return node;
    }

    private void fillPrevVision(RefusalFollowUp node) {
        screenRepo.findFirstByStudentIdOrderByScreenDateDescIdDesc(node.getStudent().getId())
                .ifPresent(sc -> {
                    node.setPrevVisionLeft(sc.getNakedVisionLeft());
                    node.setPrevVisionRight(sc.getNakedVisionRight());
                });
    }

    private int countRefusals(Long studentId) {
        return (int) refusalRepo.findByStudentIdOrderBySeqNoAscIdAsc(studentId).stream()
                .filter(n -> n.getOutcome() == RefusalOutcome.REFUSED).count();
    }

    private void closeRefusalIssue(Student s, String note) {
        timeline.resolveIssueByFlag(s, ScreenFlag.REFUSAL_FOLLOWUP, note);
    }

    /** 5 分记录数值越小视力越差；任一眼较上次更低即判定继续下降 */
    private boolean isWorse(ScreenRecord latest, ScreenRecord prev) {
        Double lNow = visionMin(latest), lPrev = visionMin(prev);
        if (lNow == null) return false;
        if (lPrev == null) return true;
        // 5 分记录（>2）数值变小为下降；小数记录（<=2）数值变小也为下降
        return lNow < lPrev;
    }

    private Double visionMin(ScreenRecord r) {
        Double l = parseVision(r.getNakedVisionLeft());
        Double rr = parseVision(r.getNakedVisionRight());
        if (l == null) return rr;
        if (rr == null) return l;
        return Math.min(l, rr);
    }

    private Double parseVision(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RefusalReasonType parseReason(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return RefusalReasonType.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    private RefusalTrigger parseTrigger(String name, RefusalTrigger def) {
        if (name == null || name.isBlank()) return def;
        try {
            return RefusalTrigger.valueOf(name);
        } catch (Exception e) {
            return def;
        }
    }

    private String blank(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }

    private String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
