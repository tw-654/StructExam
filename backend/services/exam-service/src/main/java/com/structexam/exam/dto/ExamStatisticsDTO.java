package com.structexam.exam.dto;

import java.util.List;
import java.util.Map;

public class ExamStatisticsDTO {
    private Long examId;
    private String title;
    private Integer totalStudents;
    private Integer inProgressCount;
    private Integer submittedCount;
    private Integer gradedCount;
    private Double averageScore;
    private Integer maxScore;
    private Integer minScore;
    private Double passRate;
    private Integer passCount;
    private Integer failCount;
    private Double medianScore;
    private Double stdDev;
    private Map<String, Integer> scoreDistributionMap;
    private List<ScoreRangeDTO> scoreRanges;
    private Map<String, Double> gradeDistribution;

    /** 按得分率分段的成绩分布 */
    private List<ScoreDistributionBucketDTO> scoreDistribution;

    public static class ScoreRangeDTO {
        private String range;
        private Integer count;
        private Double percentage;

        public ScoreRangeDTO() {}

        public ScoreRangeDTO(String range, Integer count, Double percentage) {
            this.range = range;
            this.count = count;
            this.percentage = percentage;
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

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Integer getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(Integer inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    public Integer getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(Integer gradedCount) {
        this.gradedCount = gradedCount;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getMinScore() {
        return minScore;
    }

    public void setMinScore(Integer minScore) {
        this.minScore = minScore;
    }

    public List<ScoreDistributionBucketDTO> getScoreDistribution() {
        return scoreDistribution;
    }

    public void setScoreDistribution(List<ScoreDistributionBucketDTO> scoreDistribution) {
        this.scoreDistribution = scoreDistribution;
    }

    public Map<String, Integer> getScoreDistributionMap() {
        return scoreDistributionMap;
    }

    public void setScoreDistributionMap(Map<String, Integer> scoreDistributionMap) {
        this.scoreDistributionMap = scoreDistributionMap;
    }

    public List<ScoreRangeDTO> getScoreRanges() {
        return scoreRanges;
    }

    public void setScoreRanges(List<ScoreRangeDTO> scoreRanges) {
        this.scoreRanges = scoreRanges;
    }

    public Map<String, Double> getGradeDistribution() {
        return gradeDistribution;
    }

    public void setGradeDistribution(Map<String, Double> gradeDistribution) {
        this.gradeDistribution = gradeDistribution;
    }

    public Double getPassRate() {
        return passRate;
    }

    public void setPassRate(Double passRate) {
        this.passRate = passRate;
    }

    public Integer getPassCount() {
        return passCount;
    }

    public void setPassCount(Integer passCount) {
        this.passCount = passCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Double getMedianScore() {
        return medianScore;
    }

    public void setMedianScore(Double medianScore) {
        this.medianScore = medianScore;
    }

    public Double getStdDev() {
        return stdDev;
    }

    public void setStdDev(Double stdDev) {
        this.stdDev = stdDev;
    }
}
