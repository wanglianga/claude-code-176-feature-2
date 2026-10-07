package com.county.vision.domain.enums;

/**
 * 回访时家长对复诊/孩子视力问题的当前态度，用于观察家长态度变化趋势。
 */
public enum ParentAttitude {
    STILL_REFUSE("仍坚持拒绝"),
    HESITATE("犹豫、开始重视"),
    WILLING("愿意安排复诊"),
    ALREADY_VISITED("已自行复诊"),
    UNREACHABLE("本次未能联系上");

    private final String label;

    ParentAttitude(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
