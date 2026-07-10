<template>
  <div class="question-nav">
    <h4>题目列表</h4>
    <div class="question-grid">
      <div
        v-for="(q, index) in questions"
        :key="q.id"
        class="question-item"
        :class="{ active: currentIndex === index, answered: answeredIds.includes(q.id) }"
        @click="handleSelect(index)"
      >
        {{ index + 1 }}
      </div>
    </div>
    <div class="status-summary">
      <span>已做: {{ answeredIds.length }}/{{ questions.length }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  questions: {
    type: Array,
    default: () => []
  },
  currentIndex: {
    type: Number,
    default: 0
  },
  answeredIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select'])

const handleSelect = (index) => {
  if (index !== props.currentIndex) {
    emit('select', index)
  }
}
</script>

<style scoped>
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
</style>