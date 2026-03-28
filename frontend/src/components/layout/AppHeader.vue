<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ShoppingBag, Sprout, LayoutDashboard, LogOut } from 'lucide-vue-next'
import { formatRole } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const authStore = useAuthStore()
const cartStore = useCartStore()
const router = useRouter()

const initials = computed(() => authStore.user?.fullName.slice(0, 1) ?? '游')

const logout = () => {
  authStore.logout()
  cartStore.clear()
  void router.push('/login')
}
</script>

<template>
  <header class="sticky top-0 z-30 border-b border-stone-200 bg-[#fffaf4]/90 backdrop-blur">
    <div class="content-wrap flex items-center justify-between gap-4 py-4">
      <RouterLink :to="authStore.defaultRoute" class="flex items-center gap-3">
        <span class="flex h-11 w-11 items-center justify-center rounded-full bg-[#ece3d5] text-primary">
          <Sprout :size="20" />
        </span>
        <div>
          <p class="font-serif text-2xl font-semibold text-primary">周记农场</p>
          <p class="text-xs tracking-[0.24em] text-stone-400">农场品在线商城</p>
        </div>
      </RouterLink>

      <nav class="hidden items-center gap-6 text-sm font-medium text-stone-600 md:flex">
        <RouterLink to="/home" class="transition-colors duration-300 hover:text-primary">首页</RouterLink>
        <RouterLink to="/catalog" class="transition-colors duration-300 hover:text-primary">农场好物</RouterLink>
        <RouterLink v-if="authStore.isAuthenticated" to="/orders" class="transition-colors duration-300 hover:text-primary">我的订单</RouterLink>
        <RouterLink v-if="authStore.isAdmin" to="/admin" class="transition-colors duration-300 hover:text-primary">管理后台</RouterLink>
      </nav>

      <div class="flex items-center gap-3">
        <RouterLink
          to="/cart"
          class="relative inline-flex h-11 w-11 items-center justify-center rounded-full border border-stone-200 bg-white text-stone-700 transition-colors duration-300 hover:bg-[#f6efe5]"
        >
          <ShoppingBag :size="18" />
          <span
            v-if="cartStore.itemCount"
            class="absolute -right-1 -top-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-accent-olive px-1 text-[10px] font-semibold text-white"
          >
            {{ cartStore.itemCount }}
          </span>
        </RouterLink>

        <template v-if="authStore.isAuthenticated && authStore.user">
          <RouterLink
            v-if="authStore.isAdmin"
            to="/admin"
            class="hidden h-11 items-center gap-2 rounded-full border border-stone-200 bg-white px-4 text-sm font-medium text-stone-700 transition-colors duration-300 hover:bg-[#f6efe5] md:inline-flex"
          >
            <LayoutDashboard :size="16" />
            管理台
          </RouterLink>
          <RouterLink to="/profile" class="hidden items-center gap-3 rounded-full border border-stone-200 bg-white px-4 py-2 md:flex">
            <img
              v-if="authStore.user.avatarImageUrl"
              :src="authStore.user.avatarImageUrl"
              alt="头像"
              class="h-9 w-9 rounded-full object-cover"
            />
            <span
              v-else
              class="flex h-9 w-9 items-center justify-center rounded-full text-sm font-semibold text-white"
              :style="{ backgroundColor: authStore.user.avatarColor ?? '#8b9d77' }"
            >
              {{ initials }}
            </span>
            <div>
              <p class="text-sm font-semibold text-stone-700">{{ authStore.user.fullName }}</p>
              <p class="text-xs text-stone-400">{{ formatRole(authStore.user.role) }}</p>
            </div>
          </RouterLink>
          <RouterLink
            to="/profile"
            class="flex h-11 w-11 items-center justify-center rounded-full border border-stone-200 bg-white md:hidden"
          >
            <img
              v-if="authStore.user.avatarImageUrl"
              :src="authStore.user.avatarImageUrl"
              alt="头像"
              class="h-9 w-9 rounded-full object-cover"
            />
            <span
              v-else
              class="flex h-9 w-9 items-center justify-center rounded-full text-sm font-semibold text-white"
              :style="{ backgroundColor: authStore.user.avatarColor ?? '#8b9d77' }"
            >
              {{ initials }}
            </span>
          </RouterLink>
          <div class="hidden md:flex">
            <button type="button" class="text-stone-400 transition-colors duration-300 hover:text-primary" @click="logout">
              <LogOut :size="16" />
            </button>
          </div>
        </template>

        <template v-else>
          <RouterLink to="/login" class="organic-button--ghost inline-flex items-center rounded-full px-4 py-2 text-sm font-semibold">
            登录
          </RouterLink>
          <RouterLink to="/register" class="organic-button hidden md:inline-flex">注册</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
