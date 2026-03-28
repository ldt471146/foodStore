<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageLayout from '@/components/layout/PageLayout.vue'
import api from '@/lib/api'
import { formatCurrency, formatDateTime, formatOrderStatus, formatPaymentStatus } from '@/lib/format'
import type { Order } from '@/types'

const orders = ref<Order[]>([])
const reviewDrafts = reactive<Record<number, { rating: number; content: string; message: string; saving: boolean }>>({})
const receiptMessages = reactive<Record<number, string>>({})
const confirmingOrderId = ref<number | null>(null)

const loadOrders = async () => {
  const { data } = await api.get<Order[]>('/orders')
  orders.value = data
  for (const order of data) {
    for (const item of order.items) {
      if (!reviewDrafts[item.productId]) {
        reviewDrafts[item.productId] = { rating: 5, content: '', message: '', saving: false }
      }
    }
  }
}

const payOrder = async (orderId: number) => {
  await api.post(`/orders/${orderId}/pay`)
  await loadOrders()
}

const confirmReceipt = async (orderId: number) => {
  confirmingOrderId.value = orderId
  receiptMessages[orderId] = ''
  try {
    await api.post(`/orders/${orderId}/confirm-receipt`)
    await loadOrders()
    receiptMessages[orderId] = '已确认收货，现在可以对商品进行评价。'
  } catch (error: any) {
    receiptMessages[orderId] = error?.response?.data?.message ?? '确认收货失败'
  } finally {
    confirmingOrderId.value = null
  }
}

const submitReview = async (productId: number) => {
  const draft = reviewDrafts[productId]
  if (!draft) return
  const targetItem = orders.value
    .flatMap((order) => order.items)
    .find((item) => item.productId === productId)
  if (!targetItem?.canReview) {
    draft.message = targetItem?.reviewMessage ?? '确认收货后才能评论'
    return
  }
  draft.saving = true
  draft.message = ''
  try {
    await api.post(`/catalog/products/${productId}/reviews`, {
      orderItemId: targetItem.id,
      rating: draft.rating,
      content: draft.content,
    })
    draft.content = ''
    draft.rating = 5
    draft.message = '评价已提交，你还可以继续追加评论。'
    await loadOrders()
  } catch (error: any) {
    draft.message = error?.response?.data?.message ?? '评价提交失败'
  } finally {
    draft.saving = false
  }
}

onMounted(async () => {
  await loadOrders()
})
</script>

<template>
  <PageLayout>
    <section class="content-wrap py-12">
      <div class="flex items-end justify-between gap-4">
        <div>
          <p class="text-sm tracking-[0.22em] text-stone-400">订单中心</p>
          <h1 class="mt-2 font-serif text-4xl font-semibold text-primary">我的订单与物流进度</h1>
        </div>
        <p class="text-sm text-stone-500">{{ orders.length }} 条订单记录</p>
      </div>

      <div class="mt-8 space-y-6">
        <div v-for="order in orders" :key="order.id" class="organic-card bg-white">
          <div class="flex flex-col gap-4 border-b border-stone-100 pb-5 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-xs uppercase tracking-[0.18em] text-stone-400">订单号 {{ order.code }}</p>
              <h2 class="mt-2 font-serif text-3xl font-semibold text-primary">{{ formatCurrency(order.totalAmount) }}</h2>
            </div>
            <div class="flex flex-wrap items-center gap-3 text-sm">
              <span class="rounded-full bg-[#f5eee4] px-4 py-2 text-stone-600">状态 {{ formatOrderStatus(order.status) }}</span>
              <span class="rounded-full bg-[#eef3ea] px-4 py-2 text-accent-olive">支付 {{ formatPaymentStatus(order.paymentStatus) }}</span>
              <button
                v-if="order.paymentStatus === 'UNPAID'"
                type="button"
                class="organic-button"
                @click="payOrder(order.id)"
              >
                立即支付
              </button>
            </div>
          </div>

          <div class="mt-5 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
            <div>
              <p class="text-sm font-semibold text-stone-600">商品明细</p>
              <div class="mt-3 space-y-3">
                <div v-for="item in order.items" :key="item.id" class="rounded-organic bg-[#faf6f1] p-4">
                  <div class="flex items-center justify-between gap-4">
                    <div>
                      <p class="font-semibold text-stone-700">{{ item.productName }}</p>
                      <p class="mt-1 text-sm text-stone-500">
                        {{ item.quantity }} 件 · 单价 {{ formatCurrency(item.unitPrice) }} · 发布人 {{ item.publisherName }}
                      </p>
                    </div>
                    <p class="text-sm font-semibold text-primary">{{ formatCurrency(item.subtotal) }}</p>
                  </div>
                  <div class="mt-4 rounded-[1.4rem] bg-white p-4">
                    <p class="text-sm font-semibold text-stone-600">商品评价</p>
                    <p class="mt-2 text-sm text-stone-500">{{ item.reviewMessage }}</p>
                    <div class="mt-3 grid gap-3 md:grid-cols-[120px_1fr_auto]">
                      <select v-model="reviewDrafts[item.productId].rating" class="organic-input">
                        <option v-for="score in 5" :key="score" :value="score">{{ score }} 星</option>
                      </select>
                      <input
                        v-model="reviewDrafts[item.productId].content"
                        class="organic-input"
                        placeholder="写下这件商品的评价"
                      />
                      <button
                        type="button"
                        class="organic-button"
                        :disabled="reviewDrafts[item.productId].saving || (item.canReview && !reviewDrafts[item.productId].content.trim())"
                        @click="submitReview(item.productId)"
                      >
                        {{ reviewDrafts[item.productId].saving ? '提交中...' : (item.canReview ? '提交评价' : '暂不可评价') }}
                      </button>
                    </div>
                    <p v-if="reviewDrafts[item.productId]?.message" class="mt-2 text-sm text-accent-olive">
                      {{ reviewDrafts[item.productId].message }}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div class="rounded-organic bg-[#fffaf4] p-5">
              <p class="text-sm font-semibold text-stone-600">配送与时间线</p>
              <ul class="mt-4 space-y-3 text-sm text-stone-500">
                <li>创建时间：{{ formatDateTime(order.createdAt) }}</li>
                <li>支付时间：{{ formatDateTime(order.paidAt) }}</li>
                <li>发货时间：{{ formatDateTime(order.shippedAt) }}</li>
                <li>送达时间：{{ formatDateTime(order.deliveredAt) }}</li>
                <li>确认收货时间：{{ formatDateTime(order.receivedAt) }}</li>
                <li>物流公司：{{ order.logisticsCompany || '待录入' }}</li>
                <li>物流单号：{{ order.trackingNumber || '待录入' }}</li>
              </ul>
              <button
                v-if="order.status === 'DELIVERED'"
                type="button"
                class="organic-button mt-5"
                :disabled="confirmingOrderId === order.id"
                @click="confirmReceipt(order.id)"
              >
                {{ confirmingOrderId === order.id ? '确认中...' : '确认收货' }}
              </button>
              <p v-if="receiptMessages[order.id]" class="mt-3 text-sm text-accent-olive">
                {{ receiptMessages[order.id] }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </PageLayout>
</template>
