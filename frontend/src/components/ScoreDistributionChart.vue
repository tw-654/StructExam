<template>
  <div ref="chartRef" class="score-distribution-chart" />
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const buildOption = (buckets) => {
  const list = Array.isArray(buckets) ? buckets : []
  const ranges = list.map(item => item.range)
  const counts = list.map(item => Number(item.count) || 0)
  const maxCount = counts.reduce((peak, value) => Math.max(peak, value), 0)

  return {
    title: {
      text: '成绩分布统计',
      left: 'center',
      top: 8,
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: '#303133'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const point = params?.[0]
        if (!point) return ''
        return `${point.axisValue}<br/>人数：${point.data} 人`
      }
    },
    grid: {
      left: 48,
      right: 24,
      bottom: 48,
      top: 52
    },
    xAxis: {
      type: 'category',
      data: ranges,
      axisTick: { alignWithLabel: true },
      axisLabel: {
        interval: 0,
        fontSize: 12,
        color: '#606266'
      }
    },
    yAxis: {
      type: 'value',
      name: '人数',
      minInterval: 1,
      max: maxCount === 0 ? 5 : undefined,
      axisLabel: { color: '#606266' },
      nameTextStyle: { color: '#909399' },
      splitLine: { lineStyle: { color: '#ebeef5' } }
    },
    series: [
      {
        type: 'bar',
        data: counts,
        barMaxWidth: 56,
        itemStyle: {
          color: '#409eff',
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: { color: '#79bbff' }
        }
      }
    ]
  }
}

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  chartInstance.setOption(buildOption(props.data), true)
  chartInstance.resize()
}

const disposeChart = () => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chartInstance?.dispose()
  chartInstance = null
}

onMounted(async () => {
  await renderChart()
  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => chartInstance?.resize())
    resizeObserver.observe(chartRef.value)
  }
})

onBeforeUnmount(() => {
  disposeChart()
})

watch(
  () => props.data,
  () => {
    renderChart()
  },
  { deep: true }
)
</script>

<style scoped>
.score-distribution-chart {
  width: 100%;
  height: 280px;
}
</style>
