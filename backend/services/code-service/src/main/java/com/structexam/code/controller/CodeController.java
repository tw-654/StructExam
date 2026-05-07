package com.structexam.code.controller;

import com.structexam.code.service.CodeSandboxService;
import com.structexam.code.service.CodeService;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.CodeSaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ApiResponse<Void> submitCode(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Long> request) {
        Long examId = request.get("examId");
        Long questionId = request.get("questionId");
        codeService.submitCode(userId, examId, questionId);
        return ApiResponse.success("Code submitted successfully", null);
    }

    @PostMapping("/submitAll/{examId}")
    public ApiResponse<Void> submitAllCode(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long examId) {
        codeService.submitAllCode(userId, examId);
        return ApiResponse.success("All code submitted successfully", null);
    }

    @PostMapping("/run")
    public ApiResponse<CodeExecuteResponse> runCode(@RequestBody CodeExecuteRequest request) {
        CodeExecuteResponse response = codeSandboxService.executeCode(request);
        return ApiResponse.success(response.getMessage(), response);
    }
}
