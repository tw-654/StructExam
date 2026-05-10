<template>
  <div class="exam-container" v-loading="pageLoading">
    <el-container>
      <el-aside width="280px" class="left-panel">
        <div class="exam-info">
          <h3>{{ examDetail.title }}</h3>
          <div class="timer">
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
          <el-button type="danger" @click="handleSubmitExam">交卷并退出</el-button>
        </div>
      </el-aside>

      <el-main class="right-panel">
        <div class="question-header">
          <div class="question-title">
            <el-tag>{{ getQuestionTypeText(currentQuestion.type) }}</el-tag>
            <span class="score">{{ currentQuestion.score }}分</span>
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
              <el-button type="primary" size="small" @click="handleRunCode">运行</el-button>
              <el-button type="warning" size="small" @click="handleStopCode" :disabled="!isRunning">停止</el-button>
              <el-select v-model="language" size="small" style="width: 120px">
                <el-option label="Java" value="java" />
                <el-option label="C++" value="cpp" />
                <el-option label="Python" value="python" />
              </el-select>
            </div>
          </div>
          <div ref="editorRef" class="monaco-editor"></div>
        </div>

        <div class="terminal-container" v-if="showTerminal">
          <div class="terminal-header">
            <span>终端</span>
            <span class="terminal-status" :class="{ running: isRunning }">{{ isRunning ? '运行中' : '已停止' }}</span>
          </div>
          <div class="terminal-body" ref="terminalRef">
            <div v-for="(line, index) in terminalLines" :key="index" :class="line.type">
              <span v-if="line.type === 'input'">></span>
              {{ line.content }}
            </div>
            <div v-if="isWaitingInput" class="input-prompt">
              <span>> </span>
              <input 
                ref="terminalInputRef"
                v-model="terminalInput" 
                @keydown.enter="handleTerminalInput" 
                class="terminal-input"
                :disabled="!isWaitingInput"
                placeholder="输入数据..."
              />
            </div>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examApi, questionApi, codeApi } from '@/api/modules'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as monaco from 'monaco-editor'
import sandboxWs from '@/utils/sandboxWs'

const route = useRoute()
const router = useRouter()

const examId = computed(() => route.params.id)
const examDetail = ref({})
const questions = ref([])
const currentQuestionIndex = ref(0)
const pageLoading = ref(true)
const language = ref('java')
const editorRef = ref(null)
let editor = null

const remainingSeconds = ref(0)

const showTerminal = ref(false)
const isRunning = ref(false)
const isWaitingInput = ref(false)
const terminalLines = ref([])
const terminalInput = ref('')
const terminalInputRef = ref(null)
const terminalRef = ref(null)
let timer = null

const currentQuestion = computed(() => {
  return questions.value[currentQuestionIndex.value] || {}
})

const answeredQuestions = ref([])

const showResult = ref(false)
const runResult = ref('')
const inputData = ref('')

const formattedTime = computed(() => {
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.floor((remainingSeconds.value % 3600) / 60)
  const seconds = remainingSeconds.value % 60
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
})

const getQuestionTypeText = (type) => {
  const typeMap = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    PROGRAMMING: '编程题'
  }
  return typeMap[type] || type
}

const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}

const selectQuestion = (index) => {
  if (currentQuestionIndex.value !== index) {
    saveCurrentCode()
  }
  currentQuestionIndex.value = index
  loadQuestionCode()
}

const saveCurrentCode = async () => {
  if (!editor || !currentQuestion.value.id) return

  const code = editor.getValue()
  try {
    await codeApi.save({
      examId: examId.value,
      questionId: currentQuestion.value.id,
      code: code,
      language: language.value
    })
  } catch (error) {
    console.error('Failed to save code:', error)
  }
}

const loadQuestionCode = async () => {
  if (!currentQuestion.value.id) return

  try {
    const res = await codeApi.get(examId.value, currentQuestion.value.id)
    const code = res.data?.code || getDefaultCode(currentQuestion.value.type)
    editor.setValue(code)
  } catch (error) {
    const defaultCode = getDefaultCode(currentQuestion.value.type)
    editor.setValue(defaultCode)
  }
}

const getDefaultCode = (type) => {
  if (type === 'PROGRAMMING') {
    if (language.value === 'java') {
      return `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Your code here
    }
}`
    } else if (language.value === 'cpp') {
      return `#include <iostream>
using namespace std;

int main() {
    // Your code here
    return 0;
}`
    } else {
      return `# Your code here
`
    }
  }
  return ''
}

const handleSaveCode = async () => {
  await saveCurrentCode()
  ElMessage.success('Code saved')
}

const handleRunCode = async () => {
  if (!editor) return

  showTerminal.value = true
  terminalLines.value = []
  
  terminalLines.value.push({ type: 'output', content: '> 正在连接沙箱...' })
  scrollToBottom()

  try {
    await sandboxWs.connect()
    
    sandboxWs.onOutput((data, isError) => {
      terminalLines.value.push({ 
        type: isError ? 'error' : 'output', 
        content: data 
      })
      scrollToBottom()
    })

    sandboxWs.onError((error) => {
      terminalLines.value.push({ type: 'error', content: error })
      scrollToBottom()
    })

    sandboxWs.onStatus((status) => {
      if (status === 'WAITING_INPUT') {
        isWaitingInput.value = true
        isRunning.value = true
        terminalLines.value.push({ type: 'output', content: '> 程序等待输入:' })
        scrollToBottom()
        setTimeout(() => {
          if (terminalInputRef.value) {
            terminalInputRef.value.focus()
          }
        }, 100)
      } else if (status === 'RUNNING') {
        isRunning.value = true
        isWaitingInput.value = false
      } else if (status === 'TERMINATED' || status === 'TIMEOUT') {
        isRunning.value = false
        isWaitingInput.value = false
        terminalLines.value.push({ 
          type: 'output', 
          content: status === 'TIMEOUT' ? '> 程序执行超时，已被终止。' : '> 程序已终止。' 
        })
        scrollToBottom()
        sandboxWs.disconnect()
      } else if (status === 'COMPILE_ERROR') {
        isRunning.value = false
        isWaitingInput.value = false
      } else if (status === 'ERROR') {
        isRunning.value = false
        isWaitingInput.value = false
      }
    })

    const code = editor.getValue()
    terminalLines.value.push({ type: 'output', content: `> 正在启动 ${language.value} 程序...` })
    scrollToBottom()
    
    sandboxWs.start(code, language.value, 60)
    isRunning.value = true
    
  } catch (error) {
    console.error('Failed to connect to sandbox:', error)
    terminalLines.value.push({ type: 'error', content: '连接沙箱失败: ' + error.message })
    isRunning.value = false
    scrollToBottom()
  }
}

const handleTerminalInput = async () => {
  if (!isWaitingInput.value) return

  const input = terminalInput.value
  terminalLines.value.push({ type: 'input', content: input })
  terminalInput.value = ''
  scrollToBottom()

  sandboxWs.sendInput(input)
  isWaitingInput.value = false
}

const handleStopCode = () => {
  sandboxWs.terminate()
  isRunning.value = false
  isWaitingInput.value = false
  terminalLines.value.push({ type: 'output', content: '> 程序已手动终止。' })
  scrollToBottom()
}

const stopCode = () => {
  sandboxWs.terminate()
  isRunning.value = false
  isWaitingInput.value = false
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
    await ElMessageBox.confirm('确定要提交当前题目吗？提交后无法修改。', '提交确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await saveCurrentCode()
    await codeApi.submit({
      examId: examId.value,
      questionId: currentQuestion.value.id
    })

    if (!answeredQuestions.value.includes(currentQuestion.value.id)) {
      answeredQuestions.value.push(currentQuestion.value.id)
    }

    ElMessage.success('Question submitted')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to submit question:', error)
    }
  }
}

const handleSubmitExam = async () => {
  try {
    await ElMessageBox.confirm('确定要交卷吗？交卷后无法继续答题。', '交卷确认', {
      confirmButtonText: '确定交卷',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await saveCurrentCode()
    await codeApi.submitAll(examId.value)
    await examApi.submitExam(examId.value)

    ElMessage.success('Exam submitted successfully')
    router.push('/home')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to submit exam:', error)
    }
  }
}

const initEditor = () => {
  if (!editorRef.value) return

  editor = monaco.editor.create(editorRef.value, {
    value: getDefaultCode('PROGRAMMING'),
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

  editor.onDidChangeModelContent(() => {
    showResult.value = false
  })
}

const loadExamData = async () => {
  pageLoading.value = true
  try {
    const detailRes = await examApi.getDetail(examId.value)
    examDetail.value = detailRes.data
    questions.value = detailRes.data.questions || []

    const recordRes = await examApi.getRecord(examId.value)
    if (recordRes.data?.enterTime) {
      const enterTime = new Date(recordRes.data.enterTime).getTime()
      const duration = examDetail.value.duration * 60 * 1000
      const endTime = enterTime + duration
      const remaining = Math.max(0, endTime - Date.now())
      remainingSeconds.value = Math.floor(remaining / 1000)
    }

    if (questions.value.length > 0) {
      loadQuestionCode()
    }
  } catch (error) {
    console.error('Failed to load exam:', error)
    ElMessage.error('Failed to load exam')
    router.push('/home')
  } finally {
    pageLoading.value = false
  }
}

const startTimer = () => {
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      clearInterval(timer)
      ElMessage.warning('Time is up, auto submitting')
      handleSubmitExam()
    }
  }, 1000)
}

watch(language, (newLang) => {
  if (editor) {
    const model = editor.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, newLang)
    }
    editor.setValue(getDefaultCode('PROGRAMMING'))
  }
})

onMounted(async () => {
  await loadExamData()
  await new Promise(resolve => setTimeout(resolve, 100))
  initEditor()
  startTimer()
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
  if (editor) {
    editor.dispose()
  }
  sandboxWs.disconnect()
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

.timer span {
  font-size: 12px;
  color: #666;
}

.timer strong {
  font-size: 24px;
  color: #67c23a;
  font-family: monospace;
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
}

.question-header {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
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
  background: #fff;
  padding: 16px;
  border-radius: 4px;
  line-height: 1.8;
}

.editor-container {
  flex: 1;
  background: #fff;
  border-radius: 4px;
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

.monaco-editor {
  flex: 1;
  min-height: 250px;
}

.result-container {
  background: #fff;
  border-radius: 4px;
}

.result-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e6e6e6;
  font-weight: 500;
}

.result-output {
  margin: 0;
  padding: 16px;
  background: #f5f5f5;
  min-height: 80px;
  max-height: 150px;
  overflow-y: auto;
  font-family: monospace;
  font-size: 13px;
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
  min-height: 150px;
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.5;
}

.terminal-body .output {
  color: #d4d4d4;
  margin: 2px 0;
}

.terminal-body .input {
  color: #4ec9b0;
  margin: 2px 0;
}

.terminal-body .error {
  color: #f14c4c;
  margin: 2px 0;
}

.input-prompt {
  display: flex;
  align-items: center;
  color: #4ec9b0;
  margin-top: 8px;
}

.input-prompt span {
  margin-right: 4px;
}

.terminal-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #4ec9b0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
}

.terminal-input::placeholder {
  color: #666;
}
</style>
