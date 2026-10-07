package com.county.vision.service;

import com.county.vision.common.BusinessException;
import com.county.vision.domain.entity.*;
import com.county.vision.domain.enums.*;
import com.county.vision.domain.repository.*;
import com.county.vision.dto.RecordDtos;
import com.county.vision.security.LoginUser;
import com.county.vision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 家长拒绝复诊后的跟进服务。
 *
 * 与“暂缓处理（ParentChoice.DEFER）”不同，这里处理的是家长明确拒绝到医院/门诊复诊的情形，
 * 典型为“家长认为孩子只是疲劳用眼”。平台必须：
 * 1. 记录拒绝原因、风险告知内容、确认时间、沟通人员，绝不能简单标成“已处理”；
 * 2. 健康老师在约定时间回访，登记座位/作业用眼/户外活动/家长当前态度，并与前一次筛查同档比较；
 * 3. 后续筛查继续下降时，自动带出原拒诊记录与风险提示，重新生成复诊提醒（RE_REMIND）；
 * 4. 家长再次拒绝生成新的 REFUSAL 节点，绝不覆盖第一次记录；
 * 5. 连续拒绝且风险持续升高时，支持申请班主任/社区医生共同沟通，沟通结果与学校干预措施全程留痕。
 */
@Service
public class RefusalFollowService {

    /** 节点状态 */
    public static final String ACTIVE = "ACTIVE";
    public static final String CLOSED = "CLOSED";

    private final RefusalFollowUpNodeRepository nodeRepo;
    private final StudentRepository studentRepo;
    private final ScreenRecordRepository screenRepo;
    private final InterventionRepository interventionRepo;
    private final TimelineService timeline;

    public RefusalFollowService(RefusalFollowUpNodeRepository nodeRepo, StudentRepository studentRepo,
                                ScreenRecordRepository screenRepo, InterventionRepository interventionRepo,
                                TimelineService timeline) {
        this.nodeRepo = nodeRepo;
        this.studentRepo = studentRepo;
        this.screenRepo = screenRepo;
        this.interventionRepo = interventionRepo;
        this.timeline = timeline;
    }

    private Student requireStudent(Long id) {
        return studentRepo.findById(id).orElseThrow(() -> new BusinessException("学生不存在"));
    }

    /** 种子数据/系统任务等无 HTTP 登录场景下返回 null；正常请求返回当前登录用户 */
    private LoginUser currentOrNull() {
        try {
            return SecurityUtils.current();
        } catch (Exception e) {
            return null;
        }
    }

    /** 学校角色只能操作本校学生；无登录上下文（系统/种子）不限制 */
    private void assertSameSchool(Student s) {
        LoginUser me = currentOrNull();
        if (me != null && me.getSchoolId() != null && !me.getSchoolId().equals(s.getSchool().getId())) {
            throw new BusinessException(403, "无权操作外校学生");
        }
    }

    /* ================= 1. 记录家长拒绝复诊（追加新节点，不覆盖历史） ================= */

    @Transactional
    public RefusalFollowUpNode recordRefusal(RecordDtos.RefusalRequest req) {
        Student s = requireStudent(req.getStudentId());
        assertSameSchool(s);

        RefusalReason reason;
        try {
            reason = RefusalReason.valueOf(req.getRefusalReason());
        } catch (Exception e) {
            throw new BusinessException("拒绝原因不合法（如 EYE_FATIGUE=认为只是疲劳用眼）");
        }

        ScreenRecord latestScreen = screenRepo.findFirstByStudentIdOrderByScreenDateDescIdDesc(s.getId())
                .orElse(null);

        int seq = (int) nodeRepo.countByStudentId(s.getId()) + 1;
        int refusalCount = nodeRepo.findByStudentIdAndNodeTypeOrderBySeqAscIdAsc(s.getId(), NodeType.REFUSAL).size() + 1;

        RefusalFollowUpNode node = new RefusalFollowUpNode();
        node.setStudent(s);
        node.setSeq(seq);
        node.setNodeType(NodeType.REFUSAL);
        node.setScreenRecord(latestScreen);
        node.setRefusalReason(reason);
        node.setRefusalReasonDetail(req.getRefusalReasonDetail());
        node.setRiskNotice(req.getRiskNotice() == null || req.getRiskNotice().isBlank()
                ? defaultRiskNotice(latestScreen) : req.getRiskNotice());
        node.setConfirmTime(req.getConfirmTime() != null ? req.getConfirmTime() : LocalDateTime.now());
        node.setCommunicator(req.getCommunicator() == null || req.getCommunicator().isBlank()
                ? SecurityUtils.actorLabel() : req.getCommunicator());
        node.setPlannedReturnDate(req.getPlannedReturnDate());
        node.setReturnDone(false);
        node.setStatus(ACTIVE);
        node.setRemark(req.getRemark());
        nodeRepo.save(node);

        // 异常台账：拒诊事项在家长真正复诊前始终保持 OPEN，不允许随本次登记“被处理掉”
        String desc = String.format("家长第%d次拒绝复诊：%s%s；风险已当面/电话告知并留痕；确认时间 %s；沟通人员 %s",
                refusalCount, reason.getLabel(),
                req.getRefusalReasonDetail() == null ? "" : "（" + req.getRefusalReasonDetail() + "）",
                fmtTime(node.getConfirmTime()), node.getCommunicator());
        upsertOpenIssue(s, ScreenFlag.PARENT_REFUSAL, desc,
                "（第" + refusalCount + "次拒绝）" + desc);

        timeline.event(s, EventType.REFUSAL,
                "家长拒绝复诊【第" + refusalCount + "次·节点#" + seq + "】原因：" + reason.getLabel()
                        + (req.getRefusalReasonDetail() == null ? "" : "；原话：" + req.getRefusalReasonDetail())
                        + "。已告知风险：" + node.getRiskNotice()
                        + (req.getPlannedReturnDate() == null ? "" : "；约定 " + req.getPlannedReturnDate() + " 回访")
                        + "。沟通人员：" + node.getCommunicator());
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);

        // 连续拒绝（本次≥第2次）且最近两次筛查显示继续下降 → 风险持续升高，可申请班主任/社区医生共同沟通
        if (refusalCount >= 2 && isLatestDeclined(s)) {
            raiseHighRisk(s, refusalCount);
        }
        return node;
    }

    /* ================= 2. 约定时间回访（座位/作业用眼/户外/家长态度 + 与前次筛查比较） ================= */

    @Transactional
    public RefusalFollowUpNode returnVisit(RecordDtos.RefusalReturnVisitRequest req) {
        Student s = requireStudent(req.getStudentId());
        assertSameSchool(s);

        ParentAttitude attitude;
        try {
            attitude = ParentAttitude.valueOf(req.getParentAttitude());
        } catch (Exception e) {
            throw new BusinessException("家长当前态度不合法（STILL_REFUSE/HESITATE/WILLING/ALREADY_VISITED/UNREACHABLE）");
        }

        // 找到承载本次“待履行回访约定”的节点（最近一个未完成且带约定日期的节点），回填表示约定已履行
        RefusalFollowUpNode anchor = null;
        if (req.getNodeId() != null) {
            RefusalFollowUpNode n = nodeRepo.findById(req.getNodeId())
                    .orElseThrow(() -> new BusinessException("回访节点不存在"));
            if (!n.getStudent().getId().equals(s.getId())) throw new BusinessException("节点与学生不匹配");
            anchor = n;
        } else {
            anchor = nodeRepo.findByStudentIdOrderBySeqAscIdAsc(s.getId()).stream()
                    .filter(n -> ACTIVE.equals(n.getStatus()) && !Boolean.TRUE.equals(n.getReturnDone())
                            && effectivePlanDate(n) != null)
                    .reduce((a, b) -> b).orElse(null);
        }
        LocalDate actualDate = req.getActualReturnDate() == null ? LocalDate.now() : req.getActualReturnDate();
        LocalDate plannedDate = anchor == null ? null : effectivePlanDate(anchor);
        if (anchor != null) {
            anchor.setReturnDone(true);
            anchor.setActualReturnDate(actualDate);
            nodeRepo.save(anchor);
        }

        // 与前一次筛查结果放在同一档案里比较：取最近两次筛查自动生成对比结论
        List<ScreenRecord> last2 = screenRepo.findFirst2ByStudentIdOrderByScreenDateDescIdDesc(s.getId());
        String compareSummary = last2.size() == 2
                ? buildCompare(last2.get(1), last2.get(0))
                : (last2.isEmpty() ? "暂无筛查结果可比较"
                    : "仅有一次筛查（" + last2.get(0).getScreenDate() + "），暂无前次结果可比较");

        int seq = (int) nodeRepo.countByStudentId(s.getId()) + 1;
        RefusalFollowUpNode node = new RefusalFollowUpNode();
        node.setStudent(s);
        node.setSeq(seq);
        node.setNodeType(NodeType.RETURN_VISIT);
        node.setScreenRecord(last2.isEmpty() ? null : last2.get(0));
        node.setPlannedReturnDate(plannedDate);
        node.setActualReturnDate(actualDate);
        node.setSeatSituation(req.getSeatSituation());
        node.setHomeworkEyeUse(req.getHomeworkEyeUse());
        node.setOutdoorActivity(req.getOutdoorActivity());
        node.setParentAttitude(attitude);
        node.setCompareSummary(compareSummary);
        node.setCommunicator(req.getCommunicator() == null || req.getCommunicator().isBlank()
                ? SecurityUtils.actorLabel() : req.getCommunicator());
        node.setConfirmTime(LocalDateTime.now());
        node.setStatus(ACTIVE);
        node.setRemark(req.getRemark());

        // 家长仍拒绝/犹豫且又约定了下一次回访：新节点承载下一次待履行约定
        boolean stillPending = attitude == ParentAttitude.STILL_REFUSE
                || attitude == ParentAttitude.HESITATE || attitude == ParentAttitude.UNREACHABLE;
        if (stillPending && req.getNextPlannedReturnDate() != null) {
            node.setNextPlannedReturnDate(req.getNextPlannedReturnDate());
            node.setReturnDone(false);
        } else {
            node.setReturnDone(true);
        }
        nodeRepo.save(node);

        timeline.event(s, EventType.RETURN_VISIT,
                "健康老师按约定回访（节点#" + seq + "）：座位「" + nz(req.getSeatSituation())
                        + "」；作业/近距离用眼「" + nz(req.getHomeworkEyeUse())
                        + "」；户外活动「" + nz(req.getOutdoorActivity())
                        + "」；家长当前态度：" + attitude.getLabel()
                        + "。与前次筛查比较：" + compareSummary
                        + (node.getNextPlannedReturnDate() == null ? ""
                            : "；再次约定 " + node.getNextPlannedReturnDate() + " 回访"));
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
        return node;
    }

    /* ================= 3. 后续筛查继续下降：带出原拒诊记录，重新生成复诊提醒 ================= */

    /**
     * 在筛查登记（异常）后调用。若该生存在未闭环的拒诊记录，且新筛查较上一次继续下降，
     * 则生成 RE_REMIND 节点并带出原拒诊记录与风险提示。返回是否触发了再提醒。
     */
    @Transactional
    public boolean detectReRemind(Student s, ScreenRecord newScreen) {
        List<RefusalFollowUpNode> activeRefusals = nodeRepo
                .findByStudentIdAndNodeTypeOrderBySeqAscIdAsc(s.getId(), NodeType.REFUSAL).stream()
                .filter(n -> ACTIVE.equals(n.getStatus())).toList();
        if (activeRefusals.isEmpty()) return false;

        List<ScreenRecord> last2 = screenRepo.findFirst2ByStudentIdOrderByScreenDateDescIdDesc(s.getId());
        if (last2.size() < 2) return false;
        ScreenRecord prev = last2.get(1);
        ScreenRecord latest = last2.get(0);
        if (!latest.getId().equals(newScreen.getId())) return false;
        CompareResult cmp = compare(prev, latest);
        if (!cmp.declined()) return false;

        // 带出第一次拒诊记录（证据链起点），不覆盖它
        RefusalFollowUpNode firstRefusal = activeRefusals.get(0);
        int refusalCount = activeRefusals.size();

        int seq = (int) nodeRepo.countByStudentId(s.getId()) + 1;
        RefusalFollowUpNode node = new RefusalFollowUpNode();
        node.setStudent(s);
        node.setSeq(seq);
        node.setNodeType(NodeType.RE_REMIND);
        node.setScreenRecord(latest);
        node.setLinkedRefusalNodeId(firstRefusal.getId());
        node.setRiskSummary("原拒诊记录（节点#" + firstRefusal.getSeq() + "，"
                + fmtTime(firstRefusal.getConfirmTime()) + "，沟通人 " + nz(firstRefusal.getCommunicator()) + "）："
                + firstRefusal.getRefusalReason().getLabel()
                + "；当时风险告知：" + nz(firstRefusal.getRiskNotice())
                + "。本次筛查继续下降：" + cmp.summary());
        node.setCommunicator(SecurityUtils.actorLabel());
        node.setConfirmTime(LocalDateTime.now());
        node.setStatus(ACTIVE);
        nodeRepo.save(node);

        String desc = "筛查持续下降，系统重新生成复诊提醒：" + cmp.summary()
                + "；带出原拒诊记录节点#" + firstRefusal.getSeq() + "（累计拒绝 " + refusalCount + " 次）";
        upsertOpenIssue(s, ScreenFlag.PARENT_REFUSAL, desc, null);

        timeline.event(s, EventType.RE_REMIND,
                "后续筛查继续下降（" + cmp.summary() + "），自动带出原拒诊记录（节点#"
                        + firstRefusal.getSeq() + "，" + firstRefusal.getRefusalReason().getLabel()
                        + "）与风险提示，重新生成复诊提醒，请再次联系家长安排复诊");
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
        return true;
    }

    /* ================= 4. 连续拒绝且风险升高：申请班主任/社区医生共同沟通 ================= */

    @Transactional
    public RefusalFollowUpNode jointComm(RecordDtos.JointCommRequest req) {
        Student s = requireStudent(req.getStudentId());
        assertSameSchool(s);
        if (req.getParticipants() == null || req.getParticipants().isBlank()) {
            throw new BusinessException("共同沟通参与方不能为空（班主任/社区医生）");
        }
        if (req.getJointResult() == null || req.getJointResult().isBlank()) {
            throw new BusinessException("必须登记共同沟通结果");
        }

        int seq = (int) nodeRepo.countByStudentId(s.getId()) + 1;
        RefusalFollowUpNode node = new RefusalFollowUpNode();
        node.setStudent(s);
        node.setSeq(seq);
        node.setNodeType(NodeType.JOINT_COMM);
        node.setScreenRecord(screenRepo.findFirstByStudentIdOrderByScreenDateDescIdDesc(s.getId()).orElse(null));
        node.setJointApplicant(req.getJointApplicant() == null || req.getJointApplicant().isBlank()
                ? SecurityUtils.actorLabel() : req.getJointApplicant());
        node.setJointParticipants(req.getParticipants());
        node.setJointCommTime(req.getJointCommTime() == null ? LocalDateTime.now() : req.getJointCommTime());
        node.setJointResult(req.getJointResult());
        node.setSchoolMeasures(req.getSchoolMeasures());
        node.setCommunicator(node.getJointParticipants());
        node.setConfirmTime(node.getJointCommTime());
        node.setStatus(ACTIVE);
        node.setRemark(req.getRemark());
        nodeRepo.save(node);

        // 风险升高标记（若尚未标记）
        raiseHighRisk(s,
                nodeRepo.findByStudentIdAndNodeTypeOrderBySeqAscIdAsc(s.getId(), NodeType.REFUSAL).size());

        timeline.event(s, EventType.JOINT_COMM,
                "学校申请多方共同沟通（节点#" + seq + "），参与方：" + req.getParticipants()
                        + "；申请人：" + node.getJointApplicant()
                        + "；沟通结果：" + req.getJointResult()
                        + (req.getSchoolMeasures() == null ? "" : "；学校干预措施：" + req.getSchoolMeasures()));

        // 学校干预措施同时写入“学校干预”记录，保证在干预台账与学生档案中都能追溯到这名学生
        if (req.getSchoolMeasures() != null && !req.getSchoolMeasures().isBlank()) {
            Intervention iv = new Intervention();
            iv.setStudent(s);
            iv.setType(InterventionType.OTHER);
            iv.setExecDate(node.getJointCommTime().toLocalDate());
            iv.setExecutor("多方共同沟通（" + req.getParticipants() + "）");
            iv.setContent("拒诊风险升高共同沟通后的学校干预：" + req.getSchoolMeasures());
            iv.setExecuted(true);
            interventionRepo.save(iv);
            timeline.event(s, EventType.INTERVENTION, "学校干预【其他干预】已执行：" + iv.getContent());
        }
        timeline.changeStage(s, StudentStage.REFUSAL_FOLLOW);
        return node;
    }

    /* ================= 5. 家长最终复诊：闭环整条拒诊跟进链路 ================= */

    @Transactional
    public void closeChain(Student s) {
        List<RefusalFollowUpNode> nodes = nodeRepo.findByStudentIdOrderBySeqAscIdAsc(s.getId());
        boolean any = false;
        for (RefusalFollowUpNode n : nodes) {
            if (ACTIVE.equals(n.getStatus())) {
                n.setStatus(CLOSED);
                n.setReturnDone(true);
                nodeRepo.save(n);
                any = true;
            }
        }
        if (!any) return;
        timeline.event(s, EventType.RETURN_VISIT, "家长已带孩子完成医院复诊，此前 "
                + nodes.stream().filter(n -> n.getNodeType() == NodeType.REFUSAL).count()
                + " 次拒诊记录与全部跟进节点归档保留，拒诊跟进闭环");
        closeIssueIfOpen(s, ScreenFlag.PARENT_REFUSAL, "家长已完成复诊，拒诊跟进闭环");
        closeIssueIfOpen(s, ScreenFlag.REFUSAL_RISK_HIGH, "多方沟通后家长已带孩子复诊，风险解除");
    }

    /* ================= 查询 ================= */

    @Transactional(readOnly = true)
    public List<Map<String, Object>> nodesOfStudent(Long studentId) {
        return nodeRepo.findByStudentIdOrderBySeqAscIdAsc(studentId).stream().map(this::nodeMap).toList();
    }

    /** 拒诊跟进中的学生（去重），可按学校过滤 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> activeStudents(Long schoolIdParam) {
        LoginUser me = SecurityUtils.current();
        final Long schoolId = me.getSchoolId() != null ? me.getSchoolId() : schoolIdParam;
        Map<Long, Student> uniq = new LinkedHashMap<>();
        nodeRepo.findByStatusOrderByIdAsc(ACTIVE).forEach(n -> {
            Student s = n.getStudent();
            if (schoolId != null && !schoolId.equals(s.getSchool().getId())) return;
            uniq.putIfAbsent(s.getId(), s);
        });
        List<Map<String, Object>> out = new ArrayList<>();
        for (Student s : uniq.values()) out.add(studentCard(s));
        return out;
    }

    /** 到约定时间/逾期应回访的节点（健康老师据此发起回访） */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> dueReturnVisits(LocalDate today) {
        LoginUser me = SecurityUtils.current();
        return nodeRepo.findDueReturnVisits(today == null ? LocalDate.now() : today).stream()
                .filter(n -> me.getSchoolId() == null || me.getSchoolId().equals(n.getStudent().getSchool().getId()))
                .map(n -> {
                    Map<String, Object> m = nodeMap(n);
                    m.put("studentName", n.getStudent().getName());
                    m.put("schoolName", n.getStudent().getSchool().getName());
                    m.put("parentName", n.getStudent().getParentName());
                    m.put("parentPhone", n.getStudent().getParentPhone());
                    return m;
                }).toList();
    }

    /** 连续拒绝且风险持续升高、建议/已可申请共同沟通的学生 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> jointCandidates() {
        LoginUser me = SecurityUtils.current();
        Map<Long, Student> uniq = new LinkedHashMap<>();
        nodeRepo.findByStatusOrderByIdAsc(ACTIVE).forEach(n -> {
            Student s = n.getStudent();
            if (me.getSchoolId() != null && !me.getSchoolId().equals(s.getSchool().getId())) return;
            long refusals = nodeRepo.findByStudentIdAndNodeTypeOrderBySeqAscIdAsc(s.getId(), NodeType.REFUSAL)
                    .stream().filter(x -> ACTIVE.equals(x.getStatus())).count();
            boolean highRisk = TimelineService.splitFlags(s.getFlags()).contains(ScreenFlag.REFUSAL_RISK_HIGH.name());
            if (refusals >= 2 && highRisk) uniq.putIfAbsent(s.getId(), s);
        });
        List<Map<String, Object>> out = new ArrayList<>();
        for (Student s : uniq.values()) {
            Map<String, Object> card = studentCard(s);
            long jointCount = nodeRepo.findByStudentIdOrderBySeqAscIdAsc(s.getId()).stream()
                    .filter(n -> n.getNodeType() == NodeType.JOINT_COMM).count();
            card.put("jointCommCount", jointCount);
            out.add(card);
        }
        return out;
    }

    /* ================= 投影 ================= */

    private Map<String, Object> studentCard(Student s) {
        List<RefusalFollowUpNode> all = nodeRepo.findByStudentIdOrderBySeqAscIdAsc(s.getId());
        long refusalCount = all.stream().filter(n -> n.getNodeType() == NodeType.REFUSAL).count();
        long jointCount = all.stream().filter(n -> n.getNodeType() == NodeType.JOINT_COMM).count();
        RefusalFollowUpNode last = all.isEmpty() ? null : all.get(all.size() - 1);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("studentId", s.getId());
        m.put("studentNo", s.getStudentNo());
        m.put("name", s.getName());
        m.put("schoolName", s.getSchool().getName());
        m.put("regionName", s.getSchool().getRegion() != null ? s.getSchool().getRegion().getName() : null);
        m.put("grade", s.getGradeClass().getGrade());
        m.put("className", s.getGradeClass().getClassName());
        m.put("parentName", s.getParentName());
        m.put("parentPhone", s.getParentPhone());
        m.put("stage", s.getStage().name());
        m.put("stageLabel", s.getStage().getLabel());
        m.put("refusalCount", refusalCount);
        m.put("jointCommCount", jointCount);
        m.put("highRisk", TimelineService.splitFlags(s.getFlags()).contains(ScreenFlag.REFUSAL_RISK_HIGH.name()));
        m.put("lastNodeType", last == null ? null : last.getNodeType().name());
        m.put("lastNodeTypeLabel", last == null ? null : last.getNodeType().getLabel());
        // 最近一次待履行的约定回访日期
        all.stream().filter(n -> ACTIVE.equals(n.getStatus()) && !Boolean.TRUE.equals(n.getReturnDone())
                        && effectivePlanDate(n) != null)
                .reduce((a, b) -> b)
                .ifPresent(n -> m.put("dueDate", effectivePlanDate(n)));
        return m;
    }

    private Map<String, Object> nodeMap(RefusalFollowUpNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("studentId", n.getStudent().getId());
        m.put("seq", n.getSeq());
        m.put("nodeType", n.getNodeType().name());
        m.put("nodeTypeLabel", n.getNodeType().getLabel());
        m.put("screenRecordId", n.getScreenRecord() == null ? null : n.getScreenRecord().getId());
        m.put("screenDate", n.getScreenRecord() == null ? null : n.getScreenRecord().getScreenDate());

        m.put("refusalReason", n.getRefusalReason() == null ? null : n.getRefusalReason().name());
        m.put("refusalReasonLabel", n.getRefusalReason() == null ? null : n.getRefusalReason().getLabel());
        m.put("refusalReasonDetail", n.getRefusalReasonDetail());
        m.put("riskNotice", n.getRiskNotice());
        m.put("confirmTime", n.getConfirmTime());
        m.put("communicator", n.getCommunicator());

        m.put("plannedReturnDate", n.getPlannedReturnDate());
        m.put("actualReturnDate", n.getActualReturnDate());
        m.put("returnDone", n.getReturnDone());
        m.put("nextPlannedReturnDate", n.getNextPlannedReturnDate());
        m.put("seatSituation", n.getSeatSituation());
        m.put("homeworkEyeUse", n.getHomeworkEyeUse());
        m.put("outdoorActivity", n.getOutdoorActivity());
        m.put("parentAttitude", n.getParentAttitude() == null ? null : n.getParentAttitude().name());
        m.put("parentAttitudeLabel", n.getParentAttitude() == null ? null : n.getParentAttitude().getLabel());
        m.put("compareSummary", n.getCompareSummary());

        m.put("linkedRefusalNodeId", n.getLinkedRefusalNodeId());
        m.put("riskSummary", n.getRiskSummary());

        m.put("jointApplicant", n.getJointApplicant());
        m.put("jointParticipants", n.getJointParticipants());
        m.put("jointCommTime", n.getJointCommTime());
        m.put("jointResult", n.getJointResult());
        m.put("schoolMeasures", n.getSchoolMeasures());

        m.put("status", n.getStatus());
        m.put("remark", n.getRemark());
        m.put("createdAt", n.getCreatedAt());
        return m;
    }

    /* ================= 内部工具 ================= */

    /** 节点携带的有效约定回访日期：优先“下一次约定”，否则本节点约定 */
    private LocalDate effectivePlanDate(RefusalFollowUpNode n) {
        return n.getNextPlannedReturnDate() != null ? n.getNextPlannedReturnDate() : n.getPlannedReturnDate();
    }

    private boolean isLatestDeclined(Student s) {
        List<ScreenRecord> last2 = screenRepo.findFirst2ByStudentIdOrderByScreenDateDescIdDesc(s.getId());
        return last2.size() == 2 && compare(last2.get(1), last2.get(0)).declined();
    }

    private void raiseHighRisk(Student s, int refusalCount) {
        boolean already = TimelineService.splitFlags(s.getFlags())
                .contains(ScreenFlag.REFUSAL_RISK_HIGH.name());
        String desc = "家长已连续 " + refusalCount + " 次拒绝复诊且筛查结果持续下降，近视进展风险升高，"
                + "建议学校申请班主任/社区医生共同沟通";
        // 台账/标记幂等；仅在风险首次升高时写一次时间线提示，避免后续节点重复刷屏
        timeline.raiseIssueAppend(s, ScreenFlag.REFUSAL_RISK_HIGH, desc);
        if (!already) {
            timeline.event(s, EventType.REFUSAL, "风险升高提示：" + desc);
        }
    }

    /**
     * 打开/更新未闭环异常事项。首次按描述建单；家长再次拒绝时把新情况追加到原单描述，
     * 既保留每次拒绝的累计信息，又绝不在家长真正复诊前把事项错误闭环为“已处理”。
     */
    private void upsertOpenIssue(Student s, ScreenFlag flag, String firstDesc, String appendIfExists) {
        timeline.raiseIssueAppend(s, flag, appendIfExists == null ? firstDesc : appendIfExists);
    }

    private void closeIssueIfOpen(Student s, ScreenFlag flag, String note) {
        timeline.resolveByFlag(s, flag, note);
    }

    private String defaultRiskNotice(ScreenRecord r) {
        if (r == null) {
            return "孩子筛查已提示视力异常。若不及时到正规医疗机构散瞳复诊，可能错过假性近视干预期、"
                    + "使近视进展为真性近视并快速加深，高度近视还可能带来眼底病变风险，建议尽快复诊明确诊断。";
        }
        return String.format(
                "孩子本次筛查已提示异常（裸眼视力 左%s/右%s，球镜 左%s/右%s）。若不及时到正规医疗机构散瞳复诊，"
                        + "可能错过假性近视干预期、使近视进展为真性近视并快速加深，高度近视还可能带来眼底病变风险，"
                        + "建议尽快到县医院或合作眼科门诊复诊明确诊断。",
                nz(r.getNakedVisionLeft()), nz(r.getNakedVisionRight()),
                nz(r.getSphereLeft()), nz(r.getSphereRight()));
    }

    /** 比较两次筛查，判断是否继续下降并生成说明 */
    private CompareResult compare(ScreenRecord prev, ScreenRecord latest) {
        StringBuilder sb = new StringBuilder();
        boolean declined = false;
        boolean[] vd = {false, false};
        boolean[] sd = {false, false};

        String[][] v = {
                {prev.getNakedVisionLeft(), latest.getNakedVisionLeft()},
                {prev.getNakedVisionRight(), latest.getNakedVisionRight()}
        };
        String[] eye = {"左", "右"};
        for (int i = 0; i < 2; i++) {
            Double a = parseVision(v[i][0]);
            Double b = parseVision(v[i][1]);
            if (a != null && b != null && sameScale(a, b)) {
                boolean down = b < a - 0.0001;
                vd[i] = down;
                declined |= down;
                sb.append("裸眼视力").append(eye[i]).append(" ").append(v[i][0]).append("→").append(v[i][1])
                        .append(down ? "↓；" : "；");
            } else if (v[i][0] != null || v[i][1] != null) {
                sb.append("裸眼视力").append(eye[i]).append(" ").append(nz(v[i][0])).append("→").append(nz(v[i][1])).append("；");
            }
        }
        String[][] sph = {
                {prev.getSphereLeft(), latest.getSphereLeft()},
                {prev.getSphereRight(), latest.getSphereRight()}
        };
        for (int i = 0; i < 2; i++) {
            Double a = parseSphere(sph[i][0]);
            Double b = parseSphere(sph[i][1]);
            if (a != null && b != null) {
                boolean worse = b <= a - 0.25;
                sd[i] = worse;
                declined |= worse;
                sb.append("球镜").append(eye[i]).append(" ").append(sph[i][0]).append("→").append(sph[i][1]).append("D")
                        .append(worse ? "加深↓；" : "；");
            } else if (sph[i][0] != null || sph[i][1] != null) {
                sb.append("球镜").append(eye[i]).append(" ").append(nz(sph[i][0])).append("→").append(nz(sph[i][1])).append("；");
            }
        }
        String summary = sb.length() == 0 ? "两次筛查关键指标缺失，无法自动比较" : sb.toString();
        return new CompareResult(declined, summary, vd, sd);
    }

    private String buildCompare(ScreenRecord prev, ScreenRecord latest) {
        CompareResult c = compare(prev, latest);
        return c.summary() + (c.declined() ? "结论：视力较前次继续下降，需再次提醒家长复诊。"
                : "结论：本次筛查指标较前次未见明显下降，仍建议按医嘱复诊明确。");
    }

    private boolean sameScale(double a, double b) {
        // 同为 5 分制（>2）或同为小数制（<=2）才可直接比较
        return (a > 2 && b > 2) || (a <= 2 && b <= 2);
    }

    private Double parseVision(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseSphere(String sph) {
        if (sph == null || sph.isBlank()) return null;
        try {
            return Double.parseDouble(sph.replace("D", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private String fmtTime(LocalDateTime t) {
        return t == null ? "—" : t.toString().replace('T', ' ').substring(0, 16);
    }

    private record CompareResult(boolean declined, String summary, boolean[] visionDown, boolean[] sphereDown) {
    }
}
