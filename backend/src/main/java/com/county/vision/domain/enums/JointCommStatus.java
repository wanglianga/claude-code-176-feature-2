package com.county.vision.domain.enums;

/**
 * 共同沟通申请的处理状态。
 */
public enum JointCommStatus {
    APPLIED("已申请"),
    ACCEPTED("已受理"),
    DONE("沟通完成"),
    REJECTED("未受理");

    private final String label;

    JointCommStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
