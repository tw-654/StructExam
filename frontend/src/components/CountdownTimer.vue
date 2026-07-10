<template>
  <div class="timer" :class="{ danger: remainingSeconds <= 300 }">
    <span>剩余时间</span>
    <strong>{{ formattedTime }}</strong>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  initialSeconds: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['timeout'])

const remainingSeconds = ref(props.initialSeconds)
let timer = null

const formattedTime = computed(() => {
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.floor((remainingSeconds.value % 3600) / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const startTimer = () => {
  if (timer) clearInterval(timer)
  if (remainingSeconds.value <= 0) {
    emit('timeout')
    return
  }

  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
      return
    }
    clearInterval(timer)
    emit('timeout')
  }, 1000)
}

const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const updateSeconds = (seconds) => {
  remainingSeconds.value = Math.max(0, Number(seconds))
}

watch(() => props.initialSeconds, (newVal) => {
  remainingSeconds.value = Math.max(0, Number(newVal))
})

onMounted(() => {
  if (remainingSeconds.value > 0) {
    startTimer()
  }
})

onBeforeUnmount(() => {
  stopTimer()
})

defineExpose({
  startTimer,
  stopTimer,
  updateSeconds,
  remainingSeconds
})
</script>

<style scoped>
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
</style>