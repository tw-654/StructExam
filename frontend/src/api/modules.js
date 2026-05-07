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
    return api.put('/auth/password', null, {
      params: { oldPassword, newPassword }
    })
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
  getRecordList() {
    return api.get('/exam/record/list')
  }
}

export const questionApi = {
  getList(examId) {
    return api.get(`/question/${examId}`)
  },
  getDetail(examId, questionId) {
    return api.get(`/question/${examId}/${questionId}`)
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
  }
}
