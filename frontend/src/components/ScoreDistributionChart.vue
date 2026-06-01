<!--
  教师端成绩分布柱状图（ECharts）
  - 入参 data: [{ range: '<60%', count: n }, ...]，由 TeacherDashboard.normalizeScoreDistribution 提供
  - 横轴：得分率区间；纵轴：该区间内学生人数
  - 切换考试或接口刷新时 watch data 重绘；组件卸载时 dispose 防止泄漏
-->
<template>
  <div ref="chartRef" class="score-distribution-chart" />
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  /** 固定 5 个得分率区间及人数，见 TeacherDashboard.SCORE_DISTRIBUTION_RANGES */
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

/** 根据桶数据生成 ECharts option（标题、tooltip、柱状 series） */
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

/** 初始化或更新图表；setOption 第二个参数 true 表示不与旧 option 合并 */
const renderChart = async () => {
  await nextTick()
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  chartInstance.setOption(buildOption(props.data), true)
  chartInstance.resize()
}

/** 离开页面时释放实例与 ResizeObserver */
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
