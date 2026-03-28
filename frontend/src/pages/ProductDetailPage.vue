<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { PackagePlus, ShieldCheck, Truck, Star } from 'lucide-vue-next'
import PageLayout from '@/components/layout/PageLayout.vue'
import ProductCard from '@/components/ProductCard.vue'
import api from '@/lib/api'
import productPlaceholder from '@/assets/product-placeholder.svg'
import { formatDate } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { ProductDetail, ProductSummary, Review, ReviewEligibility } from '@/types'

const route = useRoute()
const authStore = useAuthStore()
const cartStore = useCartStore()

const product = ref<ProductDetail | null>(null)
const reviews = ref<Review[]>([])
const recommendations = ref<ProductSummary[]>([])
const reviewForm = ref({ rating: 5, content: '' })
const reviewMessage = ref('')
const reviewEligibility = ref<ReviewEligibility | null>(null)

const productId = computed(() => Number(route.params.id))

const loadData = async () => {
  const [productResponse, reviewResponse, recommendationResponse] = await Promise.all([
    api.get<ProductDetail>(`/catalog/products/${productId.value}`),
    api.get<Review[]>(`/catalog/products/${productId.value}/reviews`),
    api.get<ProductSummary[]>('/catalog/recommendations'),
  ])
  product.value = productResponse.data
  reviews.value = reviewResponse.data
  recommendations.value = recommendationResponse.data
    .filter((item: ProductSummary) => item.id !== productId.value)
    .slice(0, 4)

  if (authStore.isAuthenticated) {
    const [eligibilityResponse] = await Promise.all([
      api.get<ReviewEligibility>(`/catalog/products/${productId.value}/review-eligibility`),
      api.post(`/catalog/products/${productId.value}/browse`),
    ])
    reviewEligibility.value = eligibilityResponse.data
  } else {
    reviewEligibility.value = null
  }
}

const addToCart = async () => {
  if (!product.value) return
  await cartStore.addToCart(product.value.id, 1)
}

const submitReview = async () => {
  if (!reviewEligibility.value?.canReview) {
    reviewMessage.value = reviewEligibility.value?.message ?? '确认收货后才能评论。'
    return
  }
  reviewMessage.value = ''
  try {
    await api.post(`/catalog/products/${productId.value}/reviews`, {
      ...reviewForm.value,
      orderItemId: reviewEligibility.value.orderItemId,
    })
    reviewForm.value = { rating: 5, content: '' }
    reviewMessage.value = '评价已提交，你还可以继续追加评论。'
    await loadData()
  } catch (error: any) {
    reviewMessage.value = error?.response?.data?.message ?? '评价提交失败'
  }
}

onMounted(async () => {
  await loadData()
})

watch(productId, async () => {
  reviewMessage.value = ''
  reviewForm.value = { rating: 5, content: '' }
  await loadData()
})
</script>

<template>
  <PageLayout>
    <section v-if="product" class="content-wrap grid gap-10 py-12 md:grid-cols-[0.9fr_1.1fr]">
      <div class="organic-card overflow-hidden">
        <img
          :src="product.imageUrl || productPlaceholder"
          :alt="product.name"
          class="h-[420px] w-full rounded-[1.6rem] object-cover"
        />
      </div>

      <div class="organic-card bg-[#fffdf9]">
        <span class="organic-pill">{{ product.category }}</span>
        <h1 class="mt-4 font-serif text-5xl font-semibold text-primary">{{ product.name }}</h1>
        <div class="mt-5 flex flex-wrap items-center gap-4 text-sm text-stone-500">
          <span class="flex items-center gap-2 rounded-full bg-[#f5eee4] px-4 py-2">
            <Star :size="16" class="fill-[#d4a373] text-[#d4a373]" />
            {{ product.rating.toFixed(1) }}
          </span>
          <span>{{ product.origin }} · {{ product.farmName }}</span>
          <span>发布人 {{ product.publisherName }}</span>
          <span>库存 {{ product.stockQuantity }} {{ product.unit }}</span>
        </div>
        <p class="mt-6 text-base leading-8 text-stone-600">{{ product.description }}</p>
        <div class="mt-8 flex flex-wrap items-center gap-4">
          <p class="text-4xl font-semibold text-primary">¥{{ product.price }}</p>
          <button v-if="authStore.isAuthenticated" type="button" class="organic-button" @click="addToCart">
            <PackagePlus :size="18" class="mr-2" />
            加入购物车
          </button>
          <RouterLink v-else to="/login" class="organic-button">登录后购买</RouterLink>
        </div>
        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <div class="rounded-organic bg-[#f5eee4] p-4">
            <p class="text-xs uppercase tracking-[0.18em] text-stone-400">溯源码</p>
            <p class="mt-2 font-mono text-sm text-primary">{{ product.traceabilityCode }}</p>
          </div>
          <div class="rounded-organic bg-[#eef3ea] p-4">
            <p class="text-xs uppercase tracking-[0.18em] text-stone-400">种植日期</p>
            <p class="mt-2 text-sm text-stone-600">{{ formatDate(product.plantingDate) }}</p>
          </div>
          <div class="rounded-organic bg-[#eef7f5] p-4">
            <p class="text-xs uppercase tracking-[0.18em] text-stone-400">采收日期</p>
            <p class="mt-2 text-sm text-stone-600">{{ formatDate(product.harvestDate) }}</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="product" class="content-wrap grid gap-8 pb-10 lg:grid-cols-[0.8fr_1.2fr]">
      <div class="organic-card bg-white">
        <div class="flex items-center gap-3">
          <span class="rounded-full bg-[#eef3ea] p-3 text-accent-olive"><ShieldCheck :size="18" /></span>
          <div>
            <p class="font-semibold text-stone-700">透明溯源</p>
            <p class="text-sm text-stone-500">{{ product.certificate }}</p>
          </div>
        </div>
        <div class="mt-6 flex items-center gap-3">
          <span class="rounded-full bg-[#f5eee4] p-3 text-[#b1773a]"><Truck :size="18" /></span>
          <div>
            <p class="font-semibold text-stone-700">发货说明</p>
            <p class="text-sm text-stone-500">支付后进入加工打包流程，管理员可录入物流单号并同步状态。</p>
          </div>
        </div>
      </div>

      <div class="organic-card bg-white">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm tracking-[0.22em] text-stone-400">商品评价</p>
            <h2 class="mt-2 font-serif text-3xl font-semibold text-primary">用户评价</h2>
          </div>
          <span class="text-sm text-stone-500">{{ reviews.length }} 条</span>
        </div>

        <div class="mt-6 space-y-4">
          <div v-for="review in reviews" :key="review.id" class="rounded-organic bg-[#faf6f1] p-5">
            <div class="flex items-center justify-between">
              <p class="font-semibold text-stone-700">{{ review.userName }}</p>
              <p class="text-sm text-stone-400">{{ formatDate(review.createdAt) }}</p>
            </div>
            <p class="mt-1 text-sm text-stone-500">评分 {{ review.rating }}/5</p>
            <p class="mt-3 text-sm leading-7 text-stone-600">{{ review.content }}</p>
          </div>
        </div>

        <div v-if="authStore.isAuthenticated" class="mt-8 rounded-organic bg-[#fffaf4] p-5">
          <h3 class="font-serif text-2xl font-semibold text-primary">提交你的评价</h3>
          <p class="mt-3 text-sm text-stone-500">{{ reviewEligibility?.message ?? '确认收货后才能评论。' }}</p>
          <div class="mt-4 grid gap-4 md:grid-cols-[160px_1fr]">
            <select v-model="reviewForm.rating" class="organic-input">
              <option v-for="score in 5" :key="score" :value="score">{{ score }} 星</option>
            </select>
            <input
              v-model="reviewForm.content"
              class="organic-input"
              placeholder="写下口感、包装或新鲜度感受"
            />
          </div>
          <div class="mt-4 flex items-center gap-3">
            <button
              type="button"
              class="organic-button"
              :disabled="!reviewEligibility?.canReview || !reviewForm.content.trim()"
              @click="submitReview"
            >
              提交评价
            </button>
            <span class="text-sm text-accent-olive">{{ reviewMessage }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="content-wrap pb-16">
      <div class="flex items-end justify-between gap-4">
        <div>
          <p class="text-sm tracking-[0.22em] text-stone-400">相关推荐</p>
          <h2 class="mt-2 font-serif text-4xl font-semibold text-primary">你可能还会想看</h2>
        </div>
      </div>
      <div class="mt-8 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
        <ProductCard v-for="item in recommendations" :key="item.id" :product="item" />
      </div>
    </section>
  </PageLayout>
</template>
