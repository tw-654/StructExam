package com.structexam.exam.dto;

/**
 * 成绩分布统计中的一个得分率区间及人数。
 */
public class ScoreDistributionBucketDTO {

    private String range;
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
