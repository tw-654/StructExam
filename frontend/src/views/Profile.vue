<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>

      <div class="profile-info" v-loading="loading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ userInfo.realName }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="userInfo.role === 'TEACHER' ? 'success' : 'primary'">
              {{ userInfo.role === 'TEACHER' ? '教师' : '学生' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'">
              {{ userInfo.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatDate(userInfo.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>

      <el-form ref="passwordFormRef" :model="passwordForm" :rules="rules" label-width="100px" style="max-width: 400px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdatePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()

const loading = ref(false)
const userInfo = ref({})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('Two passwords do not match'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: 'Please enter old password', trigger: 'blur' }],
  newPassword: [
    { required: true, message: 'Please enter new password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const passwordFormRef = ref(null)

const loadUserInfo = async () => {
  loading.value = true
  try {
    userInfo.value = await authStore.fetchUserInfo()
  } catch (error) {
    console.error('Failed to load user info:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await authStore.updatePassword(passwordForm.oldPassword, passwordForm.newPassword)
        ElMessage.success('Password updated successfully')
        passwordFormRef.value.resetFields()
      } catch (error) {
        console.error('Failed to update password:', error)
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  font-weight: 500;
  font-size: 16px;
}
</style>
