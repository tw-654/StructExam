package com.structexam.code.judge;

import lombok.Getter;

/**
 * 单个测试用例的判定结果，由 {@link TestCaseJudgeService#judge} 返回。
 *
 * <p>{@link #normalizedActual} / {@link #normalizedExpected} 是经过
 * 去尾空白标准化后的字符串，便于直接入库或展示给用户。
 */
@Getter
public final class CaseJudgeResult {

    /** 判定状态（含语义名与 DB 缩写码）。 */
    private final CaseJudgeStatus status;

    /** 是否通过（等价于 status == ACCEPTED）。 */
    private final boolean passed;

    /** 标准化后的实际输出（去尾空白）。 */
    private final String normalizedActual;

    /** 标准化后的期望输出（去尾空白）。 */
    private final String normalizedExpected;

    /** 实际运行耗时（毫秒），透传自请求。 */
    private final Long timeUsedMs;

    /** 实际内存占用（KB），透传自请求。 */
    private final Long memoryUsedKb;

    /** 错误详情（CE 为编译错误，RE 为异常堆栈，其他为 null）。 */
    private final String errorMessage;

    private CaseJudgeResult(CaseJudgeStatus status,
                            String normalizedActual,
                            String normalizedExpected,
                            Long timeUsedMs,
                            Long memoryUsedKb,
                            String errorMessage) {
        this.status = status;
        this.passed = status.isPassed();
        this.normalizedActual = normalizedActual;
        this.normalizedExpected = normalizedExpected;
        this.timeUsedMs = timeUsedMs;
        this.memoryUsedKb = memoryUsedKb;
        this.errorMessage = errorMessage;
    }

    // ---- 工厂方法，供 TestCaseJudgeService 内部使用 ----

    static CaseJudgeResult accepted(String normActual, String normExpected,
                                    Long timeUsedMs, Long memoryUsedKb) {
        return new CaseJudgeResult(CaseJudgeStatus.ACCEPTED,
                normActual, normExpected, timeUsedMs, memoryUsedKb, null);
    }

    static CaseJudgeResult wrongAnswer(String normActual, String normExpected,
                                       Long timeUsedMs, Long memoryUsedKb) {
        return new CaseJudgeResult(CaseJudgeStatus.WRONG_ANSWER,
                normActual, normExpected, timeUsedMs, memoryUsedKb, null);
    }

    static CaseJudgeResult error(CaseJudgeStatus status,
                                 String normActual, String normExpected,
                                 Long timeUsedMs, Long memoryUsedKb,
                                 String errorMessage) {
        return new CaseJudgeResult(status,
                normActual, normExpected, timeUsedMs, memoryUsedKb, errorMessage);
    }
}
