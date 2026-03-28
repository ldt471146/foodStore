<script setup lang="ts">
import { reactive, ref } from 'vue'
import PageLayout from '@/components/layout/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const form = reactive({
  email: 'consumer@farmshop.local',
  newPassword: '123456',
})
const message = ref('')

const submit = async () => {
  await authStore.forgotPassword(form.email, form.newPassword)
  message.value = '密码已更新，现在可以回到登录页使用新密码。'
}
</script>

<template>
  <PageLayout :show-chrome="false">
    <section class="content-wrap py-16">
      <div class="mx-auto max-w-xl organic-card bg-white">
        <p class="text-sm tracking-[0.22em] text-stone-400">密码重置</p>
        <h1 class="mt-3 font-serif text-4xl font-semibold text-primary">找回密码</h1>
        <p class="mt-3 text-sm leading-7 text-stone-500">演示版本通过邮箱和新密码直接重置，后端会用 BCrypt 重新加密保存。</p>
        <div class="mt-8 space-y-4">
          <input v-model="form.email" class="organic-input" placeholder="注册邮箱" />
          <input v-model="form.newPassword" type="password" class="organic-input" placeholder="新密码" />
        </div>
        <button type="button" class="organic-button mt-6" @click="submit">确认重置</button>
        <p class="mt-4 text-sm text-accent-olive">{{ message }}</p>
      </div>
    </section>
  </PageLayout>
</template>
