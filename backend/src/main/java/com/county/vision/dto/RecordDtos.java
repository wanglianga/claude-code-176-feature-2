package com.county.vision.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

public class RecordDtos {

    @Data
    public static class ScreenRequest {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        private LocalDate screenDate;
        private String orgName;
        private String operatorName;
        private String deviceCalibNo;
        private String nakedVisionLeft;
        private String nakedVisionRight;
        private String sphereLeft;
        private String sphereRight;
        private String cylinderLeft;
        private String cylinderRight;
        private Boolean wearingGlasses;
        private String habitSummary;
        private Boolean retestNeeded;
        private Boolean absent;
    }

    @Data
    public static class SchoolReviewRequest {
        @NotNull
        private Long studentId;
        /** CONFIRM / REJECT / RETEST */
        private String result;
        private String opinion;
    }

    @Data
    public static class NotifyRequest {
        @NotNull
        private Long studentId;
        private String channel;
        /** true=本次电话未接通（累计次数） */
        private Boolean unreachable;
        private String remark;
    }

    @Data
    public static class ParentChoiceRequest {
        @NotNull
        private Long studentId;
        /** COUNTY_HOSPITAL / PARTNER_CLINIC / DEFER */
        private String choice;
        private String targetOrg;
        private String deferReason;
        /** 暂缓/拒绝复诊时的风险告知内容、沟通人员与原因归类（形成拒诊跟进节点） */
        private String riskNotice;
        private String communicator;
        private String reasonType;
        private Boolean riskAcknowledged;
        private java.time.LocalDate plannedCallbackDate;
        private String remark;
    }

    @Data
    public static class VisitRequest {
        @NotNull
        private Long studentId;
        private String orgName;
        private String doctor;
        private LocalDate visitDate;
        private Boolean trueMyopia;
        private Boolean needMydriasis;
        private String rxSphereLeft;
        private String rxSphereRight;
        private String rxCylinderLeft;
        private String rxCylinderRight;
        private String rxAxisLeft;
        private String rxAxisRight;
        private String pd;
        private String diagnosis;
        private Integer reviewCycleMonths;
        private Boolean subsidyEligible;
        private Boolean bigDifference;
        private String differenceNote;
    }

    @Data
    public static class GlassesRequest {
        @NotNull
        private Long studentId;
        private String opticianOrg;
        private LocalDate glassesDate;
        private Boolean partnerOptician;
        private java.math.BigDecimal amount;
        private String lensInfo;
        private Boolean discomfort;
        private String discomfortDetail;
    }

    @Data
    public static class SubsidyApplyRequest {
        @NotNull
        private Long studentId;
        private java.math.BigDecimal applyAmount;
        private String opinion;
    }

    @Data
    public static class SubsidyApproveRequest {
        @NotNull
        private Long applicationId;
        /** APPROVED / REJECTED / DISPUTE / PAID */
        private String action;
        private java.math.BigDecimal approvedAmount;
        private String opinion;
    }

    @Data
    public static class FollowUpRequest {
        @NotNull
        private Long studentId;
        private LocalDate plannedDate;
        private LocalDate actualDate;
        /** DONE / NO_SHOW */
        private String action;
        private String orgName;
        private String visionResult;
        private String remark;
    }

    @Data
    public static class InterventionRequest {
        @NotNull
        private Long studentId;
        /** 座位调整 SEAT_ADJUST 等 InterventionType */
        private String type;
        private LocalDate execDate;
        private String content;
        private Boolean executed;
    }

    @Data
    public static class IssueResolveRequest {
        @NotNull
        private Long issueId;
        private String resolveNote;
    }

    @Data
    public static class FixImportRequest {
        @NotNull
        private Long studentId;
        private String parentPhone;
        private String name;
        private String studentNo;
        private String grade;
        private String className;
        private String gender;
    }

    /* ================= 家长拒绝复诊跟进 ================= */

    /** 家长拒绝复诊登记（首次拒绝 / 后续筛查下降后再次拒绝 / 回访后仍拒绝） */
    @Data
    public static class RefusalRequest {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        /** 节点来源：INITIAL_NOTIFY / RESCREEN_DROP / CALLBACK_AFTER / MANUAL */
        private String triggerSource;
        /** 关联触发的筛查记录ID（后续筛查下降时） */
        private Long linkedScreenId;
        /** 拒绝原因归类 RefusalReasonType */
        private String reasonType;
        /** 家长原话/详细原因 */
        private String reasonDetail;
        /** 向家长告知的风险内容 */
        private String riskNotice;
        /** 家长已知悉风险仍拒绝（必填确认） */
        private Boolean riskAcknowledged;
        /** 沟通人员 */
        private String communicator;
        /** 沟通方式 */
        private String contactChannel;
        /** 与家长约定的回访日期 */
        private LocalDate plannedCallbackDate;
        private String remark;
    }

    /** 健康老师在约定时间发起回访 */
    @Data
    public static class CallbackRequest {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        /** 实际回访时间（默认当前） */
        private java.time.LocalDateTime callbackTime;
        /** 回访人（健康老师） */
        private String callbackStaff;
        private String seatSituation;
        private String homeworkEyeUse;
        private String outdoorActivity;
        /** 家长当前态度 ParentAttitude */
        private String parentAttitude;
        /** 家长仍拒绝时，本次再次告知的风险与拒绝原因（形成新的拒诊节点） */
        private String riskNotice;
        private String reasonType;
        private String reasonDetail;
        private Boolean riskAcknowledged;
        /** 与家长新约定的下次回访日期 */
        private LocalDate nextCallbackDate;
        private String remark;
    }

    /** 连续拒绝且风险持续升高，学校申请班主任/社区医生共同沟通 */
    @Data
    public static class JointCommRequest {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        /** 关联的拒诊节点ID（可空，默认最近一次） */
        private Long refusalId;
        /** JointCommType：HEAD_TEACHER / COMMUNITY_DOCTOR / BOTH */
        private String commType;
        private String applyReason;
        private String headTeacherName;
        private String communityDoctorName;
        private java.time.LocalDateTime plannedTime;
        private String channel;
    }

    /** 登记共同沟通结果与学校干预措施 */
    @Data
    public static class JointCommResultRequest {
        @NotNull
        private Long jointCommId;
        private java.time.LocalDateTime actualTime;
        private String channel;
        /** 共同沟通结果 */
        private String commResult;
        /** 学校采取的干预措施（会同时写一条可追溯的学校干预记录） */
        private String schoolMeasure;
        /** 家长是否经共同沟通后同意复诊 */
        private Boolean parentAgreed;
        private String recorder;
    }
}
