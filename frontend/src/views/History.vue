<template>
  <div class="history-container">
    <div class="title-bar">
      <h2>考试记录</h2>
    </div>

    <el-card v-loading="loading">
      <el-table :data="records" stripe style="width: 100%">
        <el-table-column prop="examId" label="考试ID" width="100" align="center" />
        <el-table-column prop="enterTime" label="进入时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.enterTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.submitTime) || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="100" align="center">
          <template #default="{ row }">
            {{ row.score !== null ? row.score : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { examApi } from '@/api/modules'

const loading = ref(false)
const records = ref([])

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await examApi.getList(1, 100)
    records.value = res.data.records || []
  } catch (error) {
    console.error('Failed to load records:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return null
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const typeMap = {
    NOT_STARTED: 'info',
    IN_PROGRESS: 'warning',
    SUBMITTED: 'success',
    GRADED: 'primary'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    NOT_STARTED: '未开始',
    IN_PROGRESS: '进行中',
    SUBMITTED: '已提交',
    GRADED: '已评分'
  }
  return textMap[status] || status
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.history-container {
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
</style>
