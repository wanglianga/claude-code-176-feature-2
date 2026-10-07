package com.county.vision.domain.enums;

/**
 * 家长拒绝复诊跟进链路上的节点类型。
 * 每次拒绝都是一个独立节点，不允许覆盖第一次记录；
 * 回访、重新提醒、多方（班主任/社区医生）沟通同样形成可追溯节点。
 */
public enum NodeType {
    REFUSAL("家长拒绝复诊"),
    RETURN_VISIT("约定回访"),
    RE_REMIND("复诊再提醒"),
    JOINT_COMM("多方共同沟通");

    private final String label;

    NodeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
