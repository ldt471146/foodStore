<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import PageLayout from '@/components/layout/PageLayout.vue'
import { formatCurrency } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import api from '@/lib/api'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const form = reactive({
  recipientName: authStore.user?.fullName ?? '',
  recipientPhone: authStore.user?.phone ?? '',
  recipientAddress: authStore.user?.address ?? '',
  note: '',
})

const checkout = async () => {
  const { data } = await api.post('/orders/checkout', form)
  cartStore.clear()
  await router.push('/orders')
  if (data?.id) {
    await api.post(`/orders/${data.id}/pay`)
  }
}

onMounted(async () => {
  await cartStore.loadCart()
})
</script>

<template>
  <PageLayout>
    <section class="content-wrap py-12">
      <div class="flex items-end justify-between gap-4">
        <div>
          <p class="text-sm uppercase tracking-[0.22em] text-stone-400">Cart</p>
          <h1 class="mt-2 font-serif text-4xl font-semibold text-primary">购物车与收货信息</h1>
        </div>
        <p class="text-sm text-stone-500">共 {{ cartStore.itemCount }} 件商品</p>
      </div>

      <div v-if="cartStore.items.length" class="mt-8 grid gap-8 lg:grid-cols-[1.2fr_0.8fr]">
        <div class="space-y-4">
          <div v-for="item in cartStore.items" :key="item.id" class="organic-card flex flex-col gap-4 bg-white md:flex-row md:items-center">
            <img
              :src="item.imageUrl || 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=900&q=80'"
              :alt="item.productName"
              class="h-28 w-full rounded-[1.4rem] object-cover md:w-36"
            />
            <div class="flex-1">
              <h2 class="font-serif text-2xl font-semibold text-primary">{{ item.productName }}</h2>
              <p class="mt-2 text-sm text-stone-500">单价 {{ formatCurrency(item.unitPrice) }}</p>
            </div>
            <div class="flex items-center gap-3">
              <button type="button" class="organic-button--ghost rounded-full px-3 py-2 text-sm" @click="cartStore.updateQuantity(item.id, item.quantity - 1)">-</button>
              <span class="min-w-8 text-center text-sm font-semibold text-stone-700">{{ item.quantity }}</span>
              <button type="button" class="organic-button--ghost rounded-full px-3 py-2 text-sm" @click="cartStore.updateQuantity(item.id, item.quantity + 1)">+</button>
            </div>
            <div class="min-w-28 text-right">
              <p class="text-lg font-semibold text-primary">{{ formatCurrency(item.subtotal) }}</p>
              <button type="button" class="mt-2 text-sm text-stone-400 transition-colors duration-300 hover:text-primary" @click="cartStore.removeItem(item.id)">
                删除
              </button>
            </div>
          </div>
        </div>

        <div class="organic-card bg-[#fffaf4]">
          <h2 class="font-serif text-3xl font-semibold text-primary">提交订单</h2>
          <div class="mt-5 space-y-4">
            <input v-model="form.recipientName" class="organic-input" placeholder="收货人姓名" />
            <input v-model="form.recipientPhone" class="organic-input" placeholder="联系电话" />
            <input v-model="form.recipientAddress" class="organic-input" placeholder="收货地址" />
            <input v-model="form.note" class="organic-input" placeholder="备注，如配送时间或包装要求" />
          </div>

          <div class="mt-6 rounded-organic bg-white p-5">
            <div class="flex items-center justify-between text-sm text-stone-500">
              <span>商品数</span>
              <span>{{ cartStore.itemCount }}</span>
            </div>
            <div class="mt-3 flex items-center justify-between text-lg font-semibold text-primary">
              <span>应付合计</span>
              <span>{{ formatCurrency(cartStore.totalAmount) }}</span>
            </div>
          </div>

          <button type="button" class="organic-button mt-6 w-full" @click="checkout">提交订单并模拟支付</button>
        </div>
      </div>

      <div v-else class="organic-card mt-8 bg-white text-center">
        <p class="font-serif text-3xl font-semibold text-primary">购物车还是空的</p>
        <p class="mt-3 text-sm text-stone-500">先去挑一件喜欢的农场好物，再回来结算。</p>
        <RouterLink to="/catalog" class="organic-button mt-6">去逛一逛</RouterLink>
      </div>
    </section>
  </PageLayout>
</template>
