package com.county.vision.domain.enums;

/**
 * 连续拒绝且风险持续升高时，学校申请的共同沟通对象。
 */
public enum JointCommType {
    HEAD_TEACHER("班主任共同沟通"),
    COMMUNITY_DOCTOR("社区医生共同沟通"),
    BOTH("班主任与社区医生共同沟通");

    private final String label;

    JointCommType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
