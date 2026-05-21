package com.structexam.code.judge;

import lombok.Builder;
import lombok.Getter;

/**
 * 单个测试用例的判定请求。
 *
 * <p>由 {@link TestCaseJudgeService#judge} 消费，调用方负责从沙箱响应和题目配置中填充字段。
 */
@Getter
@Builder
public class CaseJudgeRequest {

    // ---- 沙箱返回的原始数据 ----

    /** 学生代码的实际输出（可为 null，表示无输出或执行未完成）。 */
    private final String actualOutput;

    /** 该用例配置的期望输出。 */
    private final String expectedOutput;

    /** 本用例实际消耗时间（毫秒）；沙箱未上报时为 null。 */
    private final Long timeUsedMs;

    /** 本用例实际内存占用（KB）；沙箱未上报时为 null。 */
    private final Long memoryUsedKb;

    /**
     * 沙箱上报的错误信息（运行时异常/超时日志等）。
     * CE 时为编译错误信息，RE/TLE 时为异常堆栈或超时提示。
     */
    private final String errorMessage;

    // ---- 整体任务级别的标志（由 JudgeResult 全局状态传入）----

    /** 整体任务是否编译失败；为 true 时所有用例直接标记 CE。 */
    private final boolean compileError;

    /**
     * 本用例沙箱是否报告运行时错误。
     * <p>若沙箱不区分逐条错误类型，可传入整体 RE 标志；
     * 超时检测优先于此字段（TLE > RE）。
     */
    private final boolean runtimeError;

    // ---- 题目配置（用于自主推算 TLE / MLE）----

    /** 题目或用例级别的时间限制（毫秒）；null 表示不做超时推算。 */
    private final Long timeLimitMs;

    /** 题目或用例级别的内存限制（KB）；null 表示不做内存超限推算。 */
    private final Long memoryLimitKb;
}
