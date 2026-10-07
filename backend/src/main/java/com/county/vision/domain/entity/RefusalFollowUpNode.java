package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.NodeType;
import com.county.vision.domain.enums.ParentAttitude;
import com.county.vision.domain.enums.RefusalReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家长拒绝复诊后的跟进节点（不可变追加式记录）。
 *
 * 关键原则：
 * 1. 家长每次拒绝都生成一条 REFUSAL 新节点，第二次拒绝绝不覆盖第一次记录；
 * 2. 健康老师在约定时间回访（RETURN_VISIT），登记座位/作业用眼/户外活动/家长当前态度，
 *    与前一次筛查结果放在同一档案中比较；
 * 3. 后续筛查继续下降时生成 RE_REMIND 节点，带出原拒诊记录与风险提示，重新生成复诊提醒；
 * 4. 连续拒绝且风险持续升高时生成 JOINT_COMM 节点，记录申请的班主任/社区医生共同沟通、
 *    沟通结果与学校干预措施，全部可追溯到学生本人。
 */
@Getter
@Setter
@Entity
@Table(name = "refusal_followup_node")
public class RefusalFollowUpNode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    /** 该学生拒诊跟进链路中的节点序号（从 1 开始递增） */
    private Integer seq;

    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    /** 触发本节点的当次筛查记录（拒绝/再提醒时记录，用于与前次筛查比较） */
    @ManyToOne(fetch = FetchType.LAZY)
    private ScreenRecord screenRecord;

    /* ---------------- 拒绝复诊信息（nodeType=REFUSAL） ---------------- */

    @Enumerated(EnumType.STRING)
    private RefusalReason refusalReason;

    /** 家长拒绝原因的原话/补充说明 */
    @Column(length = 500)
    private String refusalReasonDetail;

    /** 本次向家长告知的风险内容（视力继续下降、真性近视、弱视风险等），作为留痕 */
    @Column(length = 1000)
    private String riskNotice;

    /** 家长确认拒绝的时间（不是简单“已处理”，而是明确的确认时点） */
    private LocalDateTime confirmTime;

    /** 与家长沟通的工作人员（健康老师/班主任等，实际取登录用户，可手工修正） */
    private String communicator;

    /* ---------------- 约定回访信息（nodeType=RETURN_VISIT） ---------------- */

    /** 与家长约定的回访时间 */
    private LocalDate plannedReturnDate;

    /** 实际回访时间 */
    private LocalDate actualReturnDate;

    /** 该节点携带的约定回访是否已完成（false=待健康老师在约定时间回访，用于到期提醒） */
    private Boolean returnDone = false;

    /** 本次回访后家长仍拒绝/犹豫时，约定的下一次回访日期（为空表示没有后续约定） */
    private LocalDate nextPlannedReturnDate;

    /** 回访时孩子座位情况（排/列、是否靠前、是否定期轮换） */
    @Column(length = 500)
    private String seatSituation;

    /** 作业/近距离用眼情况（每日作业时长、电子产品使用、读写姿势等） */
    @Column(length = 500)
    private String homeworkEyeUse;

    /** 户外活动情况（每日户外时长、课间远眺、体育课等） */
    @Column(length = 500)
    private String outdoorActivity;

    @Enumerated(EnumType.STRING)
    private ParentAttitude parentAttitude;

    /** 回访时与前一次筛查结果的比较结论（如裸眼视力 4.8→4.5，球镜 -0.50→-1.25） */
    @Column(length = 1000)
    private String compareSummary;

    /* ---------------- 继续下降、重新提醒（nodeType=RE_REMIND） ---------------- */

    /** 带出的原拒诊节点（第一次/上一次拒绝），形成证据链，不覆盖原记录 */
    private Long linkedRefusalNodeId;

    /** 再提醒时给出的风险提示快照 */
    @Column(length = 1000)
    private String riskSummary;

    /* ---------------- 多方共同沟通（nodeType=JOINT_COMM） ---------------- */

    /** 发起共同沟通申请的人/角色（如学校、健康老师） */
    private String jointApplicant;

    /** 参与共同沟通方（班主任、社区医生等，多个用顿号分隔） */
    @Column(length = 500)
    private String jointParticipants;

    private LocalDateTime jointCommTime;

    /** 共同沟通结果（家长是否转变、是否同意复诊等） */
    @Column(length = 1000)
    private String jointResult;

    /** 学校因此采取的干预措施（座位调整、户外安排、用眼指导、持续提醒等，可追溯到学生） */
    @Column(length = 1000)
    private String schoolMeasures;

    /* ---------------- 通用字段 ---------------- */

    /** 节点状态：ACTIVE 跟进中 / CLOSED 已完成（如家长最终复诊） */
    private String status = "ACTIVE";

    @Column(length = 500)
    private String remark;
}
