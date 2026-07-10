<template>
  <div class="home-container">
    <div class="title-bar">
      <h2>考试列表</h2>
    </div>

    <el-card v-loading="loading">
      <el-table 
        :data="examList" 
        stripe 
        style="width: 100%"
        :scroll-y="600"
        :virtual-scroll="true"
        :virtual-scroll-item-size="54"
      >
        <el-table-column prop="title" label="考试名称" min-width="200" />
        <el-table-column prop="duration" label="时长(分钟)" width="120" align="center" />
        <el-table-column prop="totalScore" label="总分" width="100" align="center" />
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="displayStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.displayStatus)">{{ getStatusText(row.displayStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="!canEnter(row)"
              @click="handleExamAction(row)"
            >
              {{ isStaff ? '查看详情' : '进入考试' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { examApi } from '@/api/modules'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const isTeacher = computed(() => authStore.role === 'TEACHER')
const isAdmin = computed(() => authStore.role === 'ADMIN')
const isStaff = computed(() => isTeacher.value || isAdmin.value)

const loading = ref(false)
const examList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadExams = async () => {
  loading.value = true
  try {
    const [examRes, recordRes] = await Promise.all([
      examApi.getList(pageNum.value, pageSize.value),
      examApi.getRecordList().catch(() => ({ data: [] }))
    ])
    const records = recordRes.data || []
    const recordMap = new Map(records.map(record => [record.examId, record]))
    examList.value = (examRes.data.records || []).map(exam => {
      const record = recordMap.get(exam.id)
      return {
        ...exam,
        userRecordStatus: record?.status,
        displayStatus: record?.status || exam.status
      }
    })
    total.value = examRes.data.total || 0
  } catch (error) {
    console.error('Failed to load exams:', error)
    ElMessage.error('加载考试列表失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  // 如果是数组格式 [年, 月, 日, 时, 分, 秒]，转换为Date对象
  if (Array.isArray(dateStr)) {
    // 月份需要减1（JavaScript月份从0开始）
    const year = dateStr[0] || 0
    const month = (dateStr[1] || 1) - 1
    const day = dateStr[2] || 1
    const hour = dateStr[3] || 0
    const minute = dateStr[4] || 0
    const second = dateStr[5] || 0
    return new Date(year, month, day, hour, minute, second).toLocaleString('zh-CN')
  }
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const typeMap = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ONGOING: 'warning',
    FINISHED: 'danger',
    IN_PROGRESS: 'warning',
    SUBMITTED: 'success',
    GRADED: 'success'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ONGOING: '进行中',
    FINISHED: '已结束',
    IN_PROGRESS: '答题中',
    SUBMITTED: '已交卷',
    GRADED: '已评分'
  }
  return textMap[status] || status
}

// 解析日期，支持数组格式和字符串格式
const parseDate = (dateValue) => {
  if (!dateValue) return null
  if (Array.isArray(dateValue)) {
    const year = dateValue[0] || 0
    const month = (dateValue[1] || 1) - 1
    const day = dateValue[2] || 1
    const hour = dateValue[3] || 0
    const minute = dateValue[4] || 0
    const second = dateValue[5] || 0
    return new Date(year, month, day, hour, minute, second)
  }
  return new Date(dateValue)
}

const canEnter = (exam) => {
  if (isStaff.value) {
    return true
  }
  if (exam.userRecordStatus === 'SUBMITTED' || exam.userRecordStatus === 'GRADED') {
    return false
  }
  const now = new Date()
  const start = parseDate(exam.startTime)
  const end = parseDate(exam.endTime)
  return now >= start && now <= end && (exam.status === 'PUBLISHED' || exam.status === 'ONGOING')
}

const handleExamAction = async (exam) => {
  if (isAdmin.value) {
    router.push(`/admin?examId=${exam.id}`)
    return
  }
  if (isTeacher.value) {
    router.push(`/teacher?examId=${exam.id}`)
    return
  }
  try {
    if (exam.userRecordStatus === 'SUBMITTED' || exam.userRecordStatus === 'GRADED') {
      ElMessage.warning('该考试已交卷，不能再次进入')
      return
    }
    await ElMessageBox.confirm(
      `即将进入“${exam.title}”，考试时长 ${exam.duration} 分钟。是否继续？`,
      '进入考试',
      {
        confirmButtonText: '进入',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await examApi.enterExam(exam.id)
    router.push(`/exam/${exam.id}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to enter exam:', error)
    }
  }
}

const handleSizeChange = () => {
  loadExams()
}

const handlePageChange = () => {
  loadExams()
}

onMounted(() => {
  loadExams()
})
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.title-bar {
  margin-bottom: 20px;
}

.title-bar h2 {
  margin: 0;
  color: #333;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
