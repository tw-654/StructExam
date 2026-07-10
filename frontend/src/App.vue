<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { preloadMonaco } from '@/utils/monacoPreloader'

onMounted(() => {
  preloadMonaco()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
