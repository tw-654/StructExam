<template>
  <div class="home-container">
    <div class="title-bar">
      <h2>考试列表</h2>
    </div>

    <el-card v-loading="loading">
      <el-table :data="examList" stripe style="width: 100%">
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
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="!canEnter(row)"
              @click="handleEnterExam(row)"
            >
              进入考试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { examApi } from '@/api/modules'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const examList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadExams = async () => {
  loading.value = true
  try {
    const res = await examApi.getList(pageNum.value, pageSize.value)
    examList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('Failed to load exams:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const typeMap = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ONGOING: 'warning',
    FINISHED: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ONGOING: '进行中',
    FINISHED: '已结束'
  }
  return textMap[status] || status
}

const canEnter = (exam) => {
  const now = new Date()
  const start = new Date(exam.startTime)
  const end = new Date(exam.endTime)
  return now >= start && now <= end && (exam.status === 'PUBLISHED' || exam.status === 'ONGOING')
}

const handleEnterExam = async (exam) => {
  try {
    await ElMessageBox.confirm(
      `即将进入 "${exam.title}"，考试时长 ${exam.duration} 分钟。是否继续？`,
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
