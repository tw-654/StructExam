package com.structexam.exam.controller;

import com.structexam.common.dto.ApiResponse;
import com.structexam.common.entity.QuestionTestCase;
import com.structexam.common.exception.BusinessException;
import com.structexam.exam.dto.QuestionTestCaseSaveRequest;
import com.structexam.exam.service.QuestionTestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试用例 CRUD。
 * 路由 /question/test-case/** 已被 gateway /api/question/** -> exam-service 覆盖，无需改网关。
 */
@RestController
@RequestMapping("/question/test-case")
public class QuestionTestCaseController {

    @Autowired
    private QuestionTestCaseService service;

    /**
     * 学生端：获取某题的公开样例（is_public=true）。
     * 用于题目描述旁展示样例输入/输出。
     */
    @GetMapping("/public/{questionId}")
    public ApiResponse<List<QuestionTestCase>> listPublic(@PathVariable Long questionId) {
        return ApiResponse.success(service.listByQuestion(questionId, true));
    }

    /**
     * 教师端：获取某题的全部测试用例（含非公开）。
     */
    @GetMapping("/teacher/{questionId}")
    public ApiResponse<List<QuestionTestCase>> listForTeacher(
            @PathVariable Long questionId,
            @RequestHeader("X-User-Role") String role) {
        requireStaff(role);
        return ApiResponse.success(service.listByQuestion(questionId, false));
    }

    /**
     * 教师端：新增单条测试用例。
     */
    @PostMapping("/teacher")
    public ApiResponse<QuestionTestCase> create(
            @RequestBody QuestionTestCaseSaveRequest req,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("测试用例已创建", service.create(teacherId, req));
    }

    /**
     * 教师端：修改单条测试用例。
     */
    @PutMapping("/teacher/{id}")
    public ApiResponse<QuestionTestCase> update(
            @PathVariable Long id,
            @RequestBody QuestionTestCaseSaveRequest req,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("测试用例已更新", service.update(teacherId, id, req));
    }

    /**
     * 教师端：删除单条测试用例（逻辑删除）。
     */
    @DeleteMapping("/teacher/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        service.delete(teacherId, id);
        return ApiResponse.success("测试用例已删除", null);
    }

    /**
     * 教师端：批量替换题目所有测试用例（编辑器一次性保存整组时使用）。
     */
    @PutMapping("/teacher/batch/{questionId}")
    public ApiResponse<List<QuestionTestCase>> replaceAll(
            @PathVariable Long questionId,
            @RequestBody List<QuestionTestCaseSaveRequest> items,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("测试用例已保存", service.replaceAll(teacherId, questionId, items));
    }

    // ---------------------------------------------------------------- helpers

    private void requireTeacher(String role) {
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "只有教师或管理员可以操作测试用例");
        }
    }

    private void requireStaff(String role) {
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "只有教师或管理员可以查看全部测试用例");
        }
    }
}
