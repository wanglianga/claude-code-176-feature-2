package com.county.vision.domain.enums;

/**
 * 家长拒绝复诊的常见原因归类。
 * 典型场景：家长认为孩子只是疲劳用眼、假性近视，拒绝到医院复诊。
 */
public enum RefusalReason {
    EYE_FATIGUE("只是疲劳用眼，休息即可"),
    PSEUDO_MYOPIA("认为是假性近视，无需就诊"),
    TOO_YOUNG("孩子年龄小，不急着看"),
    TIME_CONFLICT("家长工作忙/没时间陪同"),
    COST("担心就诊/配镜费用"),
    DISTANCE("医院距离远、就诊不便"),
    ALREADY_WEARING("已配镜/在外面看过，无需复诊"),
    OTHER("其他原因");

    private final String label;

    RefusalReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
