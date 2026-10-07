package com.county.vision.domain.enums;

/**
 * 健康老师回访时家长的当前态度。
 */
public enum ParentAttitude {
    STILL_REFUSE("仍拒绝复诊"),
    HESITATE("犹豫，继续动员"),
    WILL_OBSERVE("愿意先调整用眼习惯再观察"),
    AGREED("已同意复诊"),
    UNREACHABLE("仍联系不上");

    private final String label;

    ParentAttitude(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
