import api from './index'

export const authApi = {
  login(data) {
    return api.post('/auth/login', data)
  },
  register(data) {
    return api.post('/auth/register', data)
  },
  logout() {
    return api.post('/auth/logout')
  },
  getUserInfo() {
    return api.get('/auth/userinfo')
  },
  updatePassword(oldPassword, newPassword) {
    return api.put('/auth/password', null, { params: { oldPassword, newPassword } })
  }
}

export const examApi = {
  getList(pageNum = 1, pageSize = 10) {
    return api.get('/exam/list', { params: { pageNum, pageSize } })
  },
  getDetail(id) {
    return api.get(`/exam/${id}`)
  },
  enterExam(id) {
    return api.post(`/exam/enter/${id}`)
  },
  submitExam(id) {
    return api.post(`/exam/submit/${id}`)
  },
  getRecord(id) {
    return api.get(`/exam/record/${id}`)
  },
  getRuntime(id) {
    return api.get(`/exam/runtime/${id}`)
  },
  getRecordList() {
    return api.get('/exam/record/list')
  },
  getTeacherList(pageNum = 1, pageSize = 10) {
    return api.get('/exam/teacher/list', { params: { pageNum, pageSize } })
  },
  createTeacherExam(data) {
    return api.post('/exam/teacher', data)
  },
  updateTeacherExam(id, data) {
    return api.put(`/exam/teacher/${id}`, data)
  },
  deleteTeacherExam(id) {
    return api.delete(`/exam/teacher/${id}`)
  },
  publishTeacherExam(id) {
    return api.post(`/exam/teacher/${id}/publish`)
  },
  gradeObjective(id) {
    return api.post(`/exam/teacher/${id}/grade-objective`)
  },
  getStatistics(id) {
    return api.get(`/exam/teacher/${id}/statistics`)
  },
  /** 成绩分布：按得分率区间统计人数，供教师端柱状图使用 */
  getScoreDistribution(examId) {
    return api.get(`/exam/teacher/${examId}/score-distribution`)
  },
  getStudentScores(id) {
    return api.get(`/exam/teacher/${id}/records`)
  }
}

export const questionApi = {
  getList(examId) {
    return api.get(`/question/${examId}`)
  },
  getDetail(examId, questionId) {
    return api.get(`/question/${examId}/${questionId}`)
  },
  createTeacherQuestion(data) {
    return api.post('/question/teacher', data)
  },
  updateTeacherQuestion(id, data) {
    return api.put(`/question/teacher/${id}`, data)
  },
  deleteTeacherQuestion(id) {
    return api.delete(`/question/teacher/${id}`)
  }
}

export const codeApi = {
  save(data) {
    return api.post('/code/save', data)
  },
  get(examId, questionId) {
    return api.get(`/code/${examId}/${questionId}`)
  },
  submit(data) {
    return api.post('/code/submit', data)
  },
  submitAll(examId) {
    return api.post(`/code/submitAll/${examId}`)
  },
  run(data) {
    return api.post('/code/run', data)
  },
  submitDistributed(data) {
    return api.post('/code/distributed/submit', data)
  },
  getDistributedResult(taskId) {
    return api.get(`/code/distributed/result/${taskId}`)
  }
}

export const distributedAdminApi = {
  snapshot() {
    return api.get('/code/distributed/admin/snapshot')
  },
  submitTestTask(data) {
    return api.post('/code/distributed/admin/test-task', data)
  },
  startLoadTest(data) {
    return api.post('/code/distributed/admin/load-test', data)
  }
}

export const sandboxApi = {
  execute(data) {
    return api.post('/sandbox/execute', data)
  },
  run(data) {
    return api.post('/sandbox/run', data)
  }
}

/**
 * 编程题测试用例（exam-service QuestionTestCaseController）
 * 教师端表格「保存全部」走 batchSave（整题替换）；单条增删改接口备用。
 */
export const testCaseApi = {
  /** 学生端：仅 isPublic=true 的样例 */
  listPublic(questionId) {
    return api.get(`/question/test-case/public/${questionId}`)
  },
  /** 教师端：某题全部用例（含隐藏） */
  listForTeacher(questionId) {
    return api.get(`/question/test-case/teacher/${questionId}`)
  },
  create(data) {
    return api.post('/question/test-case/teacher', data)
  },
  update(id, data) {
    return api.put(`/question/test-case/teacher/${id}`, data)
  },
  remove(id) {
    return api.delete(`/question/test-case/teacher/${id}`)
  },
  /** 与对话框「保存全部」对应：后端 replaceAll 先删后插 */
  batchSave(questionId, items) {
    return api.put(`/question/test-case/teacher/batch/${questionId}`, items)
  }
}

export const judgeRecordApi = {
  getLatest(examId, questionId) {
    return api.get('/code/judge/latest', { params: { examId, questionId } })
  },
  getByTaskId(taskId) {
    return api.get(`/code/judge/record/${taskId}`)
  },
  getBySubmission(submissionId) {
    return api.get(`/code/judge/teacher/submission/${submissionId}`)
  }
}
