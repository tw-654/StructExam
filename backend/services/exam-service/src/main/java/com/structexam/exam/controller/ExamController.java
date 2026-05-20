package com.structexam.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.structexam.common.dto.ApiResponse;
import com.structexam.common.entity.Exam;
import com.structexam.common.entity.ExamRecord;
import com.structexam.exam.dto.ExamDetailDTO;
import com.structexam.exam.dto.ExamRuntimeDTO;
import com.structexam.exam.dto.ExamSaveRequest;
import com.structexam.exam.dto.ExamStatisticsDTO;
import com.structexam.exam.dto.StudentScoreDTO;
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

    @GetMapping("/teacher/list")
    public ApiResponse<Page<Exam>> getTeacherExamList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireStaff(role);
        return ApiResponse.success(examService.getTeacherExamList(pageNum, pageSize, viewManagerId(role)));
    }

    @PostMapping("/teacher")
    public ApiResponse<Exam> createExam(
            @RequestBody ExamSaveRequest request,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("考试已保存", examService.createExam(request, teacherId));
    }

    @PutMapping("/teacher/{id}")
    public ApiResponse<Exam> updateExam(
            @PathVariable Long id,
            @RequestBody ExamSaveRequest request,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("考试已更新", examService.updateExam(id, request, editManagerId(teacherId, role)));
    }

    @DeleteMapping("/teacher/{id}")
    public ApiResponse<Void> deleteExam(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        examService.deleteExam(id, editManagerId(teacherId, role));
        return ApiResponse.success("考试已删除", null);
    }

    @PostMapping("/teacher/{id}/publish")
    public ApiResponse<Exam> publishExam(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("考试已发布", examService.publishExam(id, editManagerId(teacherId, role)));
    }

    @PostMapping("/teacher/{id}/grade-objective")
    public ApiResponse<Integer> gradeObjective(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("批改完成", examService.gradeObjective(id, editManagerId(teacherId, role)));
    }

    @GetMapping("/teacher/{id}/statistics")
    public ApiResponse<ExamStatisticsDTO> getStatistics(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireStaff(role);
        return ApiResponse.success(examService.getExamStatistics(id, viewManagerId(role)));
    }

    @GetMapping("/teacher/{id}/records")
    public ApiResponse<List<StudentScoreDTO>> getStudentScores(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireStaff(role);
        return ApiResponse.success(examService.getStudentScores(id, viewManagerId(role)));
    }

    @GetMapping("/record/list")
    public ApiResponse<List<ExamRecord>> getExamRecords(
            @RequestHeader("X-User-Id") Long userId) {
        List<ExamRecord> records = examService.getExamRecordsByUserId(userId);
        return ApiResponse.success(records);
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

    @GetMapping("/runtime/{id}")
    public ApiResponse<ExamRuntimeDTO> getExamRuntime(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(examService.getExamRuntime(id, userId));
    }

    private void requireTeacher(String role) {
        if (!"TEACHER".equals(role)) {
            throw new com.structexam.common.exception.BusinessException(403, "Only teachers can access this API");
        }
    }

    private void requireStaff(String role) {
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new com.structexam.common.exception.BusinessException(403, "Only teachers or admins can access this API");
        }
    }

    private Long viewManagerId(String role) {
        return null;
    }

    private Long editManagerId(Long userId, String role) {
        return null;
    }
}
