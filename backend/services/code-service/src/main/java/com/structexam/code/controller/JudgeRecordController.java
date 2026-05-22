package com.structexam.code.controller;

import com.structexam.code.distributed.dto.JudgeRecordVO;
import com.structexam.code.service.JudgeRecordService;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 判题记录查询接口。路由 /code/judge/** 已被 gateway /api/code/** -> code-service 覆盖，无需改网关。
 */
@RestController
@RequestMapping("/code/judge")
public class JudgeRecordController {

    @Autowired
    private JudgeRecordService judgeRecordService;

    /**
     * 学生端：查询某题最近一次判题结果（含逐条用例明细，非公开失败用例自动脱敏）。
     * 用于考试页面提交后直接轮询，刷新页面后也可恢复。
     */
    @GetMapping("/latest")
    public ApiResponse<JudgeRecordVO> latest(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long examId,
            @RequestParam Long questionId) {
        return ApiResponse.success(judgeRecordService.getLatestForStudent(examId, userId, questionId));
    }

    /**
     * 通用：按 taskId 查判题记录（学生只能查自己的，教师/管理员可看任意）。
     * 可替代或配合现有 GET /code/distributed/result/{taskId} 使用，
     * 区别是此接口从 DB 读取（持久化），不依赖 Redis TTL。
     */
    @GetMapping("/record/{taskId}")
    public ApiResponse<JudgeRecordVO> byTaskId(
            @PathVariable String taskId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ApiResponse.success(judgeRecordService.getByTaskId(taskId, userId, role));
    }

    /**
     * 教师端：查询某次代码提交的所有历史判题（含重判）。
     */
    @GetMapping("/teacher/submission/{submissionId}")
    public ApiResponse<List<JudgeRecordVO>> bySubmission(
            @PathVariable Long submissionId,
            @RequestHeader("X-User-Role") String role) {
        requireStaff(role);
        return ApiResponse.success(judgeRecordService.listBySubmission(submissionId));
    }

    // ---------------------------------------------------------------- helpers

    private void requireStaff(String role) {
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "只有教师或管理员可以访问此接口");
        }
    }
}
