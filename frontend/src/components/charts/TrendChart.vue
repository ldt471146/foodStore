<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  labels: string[]
  values: number[]
  compareLabels?: string[]
  compareValues?: number[]
}>()

let chart: echarts.ECharts | null = null
const chartId = `trend-${Math.random().toString(36).slice(2)}`

const renderChart = async () => {
  await nextTick()
  const el = document.getElementById(chartId)
  if (!el) return
  if (!chart) {
    chart = echarts.init(el)
  }

  const labels = [...props.labels, ...(props.compareLabels ?? [])]
  chart.setOption({
    grid: {
      top: 24,
      right: 16,
      bottom: 24,
      left: 24,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fffaf4',
      borderColor: '#e7decf',
      textStyle: { color: '#5c4033' },
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#d6ccc2' } },
      axisLabel: { color: '#78716c' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#efe8dd' } },
      axisLabel: { color: '#78716c' },
    },
    series: [
      {
        name: '历史销量',
        type: 'line',
        smooth: true,
        data: props.values,
        lineStyle: { color: '#5c4033', width: 3 },
        itemStyle: { color: '#5c4033' },
        areaStyle: { color: 'rgba(92,64,51,0.08)' },
      },
      ...(props.compareValues?.length
        ? [
            {
              name: '预测销量',
              type: 'line',
              smooth: true,
              data: [...Array(props.values.length).fill(null), ...props.compareValues],
              lineStyle: { color: '#8b9d77', width: 3, type: 'dashed' },
              itemStyle: { color: '#8b9d77' },
            },
          ]
        : []),
    ],
  })
}

onMounted(() => {
  void renderChart()
  window.addEventListener('resize', handleResize)
})

watch(
  () => [props.labels, props.values, props.compareLabels, props.compareValues],
  () => {
    void renderChart()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})

const handleResize = () => {
  chart?.resize()
}
</script>

<template>
  <div :id="chartId" class="h-80 w-full"></div>
</template>
