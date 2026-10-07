package com.county.vision.domain.enums;

/**
 * 家长拒绝复诊跟进节点的处理结果。
 * 家长再次拒绝时不会覆盖上一条记录，而是新增一个节点。
 */
public enum RefusalOutcome {
    REFUSED("家长拒绝复诊"),
    RE_REMINDED("已重新提醒复诊"),
    CALLBACK_DONE("已回访"),
    AGREED_VISIT("已同意复诊"),
    JOINT_COMM("已申请共同沟通"),
    JOINT_DONE("共同沟通完成");

    private final String label;

    RefusalOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
