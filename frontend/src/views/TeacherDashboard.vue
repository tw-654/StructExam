<template>
  <div class="teacher-page">
    <div class="page-header">
      <div>
        <h2>教师管理</h2>
        <p>维护试卷、题目、发布考试，并实时查看在考人数、交卷情况和编程题判题反馈。</p>
      </div>
      <div class="page-actions">
        <el-button @click="loadExams">刷新列表</el-button>
        <el-button type="primary" @click="openExamDialog()">新建试卷</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="15">
        <el-card v-loading="loading">
          <el-table :data="exams" highlight-current-row @current-change="selectExam">
            <el-table-column prop="title" label="考试名称" min-width="180" />
            <el-table-column prop="duration" label="时长" width="80" align="center" />
            <el-table-column prop="totalScore" label="总分" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="210">
              <template #default="{ row }">
                <div>{{ formatDate(row.startTime) }}</div>
                <div class="muted">{{ formatDate(row.endTime) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click.stop="openExamDialog(row)">编辑</el-button>
                <el-button size="small" type="success" @click.stop="publishExam(row)">发布</el-button>
                <el-button size="small" type="danger" @click.stop="deleteExam(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无考试，点击右上角新建试卷" />
            </template>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="loadExams"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card>
          <template #header>
            <div class="section-title">
              <span>实时监控</span>
              <div class="header-actions">
                <span class="live-dot">自动刷新</span>
                <el-button size="small" :disabled="!selectedExam" @click="refreshSelected">刷新</el-button>
              </div>
            </div>
          </template>
          <el-empty v-if="!selectedExam" description="请选择一场考试" />
          <div v-else>
            <h3 class="selected-title">{{ selectedExam.title }}</h3>
            <div class="stat-grid">
              <div class="stat-item">
                <span>进入人数</span>
                <strong>{{ statistics.totalStudents || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>在考人数</span>
                <strong>{{ statistics.inProgressCount || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>已交卷</span>
                <strong>{{ statistics.submittedCount || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>已评分</span>
                <strong>{{ statistics.gradedCount || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>平均分</span>
                <strong>{{ Number(statistics.averageScore || 0).toFixed(1) }}</strong>
              </div>
              <div class="stat-item">
                <span>最高/最低</span>
                <strong>{{ statistics.maxScore || 0 }}/{{ statistics.minScore || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>中位数</span>
                <strong>{{ Number(statistics.medianScore || 0).toFixed(1) }}</strong>
              </div>
              <div class="stat-item">
                <span>标准差</span>
                <strong>{{ Number(statistics.stdDev || 0).toFixed(2) }}</strong>
              </div>
              <div class="stat-item highlight">
                <span>及格率</span>
                <strong :class="statistics.passRate >= 60 ? 'text-success' : 'text-danger'">
                  {{ Number(statistics.passRate || 0).toFixed(1) }}%
                </strong>
              </div>
              <div class="stat-item">
                <span>及格人数</span>
                <strong class="text-success">{{ statistics.passCount || 0 }}</strong>
              </div>
              <div class="stat-item">
                <span>不及格人数</span>
                <strong class="text-danger">{{ statistics.failCount || 0 }}</strong>
              </div>
            </div>

            <div class="chart-section" v-if="statistics.gradeDistribution">
              <div class="section-header">
                <span class="section-title-text">等级分布</span>
              </div>
              <div class="grade-distribution">
                <div v-for="(percent, grade) in statistics.gradeDistribution" :key="grade" class="grade-item">
                  <span class="grade-label">{{ grade }}</span>
                  <div class="grade-bar-bg">
                    <div class="grade-bar" :class="grade.toLowerCase()" :style="{ width: percent + '%' }"></div>
                  </div>
                  <span class="grade-percent">{{ percent.toFixed(1) }}%</span>
                </div>
              </div>
            </div>

            <div class="chart-section" v-if="statistics.scoreRanges">
              <div class="section-header">
                <span class="section-title-text">分数段分布</span>
              </div>
              <div class="score-ranges">
                <div v-for="range in statistics.scoreRanges" :key="range.range" class="score-range-item">
                  <span class="range-label">{{ range.range }}</span>
                  <div class="range-bar-bg">
                    <div class="range-bar" :style="{ width: range.percentage + '%' }"></div>
                  </div>
                  <span class="range-count">{{ range.count }}人</span>
                </div>
              </div>
            </div>
            <el-button type="warning" :disabled="!selectedExam" @click="gradeObjective">
              批改非主观题
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="detail-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="section-title">
              <span>题目管理</span>
              <el-button size="small" type="primary" :disabled="!selectedExam" @click="openQuestionDialog()">
                新建题目
              </el-button>
            </div>
          </template>
          <el-empty v-if="!selectedExam" description="请选择一场考试" />
          <el-table v-else :data="questions" size="small">
            <el-table-column prop="sortOrder" label="#" width="60" />
            <el-table-column prop="title" label="题目" min-width="150" />
            <el-table-column prop="type" label="类型" width="110">
              <template #default="{ row }">{{ questionTypeText(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="score" label="分值" width="70" align="center" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="openQuestionDialog(row)">编辑</el-button>
                <el-button
                  v-if="row.type === 'PROGRAMMING'"
                  size="small"
                  type="primary"
                  plain
                  @click="openTestCaseDialog(row)"
                >测试用例</el-button>
                <el-button size="small" type="danger" @click="deleteQuestion(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="section-title">
              <span>学生成绩与判题反馈</span>
              <span class="muted">{{ selectedExam ? selectedExam.title : '' }}</span>
            </div>
          </template>
          <el-empty v-if="!selectedExam" description="请选择一场考试" />
          <el-table v-else :data="studentScores" size="small" max-height="360">
            <el-table-column prop="userId" label="学生ID" width="90" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="recordStatusType(row.status)">{{ recordStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submittedQuestionCount" label="提交题数" width="90" align="center" />
            <el-table-column prop="judgedQuestionCount" label="已判" width="70" align="center" />
            <el-table-column prop="acceptedQuestionCount" label="AC" width="70" align="center" />
            <el-table-column prop="latestJudgeStatus" label="最新判题" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.latestJudgeStatus" :type="judgeStatusType(row.latestJudgeStatus)">
                  {{ row.latestJudgeStatus }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="latestJudgeTimeUsedMs" label="耗时" width="80" align="center">
              <template #default="{ row }">
                {{ row.latestJudgeTimeUsedMs == null ? '-' : `${row.latestJudgeTimeUsedMs}ms` }}
              </template>
            </el-table-column>
            <el-table-column prop="score" label="得分" width="80" align="center">
              <template #default="{ row }">{{ row.score == null ? '-' : row.score }}</template>
            </el-table-column>
            <el-table-column label="交卷时间" min-width="150">
              <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="examDialogVisible" :title="examForm.id ? '编辑试卷' : '新建试卷'" width="620px">
      <el-form label-width="90px">
        <el-form-item label="考试名称">
          <el-input v-model="examForm.title" />
        </el-form-item>
        <el-form-item label="考试说明">
          <el-input v-model="examForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="examTimeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="时长/总分">
          <div class="inline-fields">
            <el-input-number v-model="examForm.duration" :min="1" />
            <el-input-number v-model="examForm.totalScore" :min="1" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="examForm.status">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="examDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveExam">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="questionDialogVisible" :title="questionForm.id ? '编辑题目' : '新建题目'" width="680px">
      <el-form label-width="90px">
        <el-form-item label="题目类型">
          <el-select v-model="questionForm.type">
            <el-option label="编程题" value="PROGRAMMING" />
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目标题">
          <el-input v-model="questionForm.title" />
        </el-form-item>
        <el-form-item label="题目内容">
          <el-input v-model="questionForm.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="选项/答案">
          <el-input
            v-model="questionForm.options"
            type="textarea"
            :rows="3"
            placeholder='编程题可填测试用例 JSON，例如 {"testCases":[{"input":"1 2","expectedOutput":"3"}]}'
          />
        </el-form-item>
        <el-form-item label="分值/排序">
          <div class="inline-fields">
            <el-input-number v-model="questionForm.score" :min="1" />
            <el-input-number v-model="questionForm.sortOrder" :min="0" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试用例管理对话框 -->
    <el-dialog
      v-model="testCaseDialogVisible"
      :title="`测试用例管理 — ${testCaseQuestion.title || ''}`"
      width="860px"
      :close-on-click-modal="false"
    >
      <div class="tc-toolbar">
        <span class="muted">共 {{ testCases.length }} 条用例，通过/未通过由判题服务自动比对。</span>
        <el-button type="primary" size="small" @click="addTestCaseRow">+ 新增用例</el-button>
      </div>

      <el-table :data="testCases" v-loading="testCaseLoading" border size="small" class="tc-table">
        <el-table-column label="#" width="46" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>

        <el-table-column label="输入数据" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.inputData" type="textarea" :rows="2" placeholder="输入数据（可为空）" />
          </template>
        </el-table-column>

        <el-table-column label="期望输出" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.expectedOutput" type="textarea" :rows="2" placeholder="期望输出" />
          </template>
        </el-table-column>

        <el-table-column label="是否公开" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isPublic" />
          </template>
        </el-table-column>

        <el-table-column label="分值" width="90" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="0" :max="100" size="small" controls-position="right" />
          </template>
        </el-table-column>

        <el-table-column label="时限(ms)" width="100" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.timeLimitMs" :min="100" :max="10000" :step="100" size="small" controls-position="right" placeholder="默认" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button type="danger" size="small" plain @click="removeTestCaseRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="testCaseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="testCaseSaving" @click="saveTestCases">保存全部</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examApi, questionApi, testCaseApi } from '@/api/modules'

const route = useRoute()
const loading = ref(false)
const exams = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedExam = ref(null)
const questions = ref([])
const statistics = ref({})
const studentScores = ref([])
const examDialogVisible = ref(false)
const questionDialogVisible = ref(false)
const examTimeRange = ref([])
let refreshTimer = null

// ---------- 测试用例管理状态 ----------
const testCaseDialogVisible = ref(false)
const testCaseQuestion = ref({})
const testCases = ref([])
const testCaseLoading = ref(false)
const testCaseSaving = ref(false)
const examForm = reactive({
  id: null,
  title: '',
  description: '',
  duration: 120,
  totalScore: 100,
  startTime: '',
  endTime: '',
  status: 'DRAFT'
})

const questionForm = reactive({
  id: null,
  examId: null,
  type: 'PROGRAMMING',
  title: '',
  content: '',
  options: '',
  score: 10,
  sortOrder: 0
})

const loadExams = async () => {
  loading.value = true
  try {
    const res = await examApi.getTeacherList(pageNum.value, pageSize.value)
    exams.value = res.data.records || []
    total.value = res.data.total || 0
    const routeExamId = Number(route.query.examId)
    const targetExam = exams.value.find(exam => exam.id === routeExamId)
    if (targetExam) {
      await selectExam(targetExam)
    } else if (!selectedExam.value && exams.value.length > 0) {
      await selectExam(exams.value[0])
    }
  } catch (error) {
    console.error('Failed to load teacher exams:', error)
    ElMessage.error('加载教师端考试列表失败，请确认当前账号是教师或管理员')
  } finally {
    loading.value = false
  }
}

const selectExam = async (exam) => {
  if (!exam) return
  selectedExam.value = exam
  await refreshSelected()
}

const refreshSelected = async () => {
  if (!selectedExam.value) return
  try {
    const [detailRes, statRes, scoreRes] = await Promise.all([
      examApi.getDetail(selectedExam.value.id),
      examApi.getStatistics(selectedExam.value.id),
      examApi.getStudentScores(selectedExam.value.id)
    ])
    questions.value = detailRes.data.questions || []
    statistics.value = statRes.data || {}
    studentScores.value = scoreRes.data || []
  } catch (error) {
    console.error('Failed to refresh teacher monitor:', error)
    ElMessage.error('刷新考试监控失败')
  }
}

const openExamDialog = (exam = null) => {
  Object.assign(examForm, exam || {
    id: null,
    title: '',
    description: '',
    duration: 120,
    totalScore: 100,
    startTime: '',
    endTime: '',
    status: 'DRAFT'
  })
  examTimeRange.value = exam ? [exam.startTime, exam.endTime] : []
  examDialogVisible.value = true
}

const saveExam = async () => {
  if (!examTimeRange.value || examTimeRange.value.length !== 2) {
    ElMessage.warning('请选择考试时间范围')
    return
  }
  const payload = {
    title: examForm.title,
    description: examForm.description,
    duration: examForm.duration,
    totalScore: examForm.totalScore,
    startTime: examTimeRange.value[0],
    endTime: examTimeRange.value[1],
    status: examForm.status
  }
  const res = examForm.id
    ? await examApi.updateTeacherExam(examForm.id, payload)
    : await examApi.createTeacherExam(payload)
  ElMessage.success('试卷已保存')
  examDialogVisible.value = false
  await loadExams()
  selectedExam.value = res.data
  await refreshSelected()
}

const publishExam = async (exam) => {
  await examApi.publishTeacherExam(exam.id)
  ElMessage.success('考试已发布')
  await loadExams()
}

const deleteExam = async (exam) => {
  await ElMessageBox.confirm(`确定删除“${exam.title}”吗？`, '删除试卷', { type: 'warning' })
  await examApi.deleteTeacherExam(exam.id)
  if (selectedExam.value?.id === exam.id) {
    selectedExam.value = null
    questions.value = []
    statistics.value = {}
    studentScores.value = []
  }
  ElMessage.success('试卷已删除')
  await loadExams()
}

const openQuestionDialog = (question = null) => {
  Object.assign(questionForm, question || {
    id: null,
    examId: selectedExam.value?.id,
    type: 'PROGRAMMING',
    title: '',
    content: '',
    options: '',
    score: 10,
    sortOrder: questions.value.length + 1
  })
  questionForm.examId = selectedExam.value?.id
  questionDialogVisible.value = true
}

const saveQuestion = async () => {
  if (!selectedExam.value) return
  const payload = {
    examId: selectedExam.value.id,
    type: questionForm.type,
    title: questionForm.title,
    content: questionForm.content,
    options: questionForm.options,
    score: questionForm.score,
    sortOrder: questionForm.sortOrder
  }
  questionForm.id
    ? await questionApi.updateTeacherQuestion(questionForm.id, payload)
    : await questionApi.createTeacherQuestion(payload)
  ElMessage.success('题目已保存')
  questionDialogVisible.value = false
  await refreshSelected()
}

const deleteQuestion = async (question) => {
  await ElMessageBox.confirm(`确定删除题目“${question.title}”吗？`, '删除题目', { type: 'warning' })
  await questionApi.deleteTeacherQuestion(question.id)
  ElMessage.success('题目已删除')
  await refreshSelected()
}


// ---------- 测试用例管理方法 ----------

const openTestCaseDialog = async (question) => {
  testCaseQuestion.value = question
  testCaseDialogVisible.value = true
  testCases.value = []
  testCaseLoading.value = true
  try {
    const res = await testCaseApi.listForTeacher(question.id)
    testCases.value = (res.data || []).map(tc => ({
      id: tc.id,
      inputData: tc.inputData || '',
      expectedOutput: tc.expectedOutput || '',
      isPublic: !!tc.isPublic,
      score: tc.score ?? 0,
      timeLimitMs: tc.timeLimitMs ?? null,
      sortOrder: tc.sortOrder ?? 0
    }))
  } catch (error) {
    ElMessage.error('加载测试用例失败')
  } finally {
    testCaseLoading.value = false
  }
}

const addTestCaseRow = () => {
  testCases.value.push({
    id: null,
    inputData: '',
    expectedOutput: '',
    isPublic: true,
    score: 0,
    timeLimitMs: null,
    sortOrder: testCases.value.length + 1
  })
}

const removeTestCaseRow = (index) => {
  testCases.value.splice(index, 1)
}

const saveTestCases = async () => {
  if (!testCaseQuestion.value.id) return
  testCaseSaving.value = true
  try {
    const items = testCases.value.map((tc, idx) => ({
      questionId: testCaseQuestion.value.id,
      inputData: tc.inputData,
      expectedOutput: tc.expectedOutput,
      isPublic: tc.isPublic,
      score: tc.score || 0,
      timeLimitMs: tc.timeLimitMs || null,
      sortOrder: idx + 1
    }))
    await testCaseApi.batchSave(testCaseQuestion.value.id, items)
    ElMessage.success('测试用例已保存')
    testCaseDialogVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  } finally {
    testCaseSaving.value = false
  }
}


const gradeObjective = async () => {
  if (!selectedExam.value) return
  const res = await examApi.gradeObjective(selectedExam.value.id)
  ElMessage.success(`批改完成，处理 ${res.data} 份记录`)
  await refreshSelected()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const statusText = (status) => ({
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已结束'
}[status] || status)

const statusType = (status) => ({
  DRAFT: 'info',
  PUBLISHED: 'success',
  ONGOING: 'warning',
  FINISHED: 'danger'
}[status] || 'info')

const recordStatusText = (status) => ({
  NOT_STARTED: '未开始',
  IN_PROGRESS: '答题中',
  SUBMITTED: '已交卷',
  GRADED: '已评分'
}[status] || status)

const recordStatusType = (status) => ({
  IN_PROGRESS: 'warning',
  SUBMITTED: 'success',
  GRADED: 'primary'
}[status] || 'info')

const questionTypeText = (type) => ({
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  PROGRAMMING: '编程题'
}[type] || type)

const judgeStatusType = (status) => ({
  AC: 'success',
  WA: 'danger',
  TLE: 'warning',
  MLE: 'warning',
  RE: 'danger',
  CE: 'danger',
  FAILED: 'danger'
}[status] || 'info')

onMounted(() => {
  loadExams()
  refreshTimer = setInterval(() => {
    if (selectedExam.value && !examDialogVisible.value && !questionDialogVisible.value) {
      refreshSelected().catch(() => {})
    }
  }, 5000)
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.teacher-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.page-header h2,
.selected-title {
  margin: 0;
}

.page-header p,
.muted {
  color: #666;
  font-size: 13px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.live-dot {
  color: #67c23a;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-row {
  margin-top: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.stat-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-item span {
  color: #666;
  font-size: 13px;
}

.stat-item strong {
  font-size: 20px;
}

.inline-fields {
  display: flex;
  gap: 12px;
}


.stat-item.highlight {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
}

.stat-item.highlight span {
  color: rgba(255, 255, 255, 0.8);
}

.stat-item.highlight strong {
  color: #fff;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

.chart-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.section-header {
  margin-bottom: 12px;
}

.section-title-text {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.grade-distribution {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.grade-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.grade-label {
  width: 30px;
  font-weight: 600;
  font-size: 13px;
}

.grade-bar-bg {
  flex: 1;
  height: 16px;
  background: #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.grade-bar {
  height: 100%;
  border-radius: 8px;
  transition: width 0.5s ease;
}

.grade-bar.a {
  background: linear-gradient(90deg, #52c41a, #73d13d);
}

.grade-bar.b {
  background: linear-gradient(90deg, #1890ff, #40a9ff);
}

.grade-bar.c {
  background: linear-gradient(90deg, #faad14, #ffc53d);
}

.grade-bar.d {
  background: linear-gradient(90deg, #fa8c16, #ffa940);
}

.grade-bar.f {
  background: linear-gradient(90deg, #f5222d, #ff4d4f);
}

.grade-percent {
  width: 50px;
  text-align: right;
  font-size: 13px;
  color: #666;
}

.score-ranges {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-range-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-label {
  width: 60px;
  font-size: 12px;
  color: #666;
}

.range-bar-bg {
  flex: 1;
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.range-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 6px;
  transition: width 0.5s ease;
}

.range-count {
  width: 50px;
  text-align: right;
  font-size: 12px;
  color: #999;
}

.tc-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tc-table {
  width: 100%;

}
</style>
