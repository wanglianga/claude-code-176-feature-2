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

    /** 家长拒绝复诊登记（拒绝原因/风险告知/确认时间/沟通人员，均须留痕，不能简单标成已处理） */
    @Data
    public static class RefusalRequest {
        @NotNull
        private Long studentId;
        /** RefusalReason 名称，如 EYE_FATIGUE（认为只是疲劳用眼） */
        private String refusalReason;
        /** 家长原话/补充说明 */
        private String refusalReasonDetail;
        /** 本次已向家长告知的风险内容（留痕）；为空时使用平台标准风险提示 */
        private String riskNotice;
        /** 沟通人员（健康老师/班主任），为空取当前登录人 */
        private String communicator;
        /** 家长确认拒绝的时间（为空取当前时间；支持沟通后补录） */
        private java.time.LocalDateTime confirmTime;
        /** 与家长约定的回访日期 */
        private java.time.LocalDate plannedReturnDate;
        private String remark;
    }

    /** 健康老师在约定时间发起的回访（座位/作业用眼/户外活动/家长当前态度，与前一次筛查同档比较） */
    @Data
    public static class RefusalReturnVisitRequest {
        @NotNull
        private Long studentId;
        /** 可选：指定回访节点；为空则回填该学生最近一个待回访节点 */
        private Long nodeId;
        private java.time.LocalDate actualReturnDate;
        private String seatSituation;
        private String homeworkEyeUse;
        private String outdoorActivity;
        /** ParentAttitude 名称 */
        private String parentAttitude;
        /** 家长仍拒绝时，可同时约定下一次回访日期 */
        private java.time.LocalDate nextPlannedReturnDate;
        /** 回访沟通人员（为空取当前登录人） */
        private String communicator;
        private String remark;
    }

    /** 连续拒绝且风险持续升高时，学校申请班主任/社区医生共同沟通，登记沟通结果与学校干预措施 */
    @Data
    public static class JointCommRequest {
        @NotNull
        private Long studentId;
        /** 申请人（为空取当前登录人） */
        private String jointApplicant;
        /** 参与方，如：三班班主任陈老师、阳光社区卫生服务中心王医生 */
        private String participants;
        private java.time.LocalDateTime jointCommTime;
        /** 共同沟通结果 */
        private String jointResult;
        /** 学校采取的干预措施（可追溯到该学生） */
        private String schoolMeasures;
        private String remark;
    }
}
