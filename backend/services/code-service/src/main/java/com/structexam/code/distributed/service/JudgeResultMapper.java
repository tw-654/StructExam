package com.structexam.code.distributed.service;

import com.structexam.code.distributed.dto.JudgeResult;
import com.structexam.code.distributed.dto.JudgeTask;
import com.structexam.code.distributed.dto.JudgeTaskStatus;
import com.structexam.common.dto.CodeExecuteResponse;

import java.time.LocalDateTime;
import java.util.List;

public final class JudgeResultMapper {

    private JudgeResultMapper() {
    }

    public static JudgeResult fromExecution(JudgeTask task, CodeExecuteResponse response) {
        JudgeResult result = baseResult(task);
        result.setStatus(resolveStatus(response));
        result.setTimeUsedMs(response.getExecutionTime());
        result.setError(resolveError(response));
        result.setTestCaseResults(response.getTestResults());
        result.setOutput(resolveOutput(response.getTestResults()));
        return result;
    }

    public static JudgeResult failed(JudgeTask task, String message) {
        JudgeResult result = baseResult(task);
        result.setStatus(JudgeTaskStatus.FAILED);
        result.setError(message);
        return result;
    }

    private static JudgeResult baseResult(JudgeTask task) {
        JudgeResult result = new JudgeResult();
        result.setTaskId(task.getTaskId());
        result.setUserId(task.getUserId());
        result.setExamId(task.getExamId());
        result.setQuestionId(task.getQuestionId());
        result.setFinishedTime(LocalDateTime.now());
        return result;
    }

    private static JudgeTaskStatus resolveStatus(CodeExecuteResponse response) {
        if (response.getCompileError() != null && !response.getCompileError().isBlank()) {
            return JudgeTaskStatus.CE;
        }
        if (response.getRuntimeError() != null && !response.getRuntimeError().isBlank()) {
            return JudgeTaskStatus.RE;
        }
        return response.isSuccess() ? JudgeTaskStatus.AC : JudgeTaskStatus.WA;
    }

    private static String resolveError(CodeExecuteResponse response) {
        if (response.getCompileError() != null && !response.getCompileError().isBlank()) {
            return response.getCompileError();
        }
        if (response.getRuntimeError() != null && !response.getRuntimeError().isBlank()) {
            return response.getRuntimeError();
        }
        return response.isSuccess() ? null : response.getMessage();
    }

    private static String resolveOutput(List<CodeExecuteResponse.TestResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return null;
        }
        return testResults.get(testResults.size() - 1).getActualOutput();
    }
}
