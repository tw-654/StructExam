package com.structexam.sandbox.controller;

import com.structexam.common.dto.ApiResponse;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.sandbox.service.SandboxRunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sandbox")
public class SandboxRunController {

    private final SandboxRunService sandboxRunService;

    public SandboxRunController(SandboxRunService sandboxRunService) {
        this.sandboxRunService = sandboxRunService;
    }

    @PostMapping("/run")
    public ApiResponse<CodeExecuteResponse> run(@RequestBody CodeExecuteRequest request) {
        CodeExecuteResponse response = sandboxRunService.execute(request);
        return ApiResponse.success(response.getMessage(), response);
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
