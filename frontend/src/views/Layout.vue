<template>
  <div class="layout-container">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2 class="logo">数据结构机考平台</h2>
        </div>
        <div class="header-right">
          <span class="username">{{ authStore.realName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              <el-icon><User /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="history">考试记录</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const handleCommand = async (command) => {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'history') {
    router.push('/history')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-container {
  height: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 24px;
}

.logo {
  margin: 0;
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  font-size: 14px;
  color: #666;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.main-content {
  background: #f5f5f5;
  padding: 24px;
}
</style>
