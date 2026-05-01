package com.structexam.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.structexam.common.entity.Exam;
import com.structexam.common.entity.ExamRecord;
import com.structexam.common.entity.Question;
import com.structexam.common.exception.BusinessException;
import com.structexam.exam.dto.ExamDetailDTO;
import com.structexam.exam.dto.QuestionDTO;
import com.structexam.exam.mapper.ExamMapper;
import com.structexam.exam.mapper.ExamRecordMapper;
import com.structexam.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        return examMapper.selectPage(page, wrapper);
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
            record.setEnterTime(now);
            record.setStatus("IN_PROGRESS");
            examRecordMapper.updateById(record);
        }

        redisTemplate.opsForHash().put("exam:status:" + examId + ":" + userId, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put("exam:status:" + examId + ":" + userId, "recordId", record.getId().toString());

        return record;
    }

    public ExamRecord getExamRecord(Long examId, Long userId) {
        return examRecordMapper.selectOne(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, examId)
                        .eq(ExamRecord::getUserId, userId)
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
}
