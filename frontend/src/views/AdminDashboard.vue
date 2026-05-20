<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <h2>管理员监控</h2>
        <p>查看考试运行情况、学生提交反馈和分布式判题集群状态。</p>
      </div>
      <div class="page-actions">
        <el-button @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card v-loading="examLoading">
          <template #header>
            <div class="section-title">
              <span>考试情况</span>
              <span class="muted">只读查看</span>
            </div>
          </template>
          <el-table :data="exams" highlight-current-row @current-change="selectExam">
            <el-table-column prop="title" label="考试名称" min-width="180" />
            <el-table-column prop="duration" label="时长" width="80" align="center" />
            <el-table-column prop="totalScore" label="总分" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="examStatusType(row.status)">{{ examStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="210">
              <template #default="{ row }">
                <div>{{ formatDate(row.startTime) }}</div>
                <div class="muted">{{ formatDate(row.endTime) }}</div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无考试" />
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

      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="section-title">
              <span>实时统计</span>
              <span class="live-dot">自动刷新</span>
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
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card">
      <template #header>
        <div class="section-title">
          <span>学生提交与判题反馈</span>
          <span class="muted">{{ selectedExam ? selectedExam.title : '' }}</span>
        </div>
      </template>
      <el-empty v-if="!selectedExam" description="请选择一场考试" />
      <el-table v-else :data="studentScores" size="small" max-height="320">
        <el-table-column prop="userId" label="学生ID" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="recordStatusType(row.status)">{{ recordStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedQuestionCount" label="提交题数" width="90" align="center" />
        <el-table-column prop="judgedQuestionCount" label="已判" width="80" align="center" />
        <el-table-column prop="acceptedQuestionCount" label="AC" width="80" align="center" />
        <el-table-column prop="latestJudgeStatus" label="最新判题" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.latestJudgeStatus" :type="judgeStatusType(row.latestJudgeStatus)">
              {{ row.latestJudgeStatus }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="latestJudgeTimeUsedMs" label="耗时" width="90" align="center">
          <template #default="{ row }">
            {{ row.latestJudgeTimeUsedMs == null ? '-' : `${row.latestJudgeTimeUsedMs}ms` }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80" align="center">
          <template #default="{ row }">{{ row.score == null ? '-' : row.score }}</template>
        </el-table-column>
        <el-table-column label="进入时间" min-width="150">
          <template #default="{ row }">{{ formatDate(row.enterTime) }}</template>
        </el-table-column>
        <el-table-column label="交卷时间" min-width="150">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card" v-loading="distributedLoading">
      <template #header>
        <div class="section-title">
          <span>分布式判题后台监控</span>
          <span class="muted">队列、节点、任务、交互会话</span>
        </div>
      </template>

      <div class="distributed-stats">
        <div class="stat-item">
          <span>等待队列</span>
          <strong>{{ distributed.queueSize || 0 }}</strong>
        </div>
        <div class="stat-item">
          <span>处理中</span>
          <strong>{{ distributed.processingQueueSize || 0 }}</strong>
        </div>
        <div class="stat-item">
          <span>沙箱节点</span>
          <strong>{{ nodeCount }}</strong>
        </div>
        <div class="stat-item">
          <span>健康节点</span>
          <strong>{{ healthyNodeCount }}</strong>
        </div>
        <div class="stat-item">
          <span>运行任务</span>
          <strong>{{ runningTaskCount }}</strong>
        </div>
        <div class="stat-item">
          <span>交互会话</span>
          <strong>{{ sessionCount }}</strong>
        </div>
      </div>

      <el-tabs v-model="distributedTab">
        <el-tab-pane label="沙箱节点" name="nodes">
          <el-table :data="distributed.nodes || []" size="small" max-height="300">
            <el-table-column prop="serviceId" label="服务" min-width="130" />
            <el-table-column label="地址" min-width="180">
              <template #default="{ row }">{{ row.uri || `${row.host}:${row.port}` }}</template>
            </el-table-column>
            <el-table-column label="健康" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.healthy ? 'success' : 'danger'">{{ row.healthy ? '健康' : '异常' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="runningTasks" label="运行任务" width="90" align="center" />
            <el-table-column label="容量" width="180">
              <template #default="{ row }">
                <el-progress
                  :percentage="nodeLoadPercent(row)"
                  :status="nodeLoadPercent(row) >= 100 ? 'exception' : undefined"
                />
              </template>
            </el-table-column>
            <el-table-column label="元数据" min-width="180">
              <template #default="{ row }">{{ JSON.stringify(row.metadata || {}) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="最近任务" name="tasks">
          <el-table :data="distributed.recentTasks || []" size="small" max-height="300">
            <el-table-column prop="taskId" label="任务ID" min-width="220" show-overflow-tooltip />
            <el-table-column prop="userId" label="用户" width="80" />
            <el-table-column label="考试/题目" width="120">
              <template #default="{ row }">{{ row.examId }}/{{ row.questionId }}</template>
            </el-table-column>
            <el-table-column prop="language" label="语言" width="90" />
            <el-table-column prop="sandboxNodeUri" label="执行节点" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="judgeStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="retryCount" label="重试" width="70" align="center" />
            <el-table-column prop="error" label="错误" min-width="160" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="交互会话" name="sessions">
          <el-table :data="distributed.interactiveSessions || []" size="small" max-height="300">
            <el-table-column prop="gatewaySessionId" label="网关会话" min-width="180" show-overflow-tooltip />
            <el-table-column prop="sandboxUri" label="沙箱地址" min-width="180" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="测试工具" name="tools">
          <div class="tool-grid">
            <div class="tool-panel">
              <h4>投递测试任务</h4>
              <div class="inline-fields">
                <el-input-number v-model="testTask.userId" :min="1" />
                <el-input-number v-model="testTask.examId" :min="1" />
                <el-input-number v-model="testTask.questionId" :min="1" />
              </div>
              <div class="inline-fields">
                <el-select v-model="testTask.language">
                  <el-option label="Python" value="python" />
                  <el-option label="Java" value="java" />
                  <el-option label="C++" value="cpp" />
                </el-select>
                <el-input v-model="testInput" placeholder="测试输入" />
                <el-input v-model="expectedOutput" placeholder="期望输出" />
              </div>
              <el-input v-model="testTask.code" type="textarea" :rows="5" />
              <el-button type="primary" @click="submitTestTask">投递测试任务</el-button>
            </div>

            <div class="tool-panel">
              <h4>并发模拟</h4>
              <div class="inline-fields">
                <el-input-number v-model="loadTest.users" :min="1" :max="500" />
                <el-input-number v-model="loadTest.submissionsPerUser" :min="1" :max="10" />
                <el-input-number v-model="loadTest.examId" :min="1" />
              </div>
              <div class="inline-fields">
                <el-input-number v-model="loadTest.questionIdStart" :min="1" />
                <el-select v-model="loadTest.language">
                  <el-option label="Python" value="python" />
                  <el-option label="Java" value="java" />
                  <el-option label="C++" value="cpp" />
                </el-select>
              </div>
              <el-input v-model="loadTest.code" type="textarea" :rows="5" />
              <el-button type="warning" @click="startLoadTest">开始模拟并发</el-button>
              <p class="muted">{{ loadResult }}</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { distributedAdminApi, examApi } from '@/api/modules'

const route = useRoute()
const examLoading = ref(false)
const distributedLoading = ref(false)
const exams = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedExam = ref(null)
const statistics = ref({})
const studentScores = ref([])
const distributed = ref({})
const distributedTab = ref('nodes')
const testInput = ref('hello')
const expectedOutput = ref('hello')
const loadResult = ref('尚未开始模拟')
let refreshTimer = null

const testTask = reactive({
  userId: 900001,
  examId: 990001,
  questionId: 990001,
  language: 'python',
  code: 'print(input())'
})

const loadTest = reactive({
  users: 50,
  submissionsPerUser: 1,
  examId: 880001,
  questionIdStart: 880001,
  language: 'python',
  code: 'print(input())',
  input: 'hello',
  expectedOutput: 'hello'
})

const nodeCount = computed(() => (distributed.value.nodes || []).length)
const healthyNodeCount = computed(() => (distributed.value.nodes || []).filter(node => node.healthy).length)
const runningTaskCount = computed(() => (distributed.value.nodes || []).reduce((sum, node) => sum + Number(node.runningTasks || 0), 0))
const sessionCount = computed(() => (distributed.value.interactiveSessions || []).length)

const loadExams = async () => {
  examLoading.value = true
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
    console.error('Failed to load admin exams:', error)
    ElMessage.error('加载考试情况失败')
  } finally {
    examLoading.value = false
  }
}

const selectExam = async (exam) => {
  if (!exam) return
  selectedExam.value = exam
  await refreshExamMonitor()
}

const refreshExamMonitor = async () => {
  if (!selectedExam.value) return
  try {
    const [statRes, scoreRes] = await Promise.all([
      examApi.getStatistics(selectedExam.value.id),
      examApi.getStudentScores(selectedExam.value.id)
    ])
    statistics.value = statRes.data || {}
    studentScores.value = scoreRes.data || []
  } catch (error) {
    console.error('Failed to refresh exam monitor:', error)
    ElMessage.error('刷新考试统计失败')
  }
}

const loadDistributedSnapshot = async () => {
  distributedLoading.value = true
  try {
    const res = await distributedAdminApi.snapshot()
    distributed.value = res.data || {}
  } catch (error) {
    console.error('Failed to load distributed snapshot:', error)
    ElMessage.error('加载分布式监控失败')
  } finally {
    distributedLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([
    loadExams(),
    loadDistributedSnapshot()
  ])
}

const submitTestTask = async () => {
  const res = await distributedAdminApi.submitTestTask({
    ...testTask,
    testCases: [{ input: testInput.value, expectedOutput: expectedOutput.value }]
  })
  ElMessage.success(`测试任务已入队：${res.data.taskId}`)
  await loadDistributedSnapshot()
}

const startLoadTest = async () => {
  const payload = {
    ...loadTest,
    input: testInput.value,
    expectedOutput: expectedOutput.value
  }
  const res = await distributedAdminApi.startLoadTest(payload)
  const data = res.data || {}
  loadResult.value = `请求 ${data.requestedTasks || 0} 个，入队 ${data.acceptedTasks || 0} 个，拒绝 ${data.rejectedTasks || 0} 个`
  ElMessage.success('并发模拟任务已提交')
  await loadDistributedSnapshot()
}

const nodeLoadPercent = (node) => {
  const maxConcurrency = Number(node.metadata?.maxConcurrency || node.metadata?.['max-concurrency'] || 1)
  return Math.min(100, Math.round((Number(node.runningTasks || 0) / Math.max(1, maxConcurrency)) * 100))
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const examStatusText = (status) => ({
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  FINISHED: '已结束'
}[status] || status)

const examStatusType = (status) => ({
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

const judgeStatusType = (status) => ({
  AC: 'success',
  WAIT: 'info',
  JUDGE: 'warning',
  WA: 'danger',
  TLE: 'warning',
  MLE: 'warning',
  RE: 'danger',
  CE: 'danger',
  FAILED: 'danger'
}[status] || 'info')

onMounted(async () => {
  await refreshAll()
  refreshTimer = setInterval(() => {
    refreshExamMonitor().catch(() => {})
    loadDistributedSnapshot().catch(() => {})
  }, 5000)
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.admin-page {
  max-width: 1440px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
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

.page-actions,
.section-title,
.inline-fields {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  justify-content: space-between;
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

.section-card {
  margin-top: 16px;
}

.stat-grid,
.distributed-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.distributed-stats {
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

.tool-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.tool-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-panel h4 {
  margin: 0;
}

@media (max-width: 1100px) {
  .stat-grid,
  .distributed-stats,
  .tool-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
