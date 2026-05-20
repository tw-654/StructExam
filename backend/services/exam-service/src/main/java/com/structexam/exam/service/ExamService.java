package com.structexam.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.structexam.common.entity.Exam;
import com.structexam.common.entity.CodeSubmission;
import com.structexam.common.entity.ExamRecord;
import com.structexam.common.entity.Question;
import com.structexam.common.exception.BusinessException;
import com.structexam.exam.dto.ExamDetailDTO;
import com.structexam.exam.dto.ExamRuntimeDTO;
import com.structexam.exam.dto.ExamSaveRequest;
import com.structexam.exam.dto.ExamStatisticsDTO;
import com.structexam.exam.dto.QuestionDTO;
import com.structexam.exam.dto.QuestionSaveRequest;
import com.structexam.exam.dto.StudentScoreDTO;
import com.structexam.exam.mapper.CodeSubmissionMapper;
import com.structexam.exam.mapper.ExamMapper;
import com.structexam.exam.mapper.ExamRecordMapper;
import com.structexam.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Page<Exam> getExamList(int pageNum, int pageSize, Long userId, String role) {
        Page<Exam> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();

        if ("STUDENT".equals(role)) {
            wrapper.eq(Exam::getStatus, "PUBLISHED")
                    .or()
                    .eq(Exam::getStatus, "ONGOING")
                    .or()
                    .eq(Exam::getStatus, "FINISHED");
        }

        wrapper.orderByDesc(Exam::getCreateTime);
        Page<Exam> result = examMapper.selectPage(page, wrapper);

        LocalDateTime now = LocalDateTime.now();
        for (Exam exam : result.getRecords()) {
            if (now.isBefore(exam.getStartTime())) {
                exam.setStatus("PUBLISHED");
            } else if (now.isAfter(exam.getEndTime())) {
                exam.setStatus("FINISHED");
            } else {
                exam.setStatus("ONGOING");
            }
        }

        return result;
    }

    public Page<Exam> getTeacherExamList(int pageNum, int pageSize, Long teacherId) {
        Page<Exam> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .orderByDesc(Exam::getCreateTime);
        if (teacherId != null) {
            wrapper.eq(Exam::getCreatorId, teacherId);
        }
        return examMapper.selectPage(page, wrapper);
    }

    public Exam createExam(ExamSaveRequest request, Long teacherId) {
        validateExamRequest(request);
        Exam exam = new Exam();
        applyExamRequest(exam, request);
        exam.setCreatorId(teacherId);
        exam.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
        examMapper.insert(exam);
        return exam;
    }

    public Exam updateExam(Long examId, ExamSaveRequest request, Long teacherId) {
        validateExamRequest(request);
        Exam exam = getTeacherOwnedExam(examId, teacherId);
        applyExamRequest(exam, request);
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }
        examMapper.updateById(exam);
        return exam;
    }

    public void deleteExam(Long examId, Long teacherId) {
        getTeacherOwnedExam(examId, teacherId);
        examMapper.deleteById(examId);
    }

    public Exam publishExam(Long examId, Long teacherId) {
        Exam exam = getTeacherOwnedExam(examId, teacherId);
        List<Question> questions = getQuestions(examId);
        if (questions.isEmpty()) {
            throw new BusinessException(400, "发布前至少需要添加一道题目");
        }
        exam.setStatus("PUBLISHED");
        examMapper.updateById(exam);
        return exam;
    }

    public ExamDetailDTO getExamDetail(Long examId, Long userId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "Exam not found");
        }

        ExamDetailDTO detailDTO = new ExamDetailDTO();
        detailDTO.setId(exam.getId());
        detailDTO.setTitle(exam.getTitle());
        detailDTO.setDescription(exam.getDescription());
        detailDTO.setDuration(exam.getDuration());
        detailDTO.setTotalScore(exam.getTotalScore());
        detailDTO.setStartTime(exam.getStartTime());
        detailDTO.setEndTime(exam.getEndTime());
        detailDTO.setStatus(exam.getStatus());

        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getExamId, examId)
                        .orderByAsc(Question::getSortOrder)
        );

        List<QuestionDTO> questionDTOs = questions.stream().map(q -> {
            QuestionDTO dto = new QuestionDTO();
            dto.setId(q.getId());
            dto.setType(q.getType());
            dto.setTitle(q.getTitle());
            dto.setContent(q.getContent());
            dto.setOptions(q.getOptions());
            dto.setScore(q.getScore());
            dto.setSortOrder(q.getSortOrder());
            return dto;
        }).collect(Collectors.toList());

        detailDTO.setQuestions(questionDTOs);

        redisTemplate.opsForValue().set(
                "exam:paper:" + examId,
                detailDTO,
                exam.getDuration() + 30,
                TimeUnit.MINUTES
        );

        return detailDTO;
    }

    public ExamRecord enterExam(Long examId, Long userId, String ipAddress) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "Exam not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new BusinessException(400, "Exam has not started yet");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new BusinessException(400, "Exam has already ended");
        }

        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId);

        ExamRecord record = examRecordMapper.selectOne(wrapper);

        if (record == null) {
            record = new ExamRecord();
            record.setExamId(examId);
            record.setUserId(userId);
            record.setEnterTime(now);
            record.setStatus("IN_PROGRESS");
            record.setIpAddress(ipAddress);
            examRecordMapper.insert(record);
        } else if ("SUBMITTED".equals(record.getStatus()) || "GRADED".equals(record.getStatus())) {
            throw new BusinessException(400, "Exam has already been submitted");
        } else {
            if (record.getEnterTime() == null) {
                record.setEnterTime(now);
            }
            record.setStatus("IN_PROGRESS");
            record.setIpAddress(ipAddress);
            examRecordMapper.updateById(record);
        }

        redisTemplate.opsForHash().put("exam:status:" + examId + ":" + userId, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put("exam:status:" + examId + ":" + userId, "recordId", record.getId().toString());

        return record;
    }

    public ExamRuntimeDTO getExamRuntime(Long examId, Long userId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "Exam not found");
        }
        ExamRecord record = getExamRecord(examId, userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = null;
        long remainingSeconds = 0L;
        if (record != null && "IN_PROGRESS".equals(record.getStatus()) && record.getEnterTime() != null) {
            LocalDateTime personalDeadline = record.getEnterTime().plusMinutes(exam.getDuration());
            deadline = personalDeadline.isBefore(exam.getEndTime()) ? personalDeadline : exam.getEndTime();
            remainingSeconds = Math.max(0L, Duration.between(now, deadline).getSeconds());
        }

        ExamRuntimeDTO dto = new ExamRuntimeDTO();
        dto.setExamId(examId);
        dto.setServerTime(now);
        dto.setDeadlineTime(deadline);
        dto.setRemainingSeconds(remainingSeconds);
        dto.setRecord(record);
        return dto;
    }

    public ExamRecord getExamRecord(Long examId, Long userId) {
        return examRecordMapper.selectOne(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .eq(ExamRecord::getUserId, userId)
        );
    }

    public List<ExamRecord> getExamRecordsByUserId(Long userId) {
        return examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .orderByDesc(ExamRecord::getEnterTime)
        );
    }

    public void submitExam(Long examId, Long userId) {
        ExamRecord record = getExamRecord(examId, userId);
        if (record == null) {
            throw new BusinessException(404, "Exam record not found");
        }

        if ("SUBMITTED".equals(record.getStatus()) || "GRADED".equals(record.getStatus())) {
            throw new BusinessException(400, "Exam has already been submitted");
        }

        record.setSubmitTime(LocalDateTime.now());
        record.setStatus("SUBMITTED");
        examRecordMapper.updateById(record);

        redisTemplate.opsForHash().put("exam:status:" + examId + ":" + userId, "status", "SUBMITTED");
    }

    public List<Question> getQuestions(Long examId) {
        return questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getExamId, examId)
                        .orderByAsc(Question::getSortOrder)
        );
    }

    public Question getQuestionById(Long questionId) {
        return questionMapper.selectById(questionId);
    }

    public Exam getExamById(Long examId) {
        return examMapper.selectById(examId);
    }

    public Question createQuestion(QuestionSaveRequest request, Long teacherId) {
        validateQuestionRequest(request);
        getTeacherOwnedExam(request.getExamId(), teacherId);
        Question question = new Question();
        applyQuestionRequest(question, request);
        questionMapper.insert(question);
        return question;
    }

    public Question updateQuestion(Long questionId, QuestionSaveRequest request, Long teacherId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "Question not found");
        }
        Long examId = request.getExamId() == null ? question.getExamId() : request.getExamId();
        getTeacherOwnedExam(examId, teacherId);
        validateQuestionRequest(request);
        request.setExamId(examId);
        applyQuestionRequest(question, request);
        questionMapper.updateById(question);
        return question;
    }

    public void deleteQuestion(Long questionId, Long teacherId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return;
        }
        getTeacherOwnedExam(question.getExamId(), teacherId);
        questionMapper.deleteById(questionId);
    }

    public List<StudentScoreDTO> getStudentScores(Long examId, Long teacherId) {
        getTeacherOwnedExam(examId, teacherId);
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .orderByDesc(ExamRecord::getSubmitTime));
        Map<Long, List<CodeSubmission>> submissionsByUser = codeSubmissionMapper.selectList(new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, examId))
                .stream()
                .collect(Collectors.groupingBy(CodeSubmission::getUserId));

        return records.stream().map(record -> {
            List<CodeSubmission> submissions = submissionsByUser.getOrDefault(record.getUserId(), List.of());
            StudentScoreDTO dto = new StudentScoreDTO();
            dto.setRecordId(record.getId());
            dto.setExamId(record.getExamId());
            dto.setUserId(record.getUserId());
            dto.setEnterTime(record.getEnterTime());
            dto.setSubmitTime(record.getSubmitTime());
            dto.setScore(record.getScore());
            dto.setStatus(record.getStatus());
            dto.setSubmittedQuestionCount((int) submissions.stream()
                    .filter(s -> "SUBMITTED".equals(s.getStatus()) || "GRADED".equals(s.getStatus()))
                    .count());
            dto.setJudgedQuestionCount((int) submissions.stream()
                    .filter(s -> s.getJudgeStatus() != null)
                    .count());
            dto.setAcceptedQuestionCount((int) submissions.stream()
                    .filter(s -> "AC".equals(s.getJudgeStatus()))
                    .count());
            submissions.stream()
                    .filter(s -> s.getJudgeTime() != null)
                    .max((left, right) -> left.getJudgeTime().compareTo(right.getJudgeTime()))
                    .ifPresent(latest -> {
                        dto.setLatestJudgeStatus(latest.getJudgeStatus());
                        dto.setLatestJudgeTimeUsedMs(latest.getTimeUsedMs());
                        dto.setLatestJudgeTime(latest.getJudgeTime());
                    });
            return dto;
        }).collect(Collectors.toList());
    }

    public ExamStatisticsDTO getExamStatistics(Long examId, Long teacherId) {
        Exam exam = getTeacherOwnedExam(examId, teacherId);
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId));

        ExamStatisticsDTO dto = new ExamStatisticsDTO();
        dto.setExamId(examId);
        dto.setTitle(exam.getTitle());
        dto.setTotalStudents(records.size());
        dto.setInProgressCount((int) records.stream().filter(r -> "IN_PROGRESS".equals(r.getStatus())).count());
        dto.setSubmittedCount((int) records.stream().filter(r -> "SUBMITTED".equals(r.getStatus())).count());
        dto.setGradedCount((int) records.stream().filter(r -> "GRADED".equals(r.getStatus())).count());

        List<Integer> scores = records.stream()
                .map(ExamRecord::getScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        dto.setAverageScore(scores.isEmpty() ? 0D : scores.stream().mapToInt(Integer::intValue).average().orElse(0D));
        dto.setMaxScore(scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).max().orElse(0));
        dto.setMinScore(scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).min().orElse(0));
        return dto;
    }

    public int gradeObjective(Long examId, Long teacherId) {
        getTeacherOwnedExam(examId, teacherId);
        List<Question> questions = getQuestions(examId);
        Map<Long, Integer> scoreByQuestion = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q.getScore() == null ? 0 : q.getScore()));
        Set<Long> questionIds = scoreByQuestion.keySet();
        if (questionIds.isEmpty()) {
            return 0;
        }

        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .in(ExamRecord::getStatus, List.of("SUBMITTED", "GRADED")));
        List<CodeSubmission> submissions = codeSubmissionMapper.selectList(new LambdaQueryWrapper<CodeSubmission>()
                .eq(CodeSubmission::getExamId, examId)
                .in(CodeSubmission::getStatus, List.of("SUBMITTED", "GRADED")));
        Map<Long, Set<Long>> acceptedQuestionsByUser = submissions.stream()
                .filter(s -> questionIds.contains(s.getQuestionId()))
                .filter(s -> "AC".equals(s.getJudgeStatus()) || s.getJudgeStatus() == null)
                .collect(Collectors.groupingBy(
                        CodeSubmission::getUserId,
                        Collectors.mapping(CodeSubmission::getQuestionId, Collectors.toSet())
                ));

        for (ExamRecord record : records) {
            int score = acceptedQuestionsByUser.getOrDefault(record.getUserId(), Set.of())
                    .stream()
                    .mapToInt(questionId -> scoreByQuestion.getOrDefault(questionId, 0))
                    .sum();
            record.setScore(score);
            record.setStatus("GRADED");
            examRecordMapper.updateById(record);
        }
        return records.size();
    }

    private Exam getTeacherOwnedExam(Long examId, Long teacherId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "Exam not found");
        }
        if (teacherId != null && !teacherId.equals(exam.getCreatorId())) {
            throw new BusinessException(403, "No permission to manage this exam");
        }
        return exam;
    }

    private void applyExamRequest(Exam exam, ExamSaveRequest request) {
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setDuration(request.getDuration());
        exam.setTotalScore(request.getTotalScore());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
    }

    private void validateExamRequest(ExamSaveRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(400, "考试名称不能为空");
        }
        if (request.getDuration() == null || request.getDuration() <= 0) {
            throw new BusinessException(400, "考试时长必须大于0");
        }
        if (request.getTotalScore() == null || request.getTotalScore() <= 0) {
            throw new BusinessException(400, "总分必须大于0");
        }
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(400, "考试开始和结束时间不合法");
        }
    }

    private void applyQuestionRequest(Question question, QuestionSaveRequest request) {
        question.setExamId(request.getExamId());
        question.setType(request.getType());
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setOptions(normalizeOptions(request));
        question.setScore(request.getScore());
        question.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private void validateQuestionRequest(QuestionSaveRequest request) {
        if (request == null || request.getExamId() == null) {
            throw new BusinessException(400, "examId is required");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException(400, "题目类型不能为空");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(400, "题目标题不能为空");
        }
        if (request.getScore() == null || request.getScore() <= 0) {
            throw new BusinessException(400, "题目分值必须大于0");
        }
    }

    private String normalizeOptions(QuestionSaveRequest request) {
        if (request.getOptions() == null || request.getOptions().isBlank()) {
            return "PROGRAMMING".equals(request.getType()) ? null : "[]";
        }
        return request.getOptions();
    }
}
