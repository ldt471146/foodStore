<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PageLayout from '@/components/layout/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import orchardScene from '@/assets/china-orchard-login.svg'

const authStore = useAuthStore()
const cartStore = useCartStore()
const route = useRoute()
const router = useRouter()

const form = reactive({
  username: 'consumer',
  password: '123456',
})
const errorMessage = ref('')

const submit = async () => {
  errorMessage.value = ''
  try {
    await authStore.login(form.username, form.password)
    await cartStore.loadCart()
    await router.push((route.query.redirect as string) || authStore.defaultRoute)
  } catch {
    errorMessage.value = '登录失败，请检查用户名和密码。'
  }
}
</script>

<template>
  <PageLayout :show-chrome="false">
    <section class="relative min-h-screen overflow-hidden bg-[#f6efe2]">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(255,249,240,0.92),rgba(246,239,226,0.78)_44%,rgba(228,214,188,0.7)_100%)]" />
      <div
        class="absolute inset-0 bg-cover bg-center opacity-95"
        :style="{ backgroundImage: `url(${orchardScene})` }"
      />
      <div class="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,248,238,0.82),rgba(255,248,238,0.3)_36%,rgba(92,64,51,0.18)_100%)]" />

      <div class="content-wrap relative z-10 grid min-h-screen items-center gap-10 py-10 lg:grid-cols-[1.08fr_0.92fr]">
        <div class="max-w-2xl text-primary">
          <span class="organic-pill bg-white/80 text-[#6c7a58]">中国果园 · 登录后进入系统</span>
          <h1 class="mt-6 font-serif text-5xl font-semibold leading-tight md:text-7xl">
            先登录，再进入农场品商城与后台系统。
          </h1>
          <div class="mt-8 grid max-w-2xl gap-4 md:grid-cols-3">
            <div class="rounded-[2rem] border border-white/60 bg-white/72 p-4 backdrop-blur">
              <p class="text-xs uppercase tracking-[0.2em] text-stone-400">消费者</p>
              <p class="mt-2 text-sm leading-6 text-stone-600">浏览、下单、评价、查物流。</p>
            </div>
            <div class="rounded-[2rem] border border-white/60 bg-white/72 p-4 backdrop-blur">
              <p class="text-xs uppercase tracking-[0.2em] text-stone-400">农场管理员</p>
              <p class="mt-2 text-sm leading-6 text-stone-600">商品、库存、订单、物流。</p>
            </div>
            <div class="rounded-[2rem] border border-white/60 bg-white/72 p-4 backdrop-blur">
              <p class="text-xs uppercase tracking-[0.2em] text-stone-400">平台管理员</p>
              <p class="mt-2 text-sm leading-6 text-stone-600">客户、预测、分析、预警。</p>
            </div>
          </div>
        </div>

        <div class="mx-auto w-full max-w-xl rounded-[2.4rem] border border-white/70 bg-[rgba(255,252,247,0.88)] p-8 shadow-[0_24px_80px_rgba(92,64,51,0.15)] backdrop-blur-xl md:p-10">
          <p class="text-sm tracking-[0.24em] text-stone-400">登录入口</p>
          <h2 class="mt-3 font-serif text-4xl font-semibold text-primary">登录体验系统</h2>
          <p class="mt-3 text-sm leading-7 text-stone-500">`consumer / 123456`、`farmadmin / 123456`、`platform / 123456`</p>

          <div class="mt-8 space-y-4">
            <input v-model="form.username" class="organic-input bg-white/90" placeholder="用户名" />
            <input v-model="form.password" type="password" class="organic-input bg-white/90" placeholder="密码" />
          </div>

          <p class="mt-4 min-h-5 text-sm text-rose-500">{{ errorMessage }}</p>

          <div class="mt-4 flex flex-wrap gap-4">
            <button type="button" class="organic-button" @click="submit">进入系统</button>
            <RouterLink to="/forgot-password" class="organic-button organic-button--ghost">找回密码</RouterLink>
            <RouterLink to="/register" class="organic-button organic-button--ghost">注册账号</RouterLink>
          </div>

          <p class="mt-6 text-sm text-stone-500">未登录不可进入系统内容。</p>
        </div>
      </div>
    </section>
  </PageLayout>
</template>
