<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageLayout from '@/components/layout/PageLayout.vue'
import ProductCard from '@/components/ProductCard.vue'
import api from '@/lib/api'
import type { ProductSummary } from '@/types'

const route = useRoute()
const router = useRouter()

const categories = ref<string[]>([])
const products = ref<ProductSummary[]>([])
const query = ref((route.query.query as string) ?? '')
const category = ref((route.query.category as string) ?? '')
const featuredOnly = ref(route.query.featured === 'true')

const hasFilters = computed(() => Boolean(query.value || category.value || featuredOnly.value))

const loadCategories = async () => {
  const { data } = await api.get<string[]>('/catalog/categories')
  categories.value = data
}

const loadProducts = async () => {
  const { data } = await api.get<ProductSummary[]>('/catalog/products', {
    params: {
      query: query.value || undefined,
      category: category.value || undefined,
      featured: featuredOnly.value || undefined,
    },
  })
  products.value = data
}

const syncQuery = () => {
  void router.replace({
    query: {
      query: query.value || undefined,
      category: category.value || undefined,
      featured: featuredOnly.value ? 'true' : undefined,
    },
  })
}

watch([query, category, featuredOnly], async () => {
  syncQuery()
  await loadProducts()
})

onMounted(async () => {
  await Promise.all([loadCategories(), loadProducts()])
})
</script>

<template>
  <PageLayout>
    <section class="content-wrap py-12">
      <div class="organic-card bg-[#fffaf4]">
        <p class="text-sm tracking-[0.22em] text-stone-400">商品筛选</p>
        <div class="mt-4 flex flex-col gap-4 md:flex-row">
          <input v-model="query" class="organic-input md:flex-1" placeholder="搜索商品名、描述或农场关键词" />
          <select v-model="category" class="organic-input md:w-52">
            <option value="">全部分类</option>
            <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
          </select>
          <label class="flex items-center gap-3 rounded-full border border-stone-200 bg-white px-5 py-3 text-sm text-stone-600">
            <input v-model="featuredOnly" type="checkbox" class="h-4 w-4 rounded border-stone-300 text-primary focus:ring-stone-300" />
            仅看主推
          </label>
        </div>
      </div>
    </section>

    <section class="content-wrap pb-16">
      <div class="mb-6 flex items-center justify-between">
        <div>
          <p class="text-sm tracking-[0.22em] text-stone-400">查询结果</p>
          <h1 class="mt-2 font-serif text-4xl font-semibold text-primary">农场好物列表</h1>
        </div>
        <p class="text-sm text-stone-500">{{ products.length }} 件商品{{ hasFilters ? '已按筛选条件更新' : '' }}</p>
      </div>

      <div class="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>
    </section>
  </PageLayout>
</template>
