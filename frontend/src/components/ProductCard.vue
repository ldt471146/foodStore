<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { Star, Leaf } from 'lucide-vue-next'
import productPlaceholder from '@/assets/product-placeholder.svg'
import type { ProductSummary } from '@/types'

defineProps<{
  product: ProductSummary
}>()
</script>

<template>
  <RouterLink
    :to="`/product/${product.id}`"
    class="organic-card group flex h-full flex-col overflow-hidden bg-white/90 hover:-translate-y-1"
  >
    <div class="relative overflow-hidden rounded-[1.6rem]">
      <img
        :src="product.imageUrl || productPlaceholder"
        :alt="product.name"
        class="h-56 w-full object-cover transition-transform duration-500 group-hover:scale-105"
      />
      <span v-if="product.featured" class="organic-pill absolute left-4 top-4 bg-[#fff4df] text-[#8f5b27]">主推</span>
    </div>
    <div class="mt-5 flex flex-1 flex-col">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="text-xs uppercase tracking-[0.18em] text-stone-400">{{ product.category }}</p>
          <h3 class="mt-2 font-serif text-2xl font-semibold text-primary">{{ product.name }}</h3>
        </div>
        <div class="flex items-center gap-1 rounded-full bg-[#f5eee4] px-3 py-1 text-sm text-stone-500">
          <Star :size="14" class="fill-[#d4a373] text-[#d4a373]" />
          <span>{{ product.rating.toFixed(1) }}</span>
        </div>
      </div>
      <div class="mt-4 flex items-center gap-2 text-sm text-stone-500">
        <Leaf :size="16" class="text-accent-olive" />
        <span>{{ product.origin }} · {{ product.farmName }}</span>
      </div>
      <p class="mt-2 text-sm text-stone-400">发布人：{{ product.publisherName }}</p>
      <div class="mt-6 flex items-center justify-between">
        <p class="text-2xl font-semibold text-primary">¥{{ product.price }}</p>
        <span class="text-sm text-stone-400">库存 {{ product.stockQuantity }}</span>
      </div>
    </div>
  </RouterLink>
</template>
