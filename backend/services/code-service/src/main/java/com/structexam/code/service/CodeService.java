package com.structexam.code.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.structexam.code.mapper.CodeSubmissionMapper;
import com.structexam.common.dto.CodeSaveRequest;
import com.structexam.common.entity.CodeSubmission;
import com.structexam.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class CodeService {

    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveCode(Long userId, CodeSaveRequest request) {
        String redisKey = buildRedisKey(request.getExamId(), userId, request.getQuestionId());
        redisTemplate.opsForValue().set(redisKey, request.getCode(), 4, TimeUnit.HOURS);

        LambdaQueryWrapper<CodeSubmission> wrapper = new LambdaQueryWrapper<CodeSubmission>()
                .eq(CodeSubmission::getExamId, request.getExamId())
                .eq(CodeSubmission::getUserId, userId)
                .eq(CodeSubmission::getQuestionId, request.getQuestionId());

        CodeSubmission submission = codeSubmissionMapper.selectOne(wrapper);

        if (submission == null) {
            submission = new CodeSubmission();
            submission.setExamId(request.getExamId());
            submission.setUserId(userId);
            submission.setQuestionId(request.getQuestionId());
            submission.setCodeContent(request.getCode());
            submission.setLanguage(request.getLanguage() != null ? request.getLanguage() : "java");
            submission.setStatus("SAVED");
            codeSubmissionMapper.insert(submission);
        } else {
            submission.setCodeContent(request.getCode());
            submission.setLanguage(request.getLanguage() != null ? request.getLanguage() : submission.getLanguage());
            codeSubmissionMapper.updateById(submission);
        }
    }

    public String getCode(Long examId, Long userId, Long questionId) {
        String redisKey = buildRedisKey(examId, userId, questionId);
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);

        if (cachedCode != null) {
            return cachedCode.toString();
        }

        CodeSubmission submission = codeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, examId)
                        .eq(CodeSubmission::getUserId, userId)
                        .eq(CodeSubmission::getQuestionId, questionId)
        );

        if (submission != null) {
            redisTemplate.opsForValue().set(redisKey, submission.getCodeContent(), 4, TimeUnit.HOURS);
            return submission.getCodeContent();
        }

        return null;
    }

    public void submitCode(Long userId, Long examId, Long questionId) {
        String redisKey = buildRedisKey(examId, userId, questionId);
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);

        CodeSubmission submission = codeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, examId)
                        .eq(CodeSubmission::getUserId, userId)
                        .eq(CodeSubmission::getQuestionId, questionId)
        );

        if (submission == null) {
            if (cachedCode != null) {
                submission = new CodeSubmission();
                submission.setExamId(examId);
                submission.setUserId(userId);
                submission.setQuestionId(questionId);
                submission.setCodeContent(cachedCode.toString());
                submission.setLanguage("java");
                submission.setStatus("SUBMITTED");
                submission.setSubmitTime(LocalDateTime.now());
                codeSubmissionMapper.insert(submission);
            } else {
                throw new BusinessException(400, "No code to submit");
            }
        } else {
            if (cachedCode != null) {
                submission.setCodeContent(cachedCode.toString());
            }
            submission.setStatus("SUBMITTED");
            submission.setSubmitTime(LocalDateTime.now());
            codeSubmissionMapper.updateById(submission);
        }

        redisTemplate.delete(redisKey);
    }

    public void submitAllCode(Long userId, Long examId) {
        java.util.List<CodeSubmission> submissions = codeSubmissionMapper.selectList(
                new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, examId)
                        .eq(CodeSubmission::getUserId, userId)
        );

        for (CodeSubmission submission : submissions) {
            String redisKey = buildRedisKey(examId, userId, submission.getQuestionId());
            Object cachedCode = redisTemplate.opsForValue().get(redisKey);

            if (cachedCode != null) {
                submission.setCodeContent(cachedCode.toString());
                submission.setStatus("SUBMITTED");
                submission.setSubmitTime(LocalDateTime.now());
                codeSubmissionMapper.updateById(submission);
                redisTemplate.delete(redisKey);
            } else if (!"SUBMITTED".equals(submission.getStatus())) {
                submission.setStatus("SUBMITTED");
                submission.setSubmitTime(LocalDateTime.now());
                codeSubmissionMapper.updateById(submission);
            }
        }
    }

    private String buildRedisKey(Long examId, Long userId, Long questionId) {
        return String.format("code:temp:%d:%d:%d", examId, userId, questionId);
    }
}
