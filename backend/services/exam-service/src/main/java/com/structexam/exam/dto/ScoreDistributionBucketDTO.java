package com.structexam.exam.dto;

/**
 * 成绩分布统计中的一个得分率区间及人数。
 * range 示例：&lt;60%、60%-70%、…；count 为落入该区间的学生人数（见 ExamService.buildScoreDistribution）。
 */
public class ScoreDistributionBucketDTO {

    /** 得分率区间标签，与前端柱状图横轴一致 */
    private String range;
    /** 该区间内学生人数 */
    private Integer count;

    public ScoreDistributionBucketDTO() {
    }

    public ScoreDistributionBucketDTO(String range, Integer count) {
        this.range = range;
        this.count = count;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
