package com.county.vision.domain.enums;

/**
 * 学生当前所处的健康管理阶段（平台主状态）
 */
public enum StudentStage {
    PENDING_SCREEN("待筛查"),
    SCREENED("已筛查"),
    ABNORMAL_REVIEW("异常复核中"),        // 学校健康老师复核
    AWAIT_PARENT("待家长确认"),
    AWAIT_VISIT("待复诊"),
    VISITED("已复诊"),
    GLASSES_SUBSIDY("配镜补助中"),
    MANAGING("持续管理"),                // 已配镜/已干预，进入周期复查
    REVIEW_DUE("复查到期"),
    CLOSED_LOOP("本学年闭环"),
    DEFERRED("家长暂缓"),
    REFUSAL_FOLLOW("拒诊跟进"),
    LOST_FOLLOW("失访跟进");

    private final String label;

    StudentStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
