<template>
  <div class="history-container">
    <div class="title-bar">
      <el-button @click="$router.push('/home')" style="margin-right: 16px">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>考试记录</h2>
    </div>

    <el-card v-loading="loading">
      <el-table 
        :data="records" 
        stripe 
        style="width: 100%"
        :scroll-y="600"
        :virtual-scroll="true"
        :virtual-scroll-item-size="54"
      >
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
import { ref, onMounted, nextTick } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { examApi } from '@/api/modules'

const loading = ref(false)
const records = ref([])

const loadRecords = async () => {
  loading.value = true
  try {
    const startTime = performance.now()
    const res = await examApi.getRecordList()
    records.value = res.data || []
    
    await nextTick()
    const endTime = performance.now()
    console.log(`✅ 数据加载+首次渲染耗时: ${(endTime - startTime).toFixed(2)} ms`)
    console.log(`✅ 加载数据量: ${records.value.length} 条`)
  } catch (error) {
    console.error('Failed to load records:', error)
  } finally {
    loading.value = false
  }
}

const testVirtualScrollPerformance = async () => {
  const scrollContainer = document.querySelector('.el-table__body-wrapper')
  if (!scrollContainer) return
  
  const results = []
  
  for (let i = 0; i < 10; i++) {
    const startTime = performance.now()
    
    scrollContainer.scrollTop += 500
    
    await nextTick()
    
    const endTime = performance.now()
    const renderTime = endTime - startTime
    results.push(renderTime)
    
    console.log(`✅ 滚动第 ${i + 1} 次渲染耗时: ${renderTime.toFixed(2)} ms`)
    
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  
  const avgTime = results.reduce((a, b) => a + b, 0) / results.length
  const maxTime = Math.max(...results)
  const minTime = Math.min(...results)
  
  console.log('===== 虚拟滚动性能测试结果 =====')
  console.log(`平均渲染耗时: ${avgTime.toFixed(2)} ms`)
  console.log(`最大渲染耗时: ${maxTime.toFixed(2)} ms`)
  console.log(`最小渲染耗时: ${minTime.toFixed(2)} ms`)
  console.log(`数据量: ${records.value.length} 条`)
  console.log('=================================')
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

onMounted(async () => {
  await loadRecords()
  
  setTimeout(() => {
    testVirtualScrollPerformance()
  }, 1000)
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
