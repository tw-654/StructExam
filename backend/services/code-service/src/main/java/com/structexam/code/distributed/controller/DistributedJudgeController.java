package com.structexam.code.distributed.controller;

import com.structexam.code.distributed.dto.DistributedJudgeSubmitRequest;
import com.structexam.code.distributed.dto.ExamSubmitRequest;
import com.structexam.code.distributed.dto.ExamSubmitResponse;
import com.structexam.code.distributed.dto.JudgeResult;
import com.structexam.code.distributed.dto.JudgeTaskResponse;
import com.structexam.code.distributed.dto.SandboxNodeView;
import com.structexam.code.distributed.service.DistributedJudgeService;
import com.structexam.code.distributed.service.JudgeTaskQueueService;
import com.structexam.code.distributed.service.SandboxNodeRegistry;
import com.structexam.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/code/distributed")
public class DistributedJudgeController {

    private final DistributedJudgeService distributedJudgeService;
    private final JudgeTaskQueueService queueService;
    private final SandboxNodeRegistry nodeRegistry;

    public DistributedJudgeController(DistributedJudgeService distributedJudgeService,
                                      JudgeTaskQueueService queueService,
                                      SandboxNodeRegistry nodeRegistry) {
        this.distributedJudgeService = distributedJudgeService;
        this.queueService = queueService;
        this.nodeRegistry = nodeRegistry;
    }

    @PostMapping("/submit")
    public ApiResponse<JudgeTaskResponse> submit(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody DistributedJudgeSubmitRequest request) {
        return ApiResponse.success("提交成功", distributedJudgeService.submit(userId, request));
    }

    @GetMapping("/result/{taskId}")
    public ApiResponse<JudgeResult> result(@PathVariable String taskId) {
        return queueService.getResult(taskId)
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(404, "判题结果未生成"));
    }

    @GetMapping("/queue")
    public ApiResponse<Map<String, Long>> queue() {
        return ApiResponse.success(Map.of("size", queueService.size()));
    }

    @GetMapping("/nodes")
    public ApiResponse<List<SandboxNodeView>> nodes() {
        return ApiResponse.success(nodeRegistry.nodeViews());
    }

    @PostMapping("/exam/{examId}/submit")
    public ApiResponse<ExamSubmitResponse> submitExam(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long examId,
            @RequestBody(required = false) ExamSubmitRequest request) {
        String triggerType = request != null ? request.getTriggerType() : "manual";
        return ApiResponse.success("交卷成功", distributedJudgeService.submitExam(userId, examId, triggerType));
    }
}
