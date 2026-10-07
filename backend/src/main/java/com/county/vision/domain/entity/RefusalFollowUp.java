package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.ParentAttitude;
import com.county.vision.domain.enums.RefusalOutcome;
import com.county.vision.domain.enums.RefusalReasonType;
import com.county.vision.domain.enums.RefusalTrigger;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家长拒绝复诊跟进节点（append-only，永不覆盖）。
 *
 * <p>每一次“拒绝登记 / 健康回访 / 重新提醒后再次拒绝 / 共同沟通”都形成一条新节点，
 * 通过 {@link #seqNo} 递增串联。家长再次拒绝时新增节点，而不是修改第一条记录。</p>
 *
 * <p>节点记录：拒绝原因（归因+原话）、风险告知内容、家长知悉确认、确认时间、沟通人员；
 * 健康回访记录孩子座位、作业用眼、户外活动与家长当前态度，与历次筛查结果同档比较。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "refusal_followup")
public class RefusalFollowUp extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    /** 第几次跟进节点（从 1 开始，连续递增） */
    private Integer seqNo;

    /** 节点来源：首次告知拒绝 / 后续筛查下降重新提醒 / 回访后仍拒绝 / 手工登记 */
    @Enumerated(EnumType.STRING)
    private RefusalTrigger triggerSource;

    /** 触发本节点的那一次筛查记录（后续筛查下降重新提醒时关联），可为空 */
    @ManyToOne(fetch = FetchType.LAZY)
    private ScreenRecord linkedScreen;

    /** 本节点处理结果 */
    @Enumerated(EnumType.STRING)
    private RefusalOutcome outcome;

    /* ---------------- 拒绝原因 ---------------- */

    /** 拒绝原因归类 */
    @Enumerated(EnumType.STRING)
    private RefusalReasonType reasonType;

    /** 家长原话 / 拒绝原因详细描述 */
    @Column(length = 1000)
    private String reasonDetail;

    /* ---------------- 风险告知与确认 ---------------- */

    /** 本次向家长告知的风险内容（近视进展、并发症、复诊必要性等） */
    @Column(length = 1000)
    private String riskNotice;

    /** 家长是否已知悉风险仍坚持拒绝（风险知情确认） */
    private Boolean riskAcknowledged = false;

    /** 家长确认时间（拒绝/知悉的确认时刻） */
    private LocalDateTime confirmTime;

    /** 沟通人员（与家长沟通的健康老师/工作人员姓名） */
    private String communicator;

    /** 沟通方式：电话/面访/家长会/APP 等 */
    private String contactChannel;

    /* ---------------- 健康回访（约定时间发起） ---------------- */

    /** 与家长约定的回访时间 */
    private LocalDate plannedCallbackDate;

    /** 实际回访时间 */
    private LocalDateTime callbackTime;

    /** 回访人（健康老师） */
    private String callbackStaff;

    /** 回访：孩子当前座位情况（如第几排、是否居中、是否定期轮换） */
    @Column(length = 500)
    private String seatSituation;

    /** 回访：作业/近距离用眼情况（时长、姿势、电子产品） */
    @Column(length = 500)
    private String homeworkEyeUse;

    /** 回访：户外活动情况（每日时长、课间远眺等） */
    @Column(length = 500)
    private String outdoorActivity;

    /** 回访：家长当前态度 */
    @Enumerated(EnumType.STRING)
    private ParentAttitude parentAttitude;

    /** 回访时比对的上一次筛查裸眼视力（左/右），便于同档比较 */
    private String prevVisionLeft;
    private String prevVisionRight;

    /* ---------------- 重新复诊提醒 ---------------- */

    /** 本节点是否重新生成了复诊提醒（后续筛查持续下降时） */
    private Boolean revisitReminded = false;

    /** 重新生成的复诊提醒内容（含原拒诊记录与风险提示） */
    @Column(length = 1000)
    private String reminderContent;

    @Column(length = 1000)
    private String remark;
}
