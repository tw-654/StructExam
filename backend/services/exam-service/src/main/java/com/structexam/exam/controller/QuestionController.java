package com.structexam.exam.controller;

import com.structexam.common.dto.ApiResponse;
import com.structexam.common.entity.Question;
import com.structexam.exam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    private ExamService examService;

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
}
