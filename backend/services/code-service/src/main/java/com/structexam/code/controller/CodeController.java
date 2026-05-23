package com.structexam.code.controller;

import com.structexam.code.service.CodeSandboxService;
import com.structexam.code.service.CodeService;
import com.structexam.code.distributed.dto.JudgeTaskResponse;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.CodeSaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/code")
public class CodeController {

    @Autowired
    private CodeService codeService;

    @Autowired
    private CodeSandboxService codeSandboxService;

    @PostMapping("/save")
    public ApiResponse<Void> saveCode(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CodeSaveRequest request) {
        codeService.saveCode(userId, request);
        return ApiResponse.success("Code saved successfully", null);
    }

    @GetMapping("/{examId}/{questionId}")
    public ApiResponse<Map<String, String>> getCode(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        String code = codeService.getCode(examId, userId, questionId);
        return ApiResponse.success(Map.of("code", code != null ? code : ""));
    }

    @PostMapping("/submit")
    public ApiResponse<JudgeTaskResponse> submitCode(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CodeSaveRequest request) {
        JudgeTaskResponse response = codeService.submitCode(userId, request);
        return ApiResponse.success("提交成功，已进入分布式判题队列", response);
    }

    @PostMapping("/submitAll/{examId}")
    public ApiResponse<List<JudgeTaskResponse>> submitAllCode(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long examId) {
        List<JudgeTaskResponse> responses = codeService.submitAllCode(userId, examId);
        return ApiResponse.success("所有未判题代码已提交到分布式判题队列", responses);
    }

    @PostMapping("/run")
    public ApiResponse<CodeExecuteResponse> runCode(@RequestBody CodeExecuteRequest request) {
        CodeExecuteResponse response = codeSandboxService.executeCode(request);
        return ApiResponse.success(response.getMessage(), response);
    }
}
