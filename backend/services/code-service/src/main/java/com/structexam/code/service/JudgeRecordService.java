package com.structexam.code.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.structexam.code.distributed.dto.JudgeCaseResultVO;
import com.structexam.code.distributed.dto.JudgeRecordVO;
import com.structexam.code.distributed.dto.JudgeResult;
import com.structexam.code.distributed.dto.JudgeTask;
import com.structexam.code.distributed.dto.JudgeTaskStatus;
import com.structexam.code.judge.CaseJudgeRequest;
import com.structexam.code.judge.CaseJudgeResult;
import com.structexam.code.judge.TestCaseJudgeService;
import com.structexam.code.mapper.JudgeCaseResultMapper;
import com.structexam.code.mapper.JudgeRecordMapper;
import com.structexam.code.mapper.QuestionTestCaseMapper;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.entity.JudgeCaseResult;
import com.structexam.common.entity.JudgeRecord;
import com.structexam.common.entity.QuestionTestCase;
import com.structexam.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeRecordService {

    @Autowired
    private JudgeRecordMapper recordMapper;

    @Autowired
    private JudgeCaseResultMapper caseResultMapper;

    @Autowired
    private QuestionTestCaseMapper questionTestCaseMapper;

    @Autowired
    private TestCaseJudgeService testCaseJudgeService;

    /**
     * 判题入队时预创建一条 JUDGING 状态的记录，返回 ID 供后续关联。
     * 只在 persistResult=true 时调用（官方提交，不含纯"运行"）。
     */
    public JudgeRecord createPending(JudgeTask task, String triggerType) {
        JudgeRecord rec = new JudgeRecord();
        rec.setTaskId(task.getTaskId());
        rec.setSubmissionId(task.getSubmissionId());
        rec.setExamId(task.getExamId());
        rec.setUserId(task.getUserId());
        rec.setQuestionId(task.getQuestionId());
        rec.setLanguage(task.getLanguage());
        rec.setCodeSnapshot(task.getCode());
        rec.setTriggerType(triggerType != null ? triggerType : "SUBMIT");
        rec.setJudgeStatus("JUDGING");
        rec.setTotalCases(0);
        rec.setPassedCases(0);
        rec.setScore(0);
        rec.setMaxScore(task.getMaxScore() != null ? task.getMaxScore() : 0);
        rec.setStartedTime(LocalDateTime.now());
        recordMapper.insert(rec);
        return rec;
    }

    /**
     * 判题调度器拿到 sandbox 结果后调用：
     * 1. 按 taskId 找到 JudgeRecord（幂等，已完成则跳过）
     * 2. 用 sandbox 返回的逐条结果与题目原始用例做对齐
     * 3. 写入 t_judge_case_result 明细
     * 4. 更新 t_judge_record 聚合字段
     *
     * @param task   判题任务（含 examId/questionId/maxScore）
     * @param result 调度器拿到的判题结果
     */
    @Transactional
    public void completeJudge(JudgeTask task, JudgeResult result) {
        JudgeRecord rec = recordMapper.selectOne(
                new LambdaQueryWrapper<JudgeRecord>()
                        .eq(JudgeRecord::getTaskId, task.getTaskId()));
        if (rec == null) {
            return;
        }
        // 幂等：已经写完就不再重复
        if (!"JUDGING".equals(rec.getJudgeStatus())) {
            return;
        }

        // 加载题目用例，用于对齐 testCaseId / is_public / case score
        List<QuestionTestCase> sourceCases = questionTestCaseMapper.selectList(
                new LambdaQueryWrapper<QuestionTestCase>()
                        .eq(QuestionTestCase::getQuestionId, task.getQuestionId())
                        .eq(QuestionTestCase::getStatus, 1)
                        .orderByAsc(QuestionTestCase::getSortOrder));

        List<CodeExecuteResponse.TestResult> testResults = result.getTestCaseResults();
        List<JudgeCaseResult> caseEntities = new ArrayList<>();

        int passedCount = 0;
        int caseScore = 0;

        // 全局任务状态标志，供逐条用例的判定服务使用
        String overallStatus = result.getStatus() != null ? result.getStatus().name() : "";
        boolean isOverallCE = JudgeTaskStatus.CE.name().equals(overallStatus);
        boolean isOverallRE = JudgeTaskStatus.RE.name().equals(overallStatus);

        if (testResults != null) {
            for (int i = 0; i < testResults.size(); i++) {
                CodeExecuteResponse.TestResult tr = testResults.get(i);
                JudgeCaseResult cr = new JudgeCaseResult();
                cr.setJudgeRecordId(rec.getId());
                cr.setCaseIndex(i);

                // 从题目配置中取用例元信息（时间限制、内存限制、分值等）
                Long timeLimitMs  = null;
                Long memLimitKb   = null;
                if (i < sourceCases.size()) {
                    QuestionTestCase src = sourceCases.get(i);
                    cr.setTestCaseId(src.getId());
                    cr.setCaseName(src.getCaseName());
                    cr.setIsPublic(Boolean.TRUE.equals(src.getIsPublic()));
                    cr.setScore(src.getScore() != null ? src.getScore() : 0);
                    timeLimitMs = src.getTimeLimitMs() != null ? src.getTimeLimitMs().longValue() : null;
                    memLimitKb  = src.getMemoryLimitKb() != null ? src.getMemoryLimitKb().longValue() : null;
                } else {
                    // 来自 options JSON fallback 或自定义输入，按公开处理
                    cr.setIsPublic(true);
                    cr.setScore(0);
                }

                // 调用核心判定服务（自主标准化 + 超时/RE/CE 推断，不依赖沙箱 isPassed()）
                CaseJudgeRequest judgeReq = CaseJudgeRequest.builder()
                        .actualOutput(tr.getActualOutput())
                        .expectedOutput(tr.getExpectedOutput())
                        .timeUsedMs(tr.getExecutionTime())
                        .timeLimitMs(timeLimitMs)
                        .memoryLimitKb(memLimitKb)
                        .compileError(isOverallCE)
                        .runtimeError(isOverallRE)
                        .errorMessage(result.getError())
                        .build();
                CaseJudgeResult judged = testCaseJudgeService.judge(judgeReq);

                cr.setPassed(judged.isPassed());
                cr.setStatus(judged.getStatus().dbCode());
                cr.setInputData(tr.getInput());
                cr.setExpectedOutput(judged.getNormalizedExpected());
                cr.setActualOutput(judged.getNormalizedActual());
                cr.setTimeUsedMs(tr.getExecutionTime());
                if (judged.getErrorMessage() != null) {
                    cr.setErrorMessage(judged.getErrorMessage());
                }

                if (judged.isPassed()) {
                    passedCount++;
                    caseScore += (cr.getScore() != null ? cr.getScore() : 0);
                }
                caseEntities.add(cr);
            }
        }

        String statusName = result.getStatus() != null ? result.getStatus().name() : "FAILED";
        int maxScore = task.getMaxScore() != null ? task.getMaxScore() : 0;
        // AC 得满分；WA/CE/RE 按累计用例分或 0
        int finalScore = JudgeTaskStatus.AC.name().equals(statusName) ? maxScore : caseScore;

        // 更新 JudgeRecord
        rec.setJudgeStatus(statusName);
        rec.setTotalCases(testResults != null ? testResults.size() : 0);
        rec.setPassedCases(passedCount);
        rec.setScore(finalScore);
        rec.setMaxScore(maxScore);
        rec.setTimeUsedMs(result.getTimeUsedMs());
        rec.setSandboxNode(result.getSandboxNodeUri());
        rec.setFinishedTime(result.getFinishedTime() != null ? result.getFinishedTime() : LocalDateTime.now());

        if (JudgeTaskStatus.CE.name().equals(statusName)) {
            rec.setCompileError(result.getError());
        } else if (JudgeTaskStatus.RE.name().equals(statusName)) {
            rec.setRuntimeError(result.getError());
        } else {
            rec.setJudgeMessage(result.getError());
        }

        recordMapper.updateById(rec);

        // 写入用例明细（仅首次）
        for (JudgeCaseResult cr : caseEntities) {
            caseResultMapper.insert(cr);
        }
    }

    /**
     * 学生侧：查询某题最近一次判题（含逐条用例，非公开失败用例脱敏）。
     */
    public JudgeRecordVO getLatestForStudent(Long examId, Long userId, Long questionId) {
        JudgeRecord rec = recordMapper.selectOne(
                new LambdaQueryWrapper<JudgeRecord>()
                        .eq(JudgeRecord::getExamId, examId)
                        .eq(JudgeRecord::getUserId, userId)
                        .eq(JudgeRecord::getQuestionId, questionId)
                        .orderByDesc(JudgeRecord::getCreateTime)
                        .last("LIMIT 1"));
        if (rec == null) {
            throw new BusinessException(404, "暂无判题记录");
        }
        return toVO(rec, false);
    }

    /**
     * 按 taskId 查判题记录（学生只能查自己的，教师/管理员可见完整内容）。
     */
    public JudgeRecordVO getByTaskId(String taskId, Long userId, String role) {
        JudgeRecord rec = recordMapper.selectOne(
                new LambdaQueryWrapper<JudgeRecord>()
                        .eq(JudgeRecord::getTaskId, taskId));
        if (rec == null) {
            throw new BusinessException(404, "判题记录不存在");
        }
        boolean isStaff = "TEACHER".equals(role) || "ADMIN".equals(role);
        if (!isStaff && !rec.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看此判题记录");
        }
        return toVO(rec, isStaff);
    }

    /**
     * 教师侧：查询某次代码提交的所有历史判题（含重判）。
     */
    public List<JudgeRecordVO> listBySubmission(Long submissionId) {
        List<JudgeRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<JudgeRecord>()
                        .eq(JudgeRecord::getSubmissionId, submissionId)
                        .orderByDesc(JudgeRecord::getCreateTime));
        return records.stream().map(r -> toVO(r, true)).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- helpers

    private JudgeRecordVO toVO(JudgeRecord rec, boolean fullDetail) {
        JudgeRecordVO vo = new JudgeRecordVO();
        vo.setId(rec.getId());
        vo.setTaskId(rec.getTaskId());
        vo.setExamId(rec.getExamId());
        vo.setUserId(rec.getUserId());
        vo.setQuestionId(rec.getQuestionId());
        vo.setLanguage(rec.getLanguage());
        vo.setTriggerType(rec.getTriggerType());
        vo.setJudgeStatus(rec.getJudgeStatus());
        vo.setTotalCases(rec.getTotalCases());
        vo.setPassedCases(rec.getPassedCases());
        vo.setScore(rec.getScore());
        vo.setMaxScore(rec.getMaxScore());
        vo.setTimeUsedMs(rec.getTimeUsedMs());
        vo.setMemoryUsedKb(rec.getMemoryUsedKb());
        vo.setCompileError(rec.getCompileError());
        vo.setRuntimeError(rec.getRuntimeError());
        vo.setJudgeMessage(rec.getJudgeMessage());
        vo.setFinishedTime(rec.getFinishedTime());

        List<JudgeCaseResult> cases = caseResultMapper.selectList(
                new LambdaQueryWrapper<JudgeCaseResult>()
                        .eq(JudgeCaseResult::getJudgeRecordId, rec.getId())
                        .orderByAsc(JudgeCaseResult::getCaseIndex));
        vo.setCases(cases.stream()
                .map(cr -> toCaseVO(cr, fullDetail))
                .collect(Collectors.toList()));
        return vo;
    }

    private JudgeCaseResultVO toCaseVO(JudgeCaseResult cr, boolean fullDetail) {
        JudgeCaseResultVO vo = new JudgeCaseResultVO();
        vo.setCaseIndex(cr.getCaseIndex());
        vo.setCaseName(cr.getCaseName());
        vo.setStatus(cr.getStatus());
        vo.setPassed(cr.getPassed());
        vo.setIsPublic(cr.getIsPublic());
        vo.setTimeUsedMs(cr.getTimeUsedMs());
        vo.setMemoryUsedKb(cr.getMemoryUsedKb());
        vo.setScore(cr.getScore());

        // 错误消息对所有人可见
        vo.setErrorMessage(cr.getErrorMessage());

        boolean reveal = fullDetail || Boolean.TRUE.equals(cr.getIsPublic());
        if (reveal) {
            vo.setInputData(cr.getInputData());
            vo.setExpectedOutput(cr.getExpectedOutput());
            vo.setActualOutput(cr.getActualOutput());
        }
        return vo;
    }

}
