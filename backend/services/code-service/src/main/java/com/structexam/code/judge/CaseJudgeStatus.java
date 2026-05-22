package com.structexam.code.judge;

/**
 * 单个测试用例的判定状态。
 * <p>
 * 优先级（由高到低）：CE > TLE > RE > MLE > WA / AC
 * <p>
 * {@link #dbCode()} 返回存入 t_judge_case_result.status 的缩写字符串，
 * 与 {@link com.structexam.code.distributed.dto.JudgeTaskStatus} 的命名保持一致。
 */
public enum CaseJudgeStatus {

    /** 输出完全匹配（去尾空白后） */
    ACCEPTED,

    /** 输出不一致 */
    WRONG_ANSWER,

    /** 运行时间超出限制 */
    TIME_LIMIT_EXCEEDED,

    /** 运行时异常 */
    RUNTIME_ERROR,

    /** 编译失败（整体任务 CE，逐条用例同步标记） */
    COMPILE_ERROR,

    /** 内存超限（沙箱上报或推算） */
    MEMORY_LIMIT_EXCEEDED,

    /** 系统错误或调度失败 */
    SYSTEM_ERROR;

    /** 数据库存储的缩写码，与全局状态枚举保持一致。 */
    public String dbCode() {
        return switch (this) {
            case ACCEPTED -> "AC";
            case WRONG_ANSWER -> "WA";
            case TIME_LIMIT_EXCEEDED -> "TLE";
            case RUNTIME_ERROR -> "RE";
            case COMPILE_ERROR -> "CE";
            case MEMORY_LIMIT_EXCEEDED -> "MLE";
            case SYSTEM_ERROR -> "FAILED";
        };
    }

    /** 是否视为通过（仅 ACCEPTED 通过）。 */
    public boolean isPassed() {
        return this == ACCEPTED;
    }
}
