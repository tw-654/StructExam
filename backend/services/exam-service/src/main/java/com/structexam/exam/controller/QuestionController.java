package com.structexam.exam.controller;

import com.structexam.common.dto.ApiResponse;
import com.structexam.common.entity.Question;
import com.structexam.common.exception.BusinessException;
import com.structexam.exam.dto.QuestionSaveRequest;
import com.structexam.exam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    private ExamService examService;

    @GetMapping("/list/{examId}")
    public ApiResponse<List<Question>> getQuestionsByExamId(@PathVariable Long examId) {
        List<Question> questions = examService.getQuestions(examId);
        return ApiResponse.success(questions);
    }

    @GetMapping("/{examId}")
    public ApiResponse<List<Question>> getQuestions(@PathVariable Long examId) {
        List<Question> questions = examService.getQuestions(examId);
        return ApiResponse.success(questions);
    }

    @GetMapping("/{examId}/{questionId}")
    public ApiResponse<Question> getQuestionDetail(
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        Question question = examService.getQuestionById(questionId);
        return ApiResponse.success(question);
    }

    @PostMapping("/teacher")
    public ApiResponse<Question> createQuestion(
            @RequestBody QuestionSaveRequest request,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("题目已保存", examService.createQuestion(request, editManagerId(teacherId, role)));
    }

    @PutMapping("/teacher/{questionId}")
    public ApiResponse<Question> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody QuestionSaveRequest request,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        return ApiResponse.success("题目已更新", examService.updateQuestion(questionId, request, editManagerId(teacherId, role)));
    }

    @DeleteMapping("/teacher/{questionId}")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable Long questionId,
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") String role) {
        requireTeacher(role);
        examService.deleteQuestion(questionId, editManagerId(teacherId, role));
        return ApiResponse.success("题目已删除", null);
    }

    private void requireTeacher(String role) {
        if (!"TEACHER".equals(role)) {
            throw new BusinessException(403, "Only teachers can access this API");
        }
    }

    private Long editManagerId(Long userId, String role) {
        return null;
    }
}
