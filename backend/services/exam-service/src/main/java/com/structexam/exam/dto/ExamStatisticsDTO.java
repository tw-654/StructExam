package com.structexam.exam.dto;

import java.util.List;

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

    /** 按得分率分段的成绩分布 */
    private List<ScoreDistributionBucketDTO> scoreDistribution;

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
}
