package com.county.vision.domain.enums;

/**
 * 拒诊跟进节点的来源。
 */
public enum RefusalTrigger {
    INITIAL_NOTIFY("首次告知后拒绝复诊"),
    RESCREEN_DROP("后续筛查持续下降，重新提醒"),
    CALLBACK_AFTER("健康回访后仍拒绝"),
    MANUAL("学校手工登记");

    private final String label;

    RefusalTrigger(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
