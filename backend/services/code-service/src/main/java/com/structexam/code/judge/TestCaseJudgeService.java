package com.structexam.code.judge;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 测试用例核心判定服务。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>接收沙箱输出与题目期望输出，执行标准化比较。</li>
 *   <li>按照优先级推断用例状态：CE &gt; TLE &gt; RE &gt; MLE &gt; WA / AC。</li>
 *   <li>不依赖数据库或外部 HTTP，可在单元测试中直接实例化。</li>
 * </ul>
 *
 * <h3>标准化规则</h3>
 * <ol>
 *   <li>按 {@code \n} 分行，去除每行末尾空白（{@code \r}、空格、Tab）。</li>
 *   <li>去除整体末尾的空行与空白。</li>
 *   <li>null 输出视为空字符串。</li>
 * </ol>
 */
@Component
public class TestCaseJudgeService {

    /**
     * 对单个测试用例执行判定。
     *
     * <p>调用方需确保 {@link CaseJudgeRequest} 中的沙箱标志（compileError / runtimeError）
     * 和题目配置（timeLimitMs / memoryLimitKb）已正确填充。
     *
     * @param req 判定请求
     * @return 判定结果（含状态、标准化输出、耗时等）
     */
    public CaseJudgeResult judge(CaseJudgeRequest req) {
        String normActual   = normalize(req.getActualOutput());
        String normExpected = normalize(req.getExpectedOutput());
        Long   timeUsedMs   = req.getTimeUsedMs();
        Long   memUsedKb    = req.getMemoryUsedKb();

        // 优先级 1：编译失败（整体任务 CE，所有用例统一标记）
        if (req.isCompileError()) {
            return CaseJudgeResult.error(
                    CaseJudgeStatus.COMPILE_ERROR,
                    normActual, normExpected,
                    timeUsedMs, memUsedKb,
                    req.getErrorMessage());
        }

        // 优先级 2：超时（时间超出限制 OR 沙箱错误信息含超时关键词）
        if (isTimeLimitExceeded(req)) {
            return CaseJudgeResult.error(
                    CaseJudgeStatus.TIME_LIMIT_EXCEEDED,
                    normActual, normExpected,
                    timeUsedMs, memUsedKb,
                    buildTleMessage(req));
        }

        // 优先级 3：运行时异常（RE）
        if (req.isRuntimeError()) {
            return CaseJudgeResult.error(
                    CaseJudgeStatus.RUNTIME_ERROR,
                    normActual, normExpected,
                    timeUsedMs, memUsedKb,
                    req.getErrorMessage());
        }

        // 优先级 4：内存超限（MLE）
        if (isMemoryLimitExceeded(req)) {
            return CaseJudgeResult.error(
                    CaseJudgeStatus.MEMORY_LIMIT_EXCEEDED,
                    normActual, normExpected,
                    timeUsedMs, memUsedKb,
                    "Memory limit exceeded");
        }

        // 优先级 5：输出比较（AC / WA）
        if (normActual.equals(normExpected)) {
            return CaseJudgeResult.accepted(normActual, normExpected, timeUsedMs, memUsedKb);
        }
        return CaseJudgeResult.wrongAnswer(normActual, normExpected, timeUsedMs, memUsedKb);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * 对输出字符串进行标准化：
     * <ol>
     *   <li>null → 空字符串</li>
     *   <li>按 {@code \n} 分行，每行调用 {@link String#stripTrailing()}</li>
     *   <li>重新拼合后整体调用 {@link String#stripTrailing()}（去除末尾空行）</li>
     * </ol>
     */
    public String normalize(String output) {
        if (output == null) {
            return "";
        }
        return Arrays.stream(output.split("\n", -1))
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .stripTrailing();
    }

    /**
     * 超时检测：
     * <ul>
     *   <li>若题目配置了时间限制且实际耗时超过该限制，判定为 TLE。</li>
     *   <li>若沙箱未上报逐条耗时，则解析沙箱错误信息中的超时关键词作为兜底。</li>
     * </ul>
     */
    private boolean isTimeLimitExceeded(CaseJudgeRequest req) {
        if (req.getTimeLimitMs() != null && req.getTimeUsedMs() != null
                && req.getTimeUsedMs() > req.getTimeLimitMs()) {
            return true;
        }
        // 沙箱错误信息中含超时关键词时（RE 场景下的兜底检测）
        if (req.isRuntimeError() && containsTimeoutKeyword(req.getErrorMessage())) {
            return true;
        }
        return false;
    }

    /** 内存超限检测：实际内存占用超过题目配置限制。 */
    private boolean isMemoryLimitExceeded(CaseJudgeRequest req) {
        return req.getMemoryLimitKb() != null && req.getMemoryUsedKb() != null
                && req.getMemoryUsedKb() > req.getMemoryLimitKb();
    }

    private boolean containsTimeoutKeyword(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("time limit")
                || lower.contains("timed out")
                || lower.contains("超时");
    }

    private String buildTleMessage(CaseJudgeRequest req) {
        if (req.getTimeLimitMs() != null && req.getTimeUsedMs() != null
                && req.getTimeUsedMs() > req.getTimeLimitMs()) {
            return String.format("Time limit exceeded: %dms (limit: %dms)",
                    req.getTimeUsedMs(), req.getTimeLimitMs());
        }
        return req.getErrorMessage() != null ? req.getErrorMessage() : "Time limit exceeded";
    }
}
