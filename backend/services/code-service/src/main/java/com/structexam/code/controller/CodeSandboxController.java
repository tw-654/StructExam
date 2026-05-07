package com.structexam.code.controller;

import com.structexam.code.service.CodeSandboxService;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sandbox")
public class CodeSandboxController {

    @Autowired
    private CodeSandboxService codeSandboxService;

    @PostMapping("/execute")
    public ApiResponse<CodeExecuteResponse> executeCode(@RequestBody CodeExecuteRequest request) {
        CodeExecuteResponse response = codeSandboxService.executeCode(request);
        return ApiResponse.success("Execution completed", response);
    }

    @PostMapping("/run")
    public ApiResponse<CodeExecuteResponse> runCode(@RequestBody CodeExecuteRequest request) {
        CodeExecuteResponse response = codeSandboxService.executeCode(request);
        return ApiResponse.success("Code executed", response);
    }
}