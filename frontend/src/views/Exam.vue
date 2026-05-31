<template>
  <div class="exam-container" v-loading="pageLoading">
    <el-container>
      <el-aside width="280px" class="left-panel">
        <div class="exam-info">
          <h3>{{ examDetail.title }}</h3>
          <div class="timer" :class="{ danger: remainingSeconds <= 300 }">
            <span>剩余时间</span>
            <strong>{{ formattedTime }}</strong>
          </div>
        </div>

        <div class="question-nav">
          <h4>题目列表</h4>
          <div class="question-grid">
            <div
              v-for="(q, index) in questions"
              :key="q.id"
              class="question-item"
              :class="{ active: currentQuestionIndex === index, answered: answeredQuestions.includes(q.id) }"
              @click="selectQuestion(index)"
            >
              {{ index + 1 }}
            </div>
          </div>
          <div class="status-summary">
            <span>已做: {{ answeredQuestions.length }}/{{ questions.length }}</span>
          </div>
        </div>

        <div class="action-buttons">
          <el-button type="primary" @click="handleSaveCode">保存代码</el-button>
          <el-button type="success" @click="handleSubmitQuestion">提交本题</el-button>
          <el-button type="danger" @click="handleSubmitExam(false)">交卷并退出</el-button>
        </div>
      </el-aside>

      <el-main class="right-panel">
        <div class="question-header">
          <div class="question-title">
            <el-tag>{{ getQuestionTypeText(currentQuestion.type) }}</el-tag>
            <span class="score">{{ currentQuestion.score }} 分</span>
          </div>
          <h3>{{ currentQuestion.title }}</h3>
        </div>

        <div class="question-content" v-if="currentQuestion.content">
          <p v-html="formatContent(currentQuestion.content)"></p>
        </div>

        <div class="editor-container">
          <div class="editor-header">
            <span>代码编辑器</span>
            <div class="editor-actions">
              <el-button type="primary" size="small" @click="handleRunCode" :loading="isRunning">运行</el-button>
              <el-button type="warning" size="small" @click="handleStopCode" :disabled="!isRunning">停止</el-button>
              <el-select v-model="language" size="small" style="width: 120px">
                <el-option label="C++" value="cpp" />
                <el-option label="Java" value="java" />
                <el-option label="Python" value="python" />
              </el-select>
            </div>
          </div>
          <div ref="editorRef" class="monaco-editor"></div>
        </div>

        <div class="terminal-container" v-if="showTerminal">
          <div class="terminal-header">
            <span>终端</span>
            <span class="terminal-status" :class="{ running: isRunning }">
              {{ isRunning ? '运行中' : '已停止' }}
            </span>
          </div>
          <div class="terminal-body" ref="terminalRef">
            <template v-for="(line, index) in terminalLines" :key="index">
              <pre v-if="line.type === 'error'" class="terminal-line error">{{ line.content }}</pre>
              <div v-else class="terminal-line" :class="line.type">
                <span v-if="line.type === 'input'" class="terminal-prompt">&gt;</span>{{ line.content }}
              </div>
            </template>
          </div>
        </div>

        <!-- 判定结果面板（提交本题后展示） -->
        <div class="judge-panel" v-if="showJudgeResult">
          <div class="judge-header">
            <span class="judge-title">判定结果</span>
            <div class="judge-summary" v-if="judgeResult">
              <el-tag :type="judgeStatusTagType(judgeResult.judgeStatus)" size="large">
                {{ judgeStatusLabel(judgeResult.judgeStatus) }}
              </el-tag>
              <span class="judge-meta" v-if="judgeResult.judgeStatus !== 'JUDGING'">
                通过 {{ judgePassSummary.passed }}/{{ judgePassSummary.total }} 个用例
                <template v-if="judgePassSummary.total > 0">
                  （{{ judgePassSummary.percent }}%）
                </template>
                · 得分 {{ judgeResult.score }}/{{ judgeResult.maxScore }}
                · 耗时 {{ judgeResult.timeUsedMs != null ? judgeResult.timeUsedMs + ' ms' : '-' }}
              </span>
            </div>
            <el-button size="small" plain @click="showJudgeResult = false">关闭</el-button>
          </div>

          <!-- 加载中 -->
          <div class="judge-loading" v-if="judgeLoading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>判题中，请稍候…</span>
          </div>

          <!-- 编译错误 -->
          <div class="judge-error-block" v-if="judgeResult && judgeResult.compileError">
            <div class="error-label">编译错误</div>
            <pre class="error-pre">{{ judgeResult.compileError }}</pre>
          </div>

          <!-- 运行时错误 -->
          <div class="judge-error-block" v-if="judgeResult && judgeResult.runtimeError">
            <div class="error-label">运行时错误</div>
            <pre class="error-pre">{{ judgeResult.runtimeError }}</pre>
          </div>

        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examApi, codeApi, judgeRecordApi } from '@/api/modules'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import * as monaco from 'monaco-editor'

const route = useRoute()
const router = useRouter()

const examId = computed(() => Number(route.params.id) || 0)
const examDetail = ref({})
const questions = ref([])
const currentQuestionIndex = ref(0)
const pageLoading = ref(true)
const language = ref('cpp')
const editorRef = ref(null)
const remainingSeconds = ref(0)
const showTerminal = ref(false)
const isRunning = ref(false)
const terminalLines = ref([])
const terminalRef = ref(null)
const answeredQuestions = ref([])

let editor = null
let timer = null
let submitted = false
let judgePollTimer = null

// ---------- 判定结果状态 ----------
const showJudgeResult = ref(false)
const judgeLoading = ref(false)
const judgeResult = ref(null)

/** 考试页仅展示通过比例，不展示逐条 IO */
const judgePassSummary = computed(() => {
  const passed = judgeResult.value?.passedCases ?? 0
  const total = judgeResult.value?.totalCases ?? 0
  const percent = total > 0 ? Math.round((passed / total) * 100) : 0
  return { passed, total, percent }
})

const currentQuestion = computed(() => questions.value[currentQuestionIndex.value] || {})

const formattedTime = computed(() => {
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.floor((remainingSeconds.value % 3600) / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const getQuestionTypeText = (type) => {
  const typeMap = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    PROGRAMMING: '编程题'
  }
  return typeMap[type] || type || ''
}

const formatContent = (content) => {
  return content ? content.replace(/\n/g, '<br>') : ''
}

const selectQuestion = async (index) => {
  if (currentQuestionIndex.value !== index) {
    await saveCurrentCode()
  }
  currentQuestionIndex.value = index
  await loadQuestionCode()
}

const saveCurrentCode = async (force = false) => {
  if (!editor || !currentQuestion.value.id || (submitted && !force)) return

  try {
    await codeApi.save({
      examId: examId.value,
      questionId: currentQuestion.value.id,
      code: editor.getValue(),
      language: language.value
    })
  } catch (error) {
    console.error('Failed to save code:', error)
  }
}

const loadQuestionCode = async () => {
  if (!editor || !currentQuestion.value.id) return

  try {
    const res = await codeApi.get(examId.value, currentQuestion.value.id)
    editor.setValue(res.data?.code || getDefaultCode())
  } catch (error) {
    editor.setValue(getDefaultCode())
  }
}

const getDefaultCode = () => {
  if (language.value === 'java') {
    return `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 在这里编写代码
    }
}`
  }
  if (language.value === 'python') {
    return `# 在这里编写代码
`
  }
  return `#include <iostream>
using namespace std;

int main() {
    // 在这里编写代码
    return 0;
}`
}

const handleSaveCode = async () => {
  await saveCurrentCode(true)
  ElMessage.success('代码已保存')
}

const submitDistributedTask = async (code, questionId) => {
  const res = await codeApi.submitDistributed({
    examId: examId.value,
    questionId,
    code,
    language: language.value
  })
  return res.data.taskId
}

const submitOfficialJudgeTask = async (code, questionId) => {
  const res = await codeApi.submit({
    examId: examId.value,
    questionId,
    code,
    language: language.value
  })
  return res.data.taskId
}

const waitJudgeResult = async (taskId, maxAttempts = 60) => {
  for (let i = 0; i < maxAttempts; i++) {
    await new Promise(resolve => setTimeout(resolve, 1000))
    try {
      const res = await codeApi.getDistributedResult(taskId)
      if (res.data) return res.data
    } catch (error) {
      // 404 means the task is still running.
    }
  }
  throw new Error('判题超时，请稍后刷新查看结果')
}

const handleRunCode = async () => {
  if (!editor || isRunning.value) return

  showTerminal.value = true
  terminalLines.value = [{ type: 'output', content: '> 正在提交到分布式沙箱运行...' }]
  scrollToBottom()

  try {
    isRunning.value = true
    const taskId = await submitDistributedTask(editor.getValue(), currentQuestion.value.id)
    terminalLines.value.push({ type: 'output', content: `> 任务已入队: ${taskId}` })
    const result = await waitJudgeResult(taskId)

    terminalLines.value.push({ type: result.status === 'AC' ? 'output' : 'error', content: `> 运行完成: ${result.status}` })
    if (result.output) {
      terminalLines.value.push({ type: 'output', content: '--- 程序输出 ---' })
      terminalLines.value.push({ type: 'output', content: result.output })
    }
    if (result.error) {
      terminalLines.value.push({ type: 'error', content: '--- 错误信息 ---' })
      terminalLines.value.push({ type: 'error', content: result.error })
    }
    if (result.timeUsedMs != null) {
      terminalLines.value.push({ type: 'output', content: `执行时间: ${result.timeUsedMs} ms` })
    }
  } catch (error) {
    console.error('Failed to run code:', error)
    terminalLines.value.push({ type: 'error', content: '执行失败: ' + (error.message || '未知错误') })
  } finally {
    isRunning.value = false
    scrollToBottom()
  }
}

const handleStopCode = () => {
  isRunning.value = false
  terminalLines.value.push({ type: 'output', content: '> 已停止等待结果' })
  scrollToBottom()
}

const scrollToBottom = () => {
  setTimeout(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight
    }
  }, 100)
}

const handleSubmitQuestion = async () => {
  try {
    await ElMessageBox.confirm('确定提交当前题目吗？提交后请等待判题结果。', '提交确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const taskId = await submitOfficialJudgeTask(editor.getValue(), currentQuestion.value.id)
    if (!answeredQuestions.value.includes(currentQuestion.value.id)) {
      answeredQuestions.value.push(currentQuestion.value.id)
    }
    ElMessage.success(`题目已提交，任务号：${taskId}`)
    startJudgePoll(currentQuestion.value.id)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to submit question:', error)
    }
  }
}

/** 开始轮询判题结果（每 2 秒一次，最多 60 次）。 */
const startJudgePoll = (questionId) => {
  stopJudgePoll()
  showJudgeResult.value = true
  judgeLoading.value = true
  judgeResult.value = null

  let attempts = 0
  judgePollTimer = setInterval(async () => {
    attempts++
    try {
      const res = await judgeRecordApi.getLatest(examId.value, questionId)
      const record = res.data
      judgeResult.value = record
      if (record && record.judgeStatus !== 'JUDGING') {
        judgeLoading.value = false
        stopJudgePoll()
      }
    } catch (error) {
      // ignore, keep polling
    }
    if (attempts >= 60) {
      judgeLoading.value = false
      stopJudgePoll()
      ElMessage.warning('等待判题结果超时，请稍后刷新')
    }
  }, 2000)
}

const stopJudgePoll = () => {
  if (judgePollTimer) {
    clearInterval(judgePollTimer)
    judgePollTimer = null
  }
}

const judgeStatusLabel = (status) => ({
  JUDGING: '判题中',
  AC: '通过',
  WA: '答案错误',
  TLE: '超时',
  MLE: '内存超限',
  RE: '运行错误',
  CE: '编译错误',
  FAILED: '系统错误'
}[status] || status || '-')

const judgeStatusTagType = (status) => ({
  JUDGING: 'info',
  AC: 'success',
  WA: 'danger',
  TLE: 'warning',
  MLE: 'warning',
  RE: 'danger',
  CE: 'danger',
  FAILED: 'danger'
}[status] || 'info')
const caseStatusLabel = (status) => ({
  AC: 'AC',
  WA: '答案错误',
  TLE: '超时',
  MLE: '内存超限',
  RE: '运行错误',
  CE: '编译错误',
  FAILED: '失败'
}[status] || status || '-')
const doSubmitExam = async () => {
  if (submitted) return
  submitted = true
  if (timer) clearInterval(timer)

  await saveCurrentCode()
  await codeApi.submitAll(examId.value)
  await examApi.submitExam(examId.value)
}

const handleSubmitExam = async (auto = false) => {
  try {
    if (!auto) {
      await ElMessageBox.confirm('确定要交卷吗？交卷后无法继续答题。', '交卷确认', {
        confirmButtonText: '确定交卷',
        cancelButtonText: '取消',
        type: 'warning'
      })
    }

    await doSubmitExam()
    ElMessage.success(auto ? '考试时间已到，系统已自动交卷' : '交卷成功')
    router.push('/home')
  } catch (error) {
    submitted = false
    if (error !== 'cancel') {
      console.error('Failed to submit exam:', error)
      ElMessage.error('交卷失败，请重试')
    }
  }
}

const initEditor = () => {
  if (!editorRef.value) return

  editor = monaco.editor.create(editorRef.value, {
    value: getDefaultCode(),
    language: language.value,
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 14,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    lineNumbers: 'on',
    renderLineHighlight: 'line',
    tabSize: 4,
    insertSpaces: true
  })
}

const loadRuntime = async () => {
  const runtimeRes = await examApi.getRuntime(examId.value)
  return runtimeRes.data || {}
}

const ensureExamRuntime = async () => {
  try {
    let runtime = await loadRuntime()
    const record = runtime.record
    if (record?.status === 'SUBMITTED' || record?.status === 'GRADED') {
      ElMessage.warning('该考试已交卷')
      router.push('/home')
      return null
    }
    if (record?.status === 'IN_PROGRESS') {
      return runtime
    }
  } catch (error) {
    // No record yet; enter the exam below.
  }

  await examApi.enterExam(examId.value)
  return await loadRuntime()
}

const updateRemainingTime = (runtime) => {
  remainingSeconds.value = Math.max(0, Number(runtime?.remainingSeconds || 0))
}

const loadExamData = async () => {
  pageLoading.value = true
  try {
    const detailRes = await examApi.getDetail(examId.value)
    examDetail.value = detailRes.data || {}
    questions.value = examDetail.value.questions || []

    const runtime = await ensureExamRuntime()
    if (!runtime) return false
    updateRemainingTime(runtime)
    return true
  } catch (error) {
    console.error('Failed to load exam:', error)
    ElMessage.error('加载考试失败')
    router.push('/home')
    return false
  } finally {
    pageLoading.value = false
  }
}

const startTimer = () => {
  if (timer) clearInterval(timer)
  if (remainingSeconds.value <= 0) {
    ElMessage.warning('当前考试剩余时间为 0，请回到首页确认考试状态')
    router.push('/home')
    return
  }

  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
      return
    }
    clearInterval(timer)
    ElMessage.warning('考试时间已到，正在自动交卷')
    handleSubmitExam(true)
  }, 1000)
}

watch(language, (newLang) => {
  if (!editor) return
  const model = editor.getModel()
  if (model) {
    monaco.editor.setModelLanguage(model, newLang)
  }
  editor.setValue(getDefaultCode())
})

onMounted(async () => {
  const ready = await loadExamData()
  if (!ready) return
  await new Promise(resolve => setTimeout(resolve, 100))
  initEditor()
  await loadQuestionCode()
  startTimer()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  stopJudgePoll()
  if (editor) editor.dispose()
})
</script>

<style scoped>
.exam-container {
  height: calc(100vh - 60px);
  background: #f5f5f5;
}

.el-container {
  height: 100%;
}

.left-panel {
  background: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.exam-info {
  padding-bottom: 16px;
  border-bottom: 1px solid #e6e6e6;
}

.exam-info h3 {
  margin: 0 0 12px 0;
  color: #333;
}

.timer {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 4px;
}

.timer.danger {
  background: #fef0f0;
}

.timer span {
  font-size: 12px;
  color: #666;
}

.timer strong {
  font-size: 24px;
  color: #67c23a;
  font-family: monospace;
}

.timer.danger strong {
  color: #f56c6c;
}

.question-nav {
  flex: 1;
  padding: 16px 0;
  overflow-y: auto;
}

.question-nav h4 {
  margin: 0 0 12px 0;
  color: #333;
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.question-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

.question-item:hover {
  border-color: #409eff;
}

.question-item.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.question-item.answered {
  background: #67c23a;
  border-color: #67c23a;
  color: #fff;
}

.status-summary {
  margin-top: 12px;
  font-size: 12px;
  color: #666;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.right-panel {
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
  overflow-y: auto;
}

.question-header,
.question-content,
.editor-container {
  background: #fff;
  border-radius: 4px;
}

.question-header,
.question-content {
  padding: 16px;
}

.question-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.question-title .score {
  color: #f56c6c;
  font-weight: bold;
}

.question-header h3 {
  margin: 0;
  color: #333;
}

.question-content {
  line-height: 1.8;
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 300px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e6e6e6;
  font-weight: 500;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.monaco-editor {
  flex: 1;
  min-height: 250px;
}

.terminal-container {
  background: #1e1e1e;
  border-radius: 4px;
  overflow: hidden;
}

.terminal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #333;
  color: #fff;
  font-weight: 500;
}

.terminal-status {
  font-size: 12px;
  color: #999;
  padding: 2px 8px;
  background: #666;
  border-radius: 10px;
}

.terminal-status.running {
  background: #67c23a;
  color: #fff;
}

.terminal-body {
  padding: 12px;
  min-height: 2.5rem;
  height: auto;
  max-height: min(50vh, 560px);
  overflow-y: auto;
  overflow-x: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 14px;
  line-height: 1.5;
}

.terminal-line {
  margin: 2px 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.terminal-line.output {
  color: #d4d4d4;
}

.terminal-line.input {
  color: #4ec9b0;
}

.terminal-line.error,
.terminal-body pre.error {
  color: #f14c4c;
  margin: 4px 0;
  padding: 0;
  background: transparent;
  border: none;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.terminal-prompt {
  margin-right: 4px;
}

/* ---- 判定结果面板 ---- */
.judge-panel {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
  max-height: 400px;
  overflow-y: auto;
}

.judge-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e6e6e6;
}

.judge-title {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
}

.judge-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.judge-meta {
  font-size: 13px;
  color: #666;
}

.judge-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  color: #409eff;
  font-size: 14px;
}

.judge-error-block {
  margin: 12px 16px;
  background: #fff0f0;
  border-radius: 4px;
  padding: 10px 12px;
}

.error-label {
  font-size: 12px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 6px;
}

.error-pre {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: #c0392b;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
  max-height: min(50vh, 560px);
  overflow: auto;
}

</style>
