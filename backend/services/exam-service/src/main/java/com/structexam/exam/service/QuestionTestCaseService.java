package com.structexam.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.structexam.common.entity.Question;
import com.structexam.common.entity.QuestionTestCase;
import com.structexam.common.exception.BusinessException;
import com.structexam.exam.dto.QuestionTestCaseSaveRequest;
import com.structexam.exam.mapper.QuestionMapper;
import com.structexam.exam.mapper.QuestionTestCaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionTestCaseService {

    @Autowired
    private QuestionTestCaseMapper testCaseMapper;

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 查询题目的测试用例列表。
     *
     * @param questionId 题目 ID
     * @param publicOnly true=只返回对学生公开的用例（学生端调用），false=返回全部（教师端）
     */
    public List<QuestionTestCase> listByQuestion(Long questionId, boolean publicOnly) {
        LambdaQueryWrapper<QuestionTestCase> wrapper = new LambdaQueryWrapper<QuestionTestCase>()
                .eq(QuestionTestCase::getQuestionId, questionId)
                .eq(QuestionTestCase::getStatus, 1)
                .orderByAsc(QuestionTestCase::getSortOrder);
        if (publicOnly) {
            wrapper.eq(QuestionTestCase::getIsPublic, true);
        }
        return testCaseMapper.selectList(wrapper);
    }

    /**
     * 新增单条测试用例（教师）。
     */
    public QuestionTestCase create(Long teacherId, QuestionTestCaseSaveRequest req) {
        validateRequest(req);
        verifyQuestionExists(req.getQuestionId());
        QuestionTestCase tc = toEntity(req);
        tc.setStatus(1);
        testCaseMapper.insert(tc);
        return tc;
    }

    /**
     * 修改单条测试用例（教师）。
     */
    public QuestionTestCase update(Long teacherId, Long id, QuestionTestCaseSaveRequest req) {
        QuestionTestCase existing = testCaseMapper.selectById(id);
        if (existing == null || existing.getStatus() == 0) {
            throw new BusinessException(404, "测试用例不存在");
        }
        applyChanges(existing, req);
        testCaseMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除单条测试用例（逻辑删除，status=0）。
     */
    public void delete(Long teacherId, Long id) {
        QuestionTestCase existing = testCaseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "测试用例不存在");
        }
        existing.setStatus(0);
        testCaseMapper.updateById(existing);
    }

    /**
     * 批量替换题目的全部测试用例（教师编辑器"保存"时调用）。
     * 先物理删除旧用例，再按 sortOrder 顺序插入新用例。
     */
    @Transactional
    public List<QuestionTestCase> replaceAll(Long teacherId, Long questionId,
                                             List<QuestionTestCaseSaveRequest> items) {
        verifyQuestionExists(questionId);
        testCaseMapper.delete(new LambdaQueryWrapper<QuestionTestCase>()
                .eq(QuestionTestCase::getQuestionId, questionId));

        List<QuestionTestCase> saved = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            QuestionTestCaseSaveRequest req = items.get(i);
            req.setQuestionId(questionId);
            if (req.getSortOrder() == null) {
                req.setSortOrder(i);
            }
            validateRequest(req);
            QuestionTestCase tc = toEntity(req);
            tc.setStatus(1);
            testCaseMapper.insert(tc);
            saved.add(tc);
        }
        return saved;
    }

    // ---------------------------------------------------------------- helpers

    private void validateRequest(QuestionTestCaseSaveRequest req) {
        if (req.getQuestionId() == null) {
            throw new BusinessException(400, "questionId 不能为空");
        }
        if (!StringUtils.hasText(req.getInputData())) {
            throw new BusinessException(400, "输入数据不能为空");
        }
    }

    private void verifyQuestionExists(Long questionId) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BusinessException(404, "题目不存在");
        }
        if (!"PROGRAMMING".equals(q.getType())) {
            throw new BusinessException(400, "只有编程题才能配置测试用例");
        }
    }

    private QuestionTestCase toEntity(QuestionTestCaseSaveRequest req) {
        QuestionTestCase tc = new QuestionTestCase();
        tc.setQuestionId(req.getQuestionId());
        tc.setCaseName(req.getCaseName());
        tc.setInputData(req.getInputData());
        tc.setExpectedOutput(req.getExpectedOutput());
        tc.setIsSample(req.getIsSample() != null ? req.getIsSample() : false);
        tc.setIsPublic(req.getIsPublic() != null ? req.getIsPublic() : false);
        tc.setWeight(req.getWeight() != null ? req.getWeight() : 1);
        tc.setTimeLimitMs(req.getTimeLimitMs());
        tc.setMemoryLimitKb(req.getMemoryLimitKb());
        tc.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        return tc;
    }

    private void applyChanges(QuestionTestCase tc, QuestionTestCaseSaveRequest req) {
        if (req.getCaseName() != null)       tc.setCaseName(req.getCaseName());
        if (req.getInputData() != null)      tc.setInputData(req.getInputData());
        if (req.getExpectedOutput() != null) tc.setExpectedOutput(req.getExpectedOutput());
        if (req.getIsSample() != null)       tc.setIsSample(req.getIsSample());
        if (req.getIsPublic() != null)       tc.setIsPublic(req.getIsPublic());
        if (req.getWeight() != null)         tc.setWeight(req.getWeight());
        if (req.getTimeLimitMs() != null)    tc.setTimeLimitMs(req.getTimeLimitMs());
        if (req.getMemoryLimitKb() != null)  tc.setMemoryLimitKb(req.getMemoryLimitKb());
        if (req.getSortOrder() != null)      tc.setSortOrder(req.getSortOrder());
    }
}
