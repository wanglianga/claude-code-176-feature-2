package com.county.vision.domain.enums;

/**
 * 家长拒绝复诊时的归因分类（其口头理由归到哪一类，便于学校统计与干预）。
 */
public enum RefusalReasonType {
    FATIGUE("认为只是疲劳用眼/假性近视"),
    TEMPORARY("认为过段时间会自行恢复"),
    NO_TIME("家长工作忙、没时间带孩子就诊"),
    COST("担心费用/不愿承担"),
    DISTRUST("不信任筛查结果或医院"),
    ALREADY_GLASSES("已自行配镜，认为无需复诊"),
    OTHER("其他原因");

    private final String label;

    RefusalReasonType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
