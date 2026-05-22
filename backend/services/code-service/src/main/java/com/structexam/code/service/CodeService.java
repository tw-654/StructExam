package com.structexam.code.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structexam.code.distributed.dto.DistributedJudgeSubmitRequest;
import com.structexam.code.distributed.dto.JudgeTaskResponse;
import com.structexam.code.distributed.service.DistributedJudgeService;
import com.structexam.code.mapper.CodeSubmissionMapper;
import com.structexam.code.mapper.QuestionMapper;
import com.structexam.code.mapper.QuestionTestCaseMapper;
import com.structexam.common.dto.CodeSaveRequest;
import com.structexam.common.dto.TestCase;
import com.structexam.common.entity.CodeSubmission;
import com.structexam.common.entity.Question;
import com.structexam.common.entity.QuestionTestCase;
import com.structexam.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CodeService {

    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DistributedJudgeService distributedJudgeService;

    @Autowired
    private QuestionTestCaseMapper questionTestCaseMapper;

    @Autowired
    private ObjectMapper objectMapper;

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

    public JudgeTaskResponse submitCode(Long userId, CodeSaveRequest request) {
        if (request == null || request.getExamId() == null || request.getQuestionId() == null) {
            throw new BusinessException(400, "examId and questionId are required");
        }
        Long examId = request.getExamId();
        Long questionId = request.getQuestionId();
        String redisKey = buildRedisKey(examId, userId, questionId);
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);

        CodeSubmission submission = codeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, examId)
                        .eq(CodeSubmission::getUserId, userId)
                        .eq(CodeSubmission::getQuestionId, questionId)
        );

        String code = resolveSubmittedCode(request.getCode(), cachedCode, submission);
        String language = resolveLanguage(request.getLanguage(), submission);
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "No code to submit");
        }

        Question question = questionMapper.selectById(questionId);
        CodeSubmission savedSubmission = upsertSubmittedCode(userId, examId, questionId, code, language, submission);

        DistributedJudgeSubmitRequest judgeRequest = new DistributedJudgeSubmitRequest();
        judgeRequest.setExamId(examId);
        judgeRequest.setQuestionId(questionId);
        judgeRequest.setCode(code);
        judgeRequest.setLanguage(language);
        judgeRequest.setTestCases(parseTestCases(question));
        judgeRequest.setMaxScore(question == null || question.getScore() == null ? 0 : question.getScore());
        judgeRequest.setPersistResult(true);
        judgeRequest.setSubmissionId(savedSubmission.getId());
        // triggerType 由调用方通过 request 传入，默认为 SUBMIT
        if (request.getTriggerType() != null) {
            judgeRequest.setTriggerType(request.getTriggerType());
        }

        JudgeTaskResponse response = distributedJudgeService.submit(userId, judgeRequest);
        redisTemplate.delete(redisKey);
        return response;
    }

    public JudgeTaskResponse submitCode(Long userId, Long examId, Long questionId) {
        CodeSaveRequest request = new CodeSaveRequest();
        request.setExamId(examId);
        request.setQuestionId(questionId);
        return submitCode(userId, request);
    }

    /** 写入或更新 t_code_submission，返回包含 id 的实体（用于 JudgeRecord.submissionId 关联）。 */
    private CodeSubmission upsertSubmittedCode(Long userId, Long examId, Long questionId,
                                               String code, String language, CodeSubmission submission) {
        if (submission == null) {
            submission = new CodeSubmission();
            submission.setExamId(examId);
            submission.setUserId(userId);
            submission.setQuestionId(questionId);
            submission.setCodeContent(code);
            submission.setLanguage(language);
            submission.setStatus("SUBMITTED");
            submission.setSubmitTime(LocalDateTime.now());
            codeSubmissionMapper.insert(submission);   // MyBatis-Plus AUTO 策略回填 id
        } else {
            submission.setCodeContent(code);
            submission.setLanguage(language);
            submission.setStatus("SUBMITTED");
            submission.setSubmitTime(LocalDateTime.now());
            submission.setJudgeStatus(null);
            submission.setJudgeTime(null);
            submission.setJudgeMessage(null);
            submission.setTimeUsedMs(null);
            submission.setMemoryUsedKb(null);
            codeSubmissionMapper.updateById(submission);
        }
        return submission;
    }

    public List<JudgeTaskResponse> submitAllCode(Long userId, Long examId) {
        java.util.List<CodeSubmission> submissions = codeSubmissionMapper.selectList(
                new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, examId)
                        .eq(CodeSubmission::getUserId, userId)
        );

        List<JudgeTaskResponse> responses = new ArrayList<>();
        for (CodeSubmission submission : submissions) {
            String redisKey = buildRedisKey(examId, userId, submission.getQuestionId());
            Object cachedCode = redisTemplate.opsForValue().get(redisKey);
            boolean hasNewCachedCode = cachedCode != null && StringUtils.hasText(cachedCode.toString());
            boolean needsJudge = hasNewCachedCode || submission.getJudgeStatus() == null;
            if (!needsJudge) {
                continue;
            }

            CodeSaveRequest request = new CodeSaveRequest();
            request.setExamId(examId);
            request.setQuestionId(submission.getQuestionId());
            request.setCode(hasNewCachedCode ? cachedCode.toString() : submission.getCodeContent());
            request.setLanguage(submission.getLanguage());
            request.setTriggerType("SUBMIT_ALL");
            try {
                responses.add(submitCode(userId, request));
            } catch (BusinessException ex) {
                if (ex.getCode() == null || ex.getCode() != 409) {
                    throw ex;
                }
            }
        }
        return responses;
    }

    private String resolveSubmittedCode(String requestCode, Object cachedCode, CodeSubmission submission) {
        if (StringUtils.hasText(requestCode)) {
            return requestCode;
        }
        if (cachedCode != null && StringUtils.hasText(cachedCode.toString())) {
            return cachedCode.toString();
        }
        return submission == null ? null : submission.getCodeContent();
    }

    private String resolveLanguage(String requestLanguage, CodeSubmission submission) {
        if (StringUtils.hasText(requestLanguage)) {
            return requestLanguage;
        }
        if (submission != null && StringUtils.hasText(submission.getLanguage())) {
            return submission.getLanguage();
        }
        return "java";
    }

    private List<TestCase> parseTestCases(Question question) {
        if (question == null) {
            return List.of();
        }

        // 优先从 t_question_test_case 读取（新表），空则 fallback 解析 options JSON
        List<QuestionTestCase> dbCases = questionTestCaseMapper.selectList(
                new LambdaQueryWrapper<QuestionTestCase>()
                        .eq(QuestionTestCase::getQuestionId, question.getId())
                        .eq(QuestionTestCase::getStatus, 1)
                        .orderByAsc(QuestionTestCase::getSortOrder));
        if (!dbCases.isEmpty()) {
            return dbCases.stream().map(tc -> {
                TestCase t = new TestCase();
                t.setInput(tc.getInputData());
                t.setExpectedOutput(tc.getExpectedOutput());
                t.setDescription(tc.getCaseName());
                return t;
            }).collect(java.util.stream.Collectors.toList());
        }

        // Fallback：解析 t_question.options 中的旧 JSON 格式
        if (!StringUtils.hasText(question.getOptions())) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(question.getOptions());
            JsonNode testCaseNode = root.isArray() ? root : root.get("testCases");
            if (testCaseNode == null || !testCaseNode.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(testCaseNode, new TypeReference<List<TestCase>>() {});
        } catch (Exception ex) {
            throw new BusinessException(400, "题目测试用例 JSON 配置不合法");
        }
    }

    private String buildRedisKey(Long examId, Long userId, Long questionId) {
        return String.format("code:temp:%d:%d:%d", examId, userId, questionId);
    }
}
