package com.structexam.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.entity.Exam;
import com.structexam.common.entity.ExamRecord;
import com.structexam.exam.dto.ExamDetailDTO;
import com.structexam.exam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping("/list")
    public ApiResponse<Page<Exam>> getExamList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        Page<Exam> page = examService.getExamList(pageNum, pageSize, userId, role);
        return ApiResponse.success(page);
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamDetailDTO> getExamDetail(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        ExamDetailDTO detail = examService.getExamDetail(id, userId);
        return ApiResponse.success(detail);
    }

    @PostMapping("/enter/{id}")
    public ApiResponse<ExamRecord> enterExam(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        ExamRecord record = examService.enterExam(id, userId, ipAddress);
        return ApiResponse.success("Entered exam successfully", record);
    }

    @PostMapping("/submit/{id}")
    public ApiResponse<Void> submitExam(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        examService.submitExam(id, userId);
        return ApiResponse.success("Exam submitted successfully", null);
    }

    @GetMapping("/record/{id}")
    public ApiResponse<ExamRecord> getExamRecord(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        ExamRecord record = examService.getExamRecord(id, userId);
        return ApiResponse.success(record);
    }
}
