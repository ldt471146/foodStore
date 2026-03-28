<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageLayout from '@/components/layout/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '123456',
  fullName: '',
  email: '',
  phone: '',
  address: '',
  avatarColor: '#8b9d77',
  role: 'CONSUMER' as 'CONSUMER' | 'FARM_ADMIN',
})
const errorMessage = ref('')

const submit = async () => {
  errorMessage.value = ''
  try {
    await authStore.register(form)
    await router.push(authStore.defaultRoute)
  } catch {
    errorMessage.value = '注册失败，请检查是否有重复用户名或邮箱。'
  }
}
</script>

<template>
  <PageLayout :show-chrome="false">
    <section class="content-wrap py-16">
      <div class="mx-auto max-w-2xl organic-card bg-white">
        <p class="text-sm tracking-[0.22em] text-stone-400">注册账号</p>
        <h1 class="mt-3 font-serif text-4xl font-semibold text-primary">创建你的体验账号</h1>
        <div class="mt-8 grid gap-4 md:grid-cols-2">
          <input v-model="form.fullName" class="organic-input" placeholder="姓名" />
          <input v-model="form.username" class="organic-input" placeholder="用户名" />
          <input v-model="form.email" class="organic-input" placeholder="邮箱" />
          <input v-model="form.phone" class="organic-input" placeholder="手机号" />
          <input v-model="form.address" class="organic-input md:col-span-2" placeholder="地址" />
          <select v-model="form.role" class="organic-input">
            <option value="CONSUMER">消费者</option>
            <option value="FARM_ADMIN">农场管理员</option>
          </select>
          <input v-model="form.password" type="password" class="organic-input" placeholder="密码" />
        </div>
        <p class="mt-4 text-sm text-rose-500">{{ errorMessage }}</p>
        <button type="button" class="organic-button mt-6" @click="submit">提交注册</button>
      </div>
    </section>
  </PageLayout>
</template>
