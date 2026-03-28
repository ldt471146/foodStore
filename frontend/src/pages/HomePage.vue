<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, Leaf, PackageCheck, ChartLine, ShieldCheck } from 'lucide-vue-next'
import PageLayout from '@/components/layout/PageLayout.vue'
import ProductCard from '@/components/ProductCard.vue'
import api from '@/lib/api'
import { useAuthStore } from '@/stores/auth'
import type { ProductSummary } from '@/types'

type HomePayload = {
  categories: string[]
  featuredProducts: ProductSummary[]
  editorChoice: ProductSummary[]
}

const authStore = useAuthStore()

const home = ref<HomePayload | null>(null)
const recommendations = ref<ProductSummary[]>([])
const homeLoadError = ref('')
const recommendationLoadError = ref('')

const loadHome = async () => {
  const { data } = await api.get<HomePayload>('/catalog/home')
  home.value = data
}

const loadRecommendations = async () => {
  const { data } = await api.get<ProductSummary[]>('/catalog/recommendations')
  recommendations.value = data
}

const featuredProducts = computed(() => {
  if (!home.value) return []
  if (home.value.featuredProducts.length > 0) {
    return home.value.featuredProducts
  }
  return home.value.editorChoice.slice(0, 4)
})

const showingFeaturedFallback = computed(
  () =>
    Boolean(
      home.value &&
        home.value.featuredProducts.length === 0 &&
        featuredProducts.value.length > 0,
    ),
)

onMounted(async () => {
  const [homeResult, recommendationResult] = await Promise.allSettled([
    loadHome(),
    loadRecommendations(),
  ])

  if (homeResult.status === 'rejected') {
    homeLoadError.value = '首页数据加载失败，请确认后端服务和数据库初始化是否完成。'
  }

  if (recommendationResult.status === 'rejected') {
    recommendationLoadError.value = '推荐内容暂时加载失败，不影响其他内容浏览。'
  }
})
</script>

<template>
  <PageLayout>
    <section class="content-wrap grid gap-10 py-12 md:grid-cols-[1.2fr_0.8fr] md:py-16">
      <div class="organic-card overflow-hidden bg-[#fff8ef]">
        <span class="organic-pill bg-[#edf1e7] text-accent-olive">登录后首页 · 自然有机业务总览</span>
        <h1 class="mt-6 max-w-3xl font-serif text-5xl font-semibold leading-tight text-primary md:text-6xl">
          {{ authStore.isAdmin ? '欢迎进入农场运营系统。' : '欢迎进入你的农场品商城。' }}
        </h1>
        <p class="mt-5 max-w-2xl text-base leading-8 text-stone-600">{{ authStore.isAdmin ? '商品、库存、订单、客户与预测。' : '浏览、下单、评价、物流一体完成。' }}</p>
        <div class="mt-8 flex flex-wrap gap-4">
          <RouterLink to="/catalog" class="organic-button">
            进入内容区
            <ArrowRight :size="16" class="ml-2" />
          </RouterLink>
          <RouterLink v-if="authStore.isAdmin" to="/admin" class="organic-button organic-button--ghost">
            查看管理台
          </RouterLink>
          <RouterLink v-else to="/orders" class="organic-button organic-button--ghost">
            查看我的订单
          </RouterLink>
        </div>
      </div>

      <div class="grid gap-4">
        <div class="organic-card bg-white">
          <p class="text-sm uppercase tracking-[0.22em] text-stone-400">系统亮点</p>
          <div class="mt-6 grid gap-4">
            <div class="flex items-start gap-4">
              <span class="rounded-full bg-[#eef3ea] p-3 text-accent-olive"><Leaf :size="18" /></span>
              <div>
                <p class="font-semibold text-stone-700">商品溯源清晰</p>
                <p class="mt-1 text-sm leading-6 text-stone-500">产地、农场、种植时间、认证信息一屏可见。</p>
              </div>
            </div>
            <div class="flex items-start gap-4">
              <span class="rounded-full bg-[#f4ece1] p-3 text-[#b1773a]"><PackageCheck :size="18" /></span>
              <div>
                <p class="font-semibold text-stone-700">库存动态预警</p>
                <p class="mt-1 text-sm leading-6 text-stone-500">后台支持入库、出库、调整记录和低库存提示。</p>
              </div>
            </div>
            <div class="flex items-start gap-4">
              <span class="rounded-full bg-[#eef7f5] p-3 text-accent-teal"><ChartLine :size="18" /></span>
              <div>
                <p class="font-semibold text-stone-700">推荐与销量趋势</p>
                <p class="mt-1 text-sm leading-6 text-stone-500">根据浏览与购买记录生成推荐，并展示未来销量预测。</p>
              </div>
            </div>
          </div>
        </div>
        <div class="organic-card bg-[#f3ece0]">
          <div class="flex items-center gap-3">
            <span class="rounded-full bg-white p-3 text-primary"><ShieldCheck :size="18" /></span>
            <div>
              <p class="font-semibold text-stone-700">即开即演示</p>
              <p class="mt-1 text-sm text-stone-500">在 IDEA 中本地启动。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="content-wrap py-4">
      <div class="organic-card bg-white">
        <p class="text-sm uppercase tracking-[0.22em] text-stone-400">分类速览</p>
        <div class="mt-4 flex flex-wrap gap-3">
          <RouterLink
            v-for="category in home?.categories ?? []"
            :key="category"
            :to="`/catalog?category=${encodeURIComponent(category)}`"
            class="rounded-full border border-stone-200 bg-[#faf6f1] px-4 py-2 text-sm text-stone-600 transition-colors duration-300 hover:bg-[#f0e8db] hover:text-primary"
          >
            {{ category }}
          </RouterLink>
        </div>
        <p v-if="homeLoadError" class="mt-4 text-sm text-[#b45309]">{{ homeLoadError }}</p>
        <p
          v-else-if="(home?.categories?.length ?? 0) === 0"
          class="mt-4 text-sm leading-7 text-stone-500"
        >
          当前还没有商品分类，先执行初始化 SQL 或在后台发布商品后再查看。
        </p>
      </div>
    </section>

    <section class="content-wrap py-12">
      <div class="flex items-end justify-between gap-4">
        <div>
          <p class="text-sm tracking-[0.22em] text-stone-400">主推商品</p>
          <h2 class="mt-2 font-serif text-4xl font-semibold text-primary">主推农场好物</h2>
          <p v-if="showingFeaturedFallback" class="mt-3 text-sm leading-7 text-stone-500">
            当前还没有单独设置主推商品，已为你展示已上架的优先商品。
          </p>
        </div>
        <RouterLink to="/catalog" class="text-sm font-semibold text-primary">查看全部</RouterLink>
      </div>
      <div class="mt-8 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
        <ProductCard v-for="product in featuredProducts" :key="product.id" :product="product" />
      </div>
      <p
        v-if="!homeLoadError && featuredProducts.length === 0"
        class="mt-6 rounded-[1.5rem] bg-white/80 px-5 py-4 text-sm leading-7 text-stone-500"
      >
        当前还没有可展示的主推商品，请先执行数据库初始化脚本，或在后台新增商品后再查看首页。
      </p>
    </section>

    <section class="content-wrap py-12">
      <div class="flex items-end justify-between gap-4">
        <div>
          <p class="text-sm tracking-[0.22em] text-stone-400">智能推荐</p>
          <h2 class="mt-2 font-serif text-4xl font-semibold text-primary">根据行为生成的推荐</h2>
        </div>
        <span class="text-sm text-stone-500">已结合当前账号记录</span>
      </div>
      <div class="mt-8 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
        <ProductCard v-for="product in recommendations" :key="product.id" :product="product" />
      </div>
      <p v-if="recommendationLoadError" class="mt-6 text-sm text-[#b45309]">
        {{ recommendationLoadError }}
      </p>
      <p
        v-else-if="recommendations.length === 0"
        class="mt-6 rounded-[1.5rem] bg-white/80 px-5 py-4 text-sm leading-7 text-stone-500"
      >
        当前还没有推荐结果。浏览商品或完成下单后，这里会逐步生成更贴近你的推荐内容。
      </p>
    </section>
  </PageLayout>
</template>
