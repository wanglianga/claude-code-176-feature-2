package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.JointCommStatus;
import com.county.vision.domain.enums.JointCommType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 连续拒绝复诊且风险持续升高时，学校申请班主任/社区医生共同沟通的记录。
 * 沟通结果与学校据此采取的干预措施，都可追溯到具体学生（关联 studentId 与 intervention）。
 */
@Getter
@Setter
@Entity
@Table(name = "joint_communication")
public class JointCommunication extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    /** 关联的拒诊跟进节点（第几次拒诊后申请） */
    @ManyToOne(fetch = FetchType.LAZY)
    private RefusalFollowUp refusalFollowUp;

    /** 共同沟通对象类型：班主任 / 社区医生 / 两者 */
    @Enumerated(EnumType.STRING)
    private JointCommType commType;

    /** 申请状态 */
    @Enumerated(EnumType.STRING)
    private JointCommStatus status = JointCommStatus.APPLIED;

    /** 申请人（学校健康老师） */
    private String applicant;
    private LocalDateTime applyTime;

    /** 申请理由（连续拒绝次数、风险升高情况） */
    @Column(length = 1000)
    private String applyReason;

    /** 被邀请参与的班主任姓名 */
    private String headTeacherName;
    /** 被邀请参与的社区医生姓名/机构 */
    private String communityDoctorName;

    /** 计划沟通时间 */
    private LocalDateTime plannedTime;
    /** 实际沟通时间 */
    private LocalDateTime actualTime;

    /** 沟通方式：家访/电话/三方会谈等 */
    private String channel;

    /** 共同沟通结果（家长是否转变、后续约定） */
    @Column(length = 1000)
    private String commResult;

    /** 学校据此采取的干预措施（座位/户外/用眼指导等）摘要，明细见 intervention 表 */
    @Column(length = 1000)
    private String schoolMeasure;

    /** 记录人 */
    private String recorder;
}
