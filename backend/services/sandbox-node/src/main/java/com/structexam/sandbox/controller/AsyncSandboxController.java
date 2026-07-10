package com.structexam.sandbox.controller;

import com.structexam.common.dto.ApiResponse;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.sandbox.service.AsyncSandboxService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sandbox/async")
public class AsyncSandboxController {

    private final AsyncSandboxService asyncSandboxService;

    public AsyncSandboxController(AsyncSandboxService asyncSandboxService) {
        this.asyncSandboxService = asyncSandboxService;
    }

    /**
     * 异步提交代码执行任务
     * 立即返回任务ID，不等待执行完成
     */
    @PostMapping("/submit")
    public ApiResponse<Map<String, String>> submitAsync(@RequestBody CodeExecuteRequest request) {
        try {
            String taskId = asyncSandboxService.submitAsync(request);
            
            Map<String, String> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("status", "PENDING");
            response.put("message", "Task submitted successfully");
            
            return ApiResponse.success("Task submitted", response);
            
        } catch (Exception e) {
            return ApiResponse.error("Failed to submit task: " + e.getMessage());
        }
    }

    /**
     * 查询任务执行结果
     */
    @GetMapping("/result/{taskId}")
    public ApiResponse<Object> getResult(@PathVariable String taskId) {
        try {
            AsyncSandboxService.TaskResult result = asyncSandboxService.getTaskResult(taskId);
            
            if (result.getStatus() == AsyncSandboxService.TaskStatus.FAILED && 
                "Task not found".equals(result.getErrorMessage())) {
                return ApiResponse.error("Task not found: " + taskId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("status", result.getStatus().name());
            
            if (result.getCompletedTime() != null) {
                response.put("completedTime", result.getCompletedTime());
            }
            
            if (result.getExecutionTime() != null) {
                response.put("executionTime", result.getExecutionTime());
            }
            
            if (result.getStatus() == AsyncSandboxService.TaskStatus.COMPLETED) {
                response.put("response", result.getResponse());
            } else if (result.getStatus() == AsyncSandboxService.TaskStatus.FAILED) {
                response.put("errorMessage", result.getErrorMessage());
            }
            
            return ApiResponse.success("Result retrieved", response);
            
        } catch (Exception e) {
            return ApiResponse.error("Failed to get result: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("mode", "async");
        return ApiResponse.success("Service is healthy", status);
    }
}