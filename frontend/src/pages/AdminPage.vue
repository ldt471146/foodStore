<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import PageLayout from '@/components/layout/PageLayout.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import api from '@/lib/api'
import { formatCurrency, formatDate, formatDateTime, formatOrderStatus, formatPaymentStatus } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'
import type { CustomerProfile, DashboardData, ForecastData, InventoryMovement } from '@/types'

type AdminProduct = {
  id: number
  name: string
  category: string
  price: number
  stockQuantity: number
  unit: string
  description: string
  imageUrl: string | null
  publisherName: string
  farmName: string | null
  origin: string | null
  certificate: string | null
  traceabilityCode: string | null
  plantingDate: string | null
  harvestDate: string | null
  organic: boolean
  featured: boolean
  rating: number
  lowStockThreshold: number
}

type ProductReview = {
  productId: number
  productName: string
  reviewId: number
  customerName: string
  rating: number
  content: string
  createdAt: string
}

type AdminOrder = {
  id: number
  code: string
  customerName: string
  username: string
  status: 'PENDING_PAYMENT' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'RECEIVED' | 'CANCELLED'
  paymentStatus: 'UNPAID' | 'PAID'
  totalAmount: number
  recipientName: string
  recipientPhone: string
  recipientAddress: string
  logisticsCompany: string | null
  trackingNumber: string | null
  createdAt: string
  receivedAt: string | null
  items: Array<{ productName: string; quantity: number; unitPrice: number; subtotal: number }>
}

const authStore = useAuthStore()

const activeTab = ref<'overview' | 'products' | 'inventory' | 'inventory-report' | 'orders' | 'customers' | 'forecast'>('overview')
const tabs = computed(() => {
  if (authStore.isPlatformAdmin) {
    return ['overview', 'inventory-report', 'orders', 'customers', 'forecast'] as const
  }
  return ['overview', 'products', 'inventory', 'inventory-report', 'orders'] as const
})

const tabLabels: Record<(typeof activeTab.value), string> = {
  overview: '总览',
  products: '商品',
  inventory: '库存',
  'inventory-report': '库存报表',
  orders: '订单',
  customers: '客户',
  forecast: '预测',
}

watch(
  tabs,
  (nextTabs) => {
    if (!(nextTabs as readonly string[]).includes(activeTab.value)) {
      activeTab.value = nextTabs[0]
    }
  },
  { immediate: true },
)

const overview = ref<DashboardData | null>(null)
const products = ref<AdminProduct[]>([])
const inventory = ref<InventoryMovement[]>([])
const orders = ref<AdminOrder[]>([])
const customers = ref<CustomerProfile[]>([])
const forecast = ref<ForecastData | null>(null)
const productReviews = ref<ProductReview[]>([])

const editingProductId = ref<number | null>(null)
const productImageFile = ref<File | null>(null)
const productImagePreview = ref<string>('')
const productForm = reactive({
  name: '',
  category: '蔬菜',
  price: 58,
  stockQuantity: 28,
  unit: '份',
  description: '',
  farmName: '',
  origin: '',
  certificate: '绿色农场认证',
  traceabilityCode: '',
  plantingDate: '',
  harvestDate: '',
  organic: true,
  featured: false,
  lowStockThreshold: 20,
})

const inventoryForm = reactive({
  productId: 0,
  type: 'INBOUND' as 'INBOUND' | 'OUTBOUND' | 'ADJUSTMENT',
  quantity: 10,
  source: '',
  remark: '',
})

const logisticsDrafts = reactive<Record<number, { company: string; trackingNumber: string }>>({})
const logisticsMessages = reactive<Record<number, string>>({})
const savingLogisticsOrderId = ref<number | null>(null)

const inventoryReport = computed(() => {
  const inbound = inventory.value
    .filter((item) => item.type === 'INBOUND')
    .reduce((sum, item) => sum + item.quantity, 0)
  const outbound = inventory.value
    .filter((item) => item.type === 'OUTBOUND')
    .reduce((sum, item) => sum + item.quantity, 0)
  const adjustment = inventory.value
    .filter((item) => item.type === 'ADJUSTMENT')
    .reduce((sum, item) => sum + item.quantity, 0)
  const totalStock = products.value.reduce((sum, product) => sum + product.stockQuantity, 0)
  const lowStockProducts = products.value
    .filter((product) => product.stockQuantity <= product.lowStockThreshold)
    .sort((left, right) => left.stockQuantity - right.stockQuantity)
  const categorySummary = Array.from(
    products.value.reduce(
      (map, product) => {
        const previous = map.get(product.category) ?? { category: product.category, productCount: 0, stock: 0, lowStockCount: 0 }
        previous.productCount += 1
        previous.stock += product.stockQuantity
        if (product.stockQuantity <= product.lowStockThreshold) {
          previous.lowStockCount += 1
        }
        map.set(product.category, previous)
        return map
      },
      new Map<string, { category: string; productCount: number; stock: number; lowStockCount: number }>(),
    ).values(),
  )
  return {
    inbound,
    outbound,
    adjustment,
    totalStock,
    lowStockProducts,
    categorySummary,
  }
})

const resetProductForm = () => {
  editingProductId.value = null
  productImageFile.value = null
  productImagePreview.value = ''
  Object.assign(productForm, {
    name: '',
    category: '蔬菜',
    price: 58,
    stockQuantity: 28,
    unit: '份',
    description: '',
    farmName: '',
    origin: '',
    certificate: '绿色农场认证',
    traceabilityCode: '',
    plantingDate: '',
    harvestDate: '',
    organic: true,
    featured: false,
    lowStockThreshold: 20,
  })
}

const fillProductForm = (product: AdminProduct) => {
  editingProductId.value = product.id
  productImageFile.value = null
  productImagePreview.value = product.imageUrl ?? ''
  Object.assign(productForm, {
    ...product,
    plantingDate: product.plantingDate ?? '',
    harvestDate: product.harvestDate ?? '',
    farmName: product.farmName ?? '',
    origin: product.origin ?? '',
    certificate: product.certificate ?? '',
    traceabilityCode: product.traceabilityCode ?? '',
  })
}

const onProductImageChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  productImageFile.value = file
  productImagePreview.value = file ? URL.createObjectURL(file) : ''
}

const loadAll = async () => {
  const [overviewResponse, productResponse, inventoryResponse, orderResponse, customerResponse, forecastResponse, productReviewResponse] =
    await Promise.all([
      api.get<DashboardData>('/admin/overview'),
      api.get<AdminProduct[]>('/admin/products'),
      api.get<InventoryMovement[]>('/admin/inventory'),
      api.get<AdminOrder[]>('/admin/orders'),
      api.get<CustomerProfile[]>('/admin/customers'),
      api.get<ForecastData>('/admin/analytics/forecast'),
      api.get<ProductReview[]>('/admin/product-reviews'),
    ])

  overview.value = overviewResponse.data
  products.value = productResponse.data
  inventory.value = inventoryResponse.data
  orders.value = orderResponse.data
  customers.value = customerResponse.data
  forecast.value = forecastResponse.data
  productReviews.value = productReviewResponse.data
  if (!inventoryForm.productId && products.value.length) {
    inventoryForm.productId = products.value[0].id
  }
  orders.value.forEach((order) => {
    logisticsDrafts[order.id] = {
      company: order.logisticsCompany ?? '',
      trackingNumber: order.trackingNumber ?? '',
    }
  })
}

const saveProduct = async () => {
  const payload = new FormData()
  payload.append('name', productForm.name)
  payload.append('category', productForm.category)
  payload.append('price', String(productForm.price))
  payload.append('stockQuantity', String(productForm.stockQuantity))
  payload.append('unit', productForm.unit)
  payload.append('description', productForm.description)
  payload.append('organic', String(productForm.organic))
  payload.append('featured', String(productForm.featured))
  payload.append('lowStockThreshold', String(productForm.lowStockThreshold))
  if (productForm.plantingDate) payload.append('plantingDate', productForm.plantingDate)
  if (productForm.harvestDate) payload.append('harvestDate', productForm.harvestDate)
  if (productForm.farmName) payload.append('farmName', productForm.farmName)
  if (productForm.origin) payload.append('origin', productForm.origin)
  if (productForm.certificate) payload.append('certificate', productForm.certificate)
  if (productForm.traceabilityCode) payload.append('traceabilityCode', productForm.traceabilityCode)
  if (productImageFile.value) payload.append('imageFile', productImageFile.value)
  if (editingProductId.value) {
    await api.put(`/admin/products/${editingProductId.value}`, payload, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  } else {
    await api.post('/admin/products', payload, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  }
  resetProductForm()
  await loadAll()
}

const deleteProduct = async (productId: number) => {
  await api.delete(`/admin/products/${productId}`)
  await loadAll()
}

const createInventory = async () => {
  await api.post('/admin/inventory/movements', inventoryForm)
  await loadAll()
}

const updateOrderStatus = async (orderId: number, status: AdminOrder['status']) => {
  await api.patch(`/admin/orders/${orderId}/status`, { status })
  await loadAll()
}

const getLogisticsDraft = (orderId: number) => {
  if (!logisticsDrafts[orderId]) {
    logisticsDrafts[orderId] = {
      company: '',
      trackingNumber: '',
    }
  }
  return logisticsDrafts[orderId]
}

const saveLogistics = async (orderId: number) => {
  const draft = getLogisticsDraft(orderId)
  const company = draft.company.trim()
  const trackingNumber = draft.trackingNumber.trim()

  if (!company || !trackingNumber) {
    logisticsMessages[orderId] = '请先填写完整的物流公司和物流单号。'
    return
  }

  savingLogisticsOrderId.value = orderId
  logisticsMessages[orderId] = ''
  try {
    await api.patch(`/admin/orders/${orderId}/logistics`, { company, trackingNumber })
    await loadAll()
    logisticsMessages[orderId] = '物流信息已保存，订单状态已更新为已发货。'
  } catch (error) {
    const message =
      typeof error === 'object' &&
      error !== null &&
      'response' in error &&
      typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message ===
        'string'
        ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
        : '保存物流失败，请检查后端返回或重新填写后再试。'
    logisticsMessages[orderId] = message || '保存物流失败，请稍后重试。'
  } finally {
    savingLogisticsOrderId.value = null
  }
}

onMounted(async () => {
  await loadAll()
})

const reviewsByProduct = computed(() => {
  return productReviews.value.reduce(
    (map, review) => {
      const current = map.get(review.productId) ?? []
      current.push(review)
      map.set(review.productId, current)
      return map
    },
    new Map<number, ProductReview[]>(),
  )
})
</script>

<template>
  <PageLayout>
    <section class="content-wrap py-12">
      <div class="organic-card bg-[#fffaf4]">
        <p class="text-sm tracking-[0.22em] text-stone-400">管理控制台</p>
        <h1 class="mt-3 font-serif text-4xl font-semibold text-primary">农场品运营工作台</h1>
        <p class="mt-3 text-sm leading-7 text-stone-500">{{ authStore.isPlatformAdmin ? '平台管理员视角' : '农场管理员视角' }}</p>
        <div class="mt-6 flex flex-wrap gap-3">
          <button
            v-for="tab in tabs"
            :key="tab"
            type="button"
            class="rounded-full px-4 py-2 text-sm font-semibold transition-colors duration-300"
            :class="activeTab === tab ? 'bg-primary text-white' : 'bg-white text-stone-500 border border-stone-200 hover:bg-[#f4eee5]'"
            @click="activeTab = tab"
          >
            {{ tabLabels[tab] }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'overview' && overview" class="content-wrap pb-16">
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">累计销售额</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ formatCurrency(overview.totalRevenue) }}</p>
        </div>
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">订单总数</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ overview.totalOrders }}</p>
        </div>
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">消费者数量</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ overview.totalCustomers }}</p>
        </div>
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">待支付订单</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ overview.pendingOrders }}</p>
        </div>
      </div>

      <div class="mt-8 grid gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <div class="organic-card bg-white">
          <p class="text-sm uppercase tracking-[0.22em] text-stone-400">低库存提醒</p>
          <div class="mt-4 space-y-3">
            <div v-for="product in overview.lowStockProducts" :key="product.id" class="rounded-organic bg-[#faf6f1] p-4">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="font-semibold text-stone-700">{{ product.name }}</p>
                  <p class="mt-1 text-sm text-stone-400">{{ product.category }}</p>
                </div>
                <p class="text-sm font-semibold text-primary">库存 {{ product.stockQuantity }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="organic-card bg-white">
          <p class="text-sm uppercase tracking-[0.22em] text-stone-400">热销排行</p>
          <div class="mt-4 space-y-3">
            <div v-for="item in overview.topProducts" :key="item.productId" class="rounded-organic bg-[#fffaf4] p-4">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="font-semibold text-stone-700">{{ item.name }}</p>
                  <p class="mt-1 text-sm text-stone-400">{{ item.category }}</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-semibold text-primary">销量 {{ item.soldQuantity }}</p>
                  <p class="mt-1 text-xs text-stone-400">{{ formatCurrency(item.sales) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'products'" class="content-wrap grid gap-8 pb-16 lg:grid-cols-[0.8fr_1.2fr]">
      <div class="organic-card bg-white">
        <div class="flex items-center justify-between">
          <h2 class="font-serif text-3xl font-semibold text-primary">{{ editingProductId ? '编辑商品' : '新增商品' }}</h2>
          <button type="button" class="text-sm text-stone-400 hover:text-primary" @click="resetProductForm">重置</button>
        </div>
        <div class="mt-5 space-y-4">
          <div class="rounded-[1.8rem] border border-dashed border-stone-300 bg-[#faf6f1] p-4">
            <p class="text-sm font-semibold text-stone-700">商品图片</p>
            <div class="mt-4 flex flex-col gap-4 md:flex-row md:items-center">
              <img
                v-if="productImagePreview"
                :src="productImagePreview"
                alt="商品图片预览"
                class="h-28 w-28 rounded-[1.4rem] object-cover"
              />
              <div
                v-else
                class="flex h-28 w-28 items-center justify-center rounded-[1.4rem] bg-white text-sm text-stone-400"
              >
                暂无图片
              </div>
              <div class="flex-1 space-y-3">
                <input type="file" accept=".jpg,.jpeg,.png,.webp" class="organic-input" @change="onProductImageChange" />
                <p class="text-sm text-stone-500">支持 jpg、jpeg、png、webp。编辑商品时不重新上传则保留原图。</p>
              </div>
            </div>
          </div>
          <input v-model="productForm.name" class="organic-input" placeholder="商品名称" />
          <div class="grid gap-4 md:grid-cols-2">
            <input v-model="productForm.category" class="organic-input" placeholder="分类" />
            <input v-model="productForm.unit" class="organic-input" placeholder="单位" />
            <input v-model.number="productForm.price" type="number" min="1" class="organic-input" placeholder="价格" />
            <input v-model.number="productForm.stockQuantity" type="number" min="0" class="organic-input" placeholder="库存" />
          </div>
          <input v-model="productForm.origin" class="organic-input" placeholder="产地" />
          <input v-model="productForm.farmName" class="organic-input" placeholder="农场名称" />
          <input v-model="productForm.traceabilityCode" class="organic-input" placeholder="溯源码" />
          <input v-model="productForm.certificate" class="organic-input" placeholder="认证信息" />
          <textarea v-model="productForm.description" class="w-full rounded-[2rem] border border-stone-200 bg-white px-5 py-4 text-sm text-stone-800 outline-none focus:border-stone-400 focus:ring-2 focus:ring-stone-200" rows="4" placeholder="商品描述"></textarea>
          <div class="grid gap-4 md:grid-cols-2">
            <input v-model="productForm.plantingDate" type="date" class="organic-input" />
            <input v-model="productForm.harvestDate" type="date" class="organic-input" />
            <input v-model.number="productForm.lowStockThreshold" type="number" min="1" class="organic-input" placeholder="预警阈值" />
            <div class="flex items-center gap-4 rounded-full border border-stone-200 bg-[#faf6f1] px-5 py-3 text-sm text-stone-600">
              <label class="flex items-center gap-2"><input v-model="productForm.organic" type="checkbox" /> 有机</label>
              <label class="flex items-center gap-2"><input v-model="productForm.featured" type="checkbox" /> 主推</label>
            </div>
          </div>
          <button type="button" class="organic-button" @click="saveProduct">{{ editingProductId ? '保存修改' : '新增商品' }}</button>
        </div>
      </div>

      <div class="space-y-4">
        <div v-for="product in products" :key="product.id" class="organic-card bg-white">
          <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div class="flex items-center gap-4">
              <img
                v-if="product.imageUrl"
                :src="product.imageUrl"
                :alt="product.name"
                class="h-20 w-20 rounded-[1.2rem] object-cover"
              />
              <div
                v-else
                class="flex h-20 w-20 items-center justify-center rounded-[1.2rem] bg-[#faf6f1] text-xs text-stone-400"
              >
                无图片
              </div>
              <div>
                <p class="text-xs uppercase tracking-[0.18em] text-stone-400">{{ product.category }}</p>
                <h3 class="mt-2 font-serif text-2xl font-semibold text-primary">{{ product.name }}</h3>
                <p class="mt-2 text-sm text-stone-500">{{ product.origin }} · {{ product.farmName }}</p>
                <p class="mt-1 text-sm text-stone-400">发布人：{{ product.publisherName }}</p>
              </div>
            </div>
            <div class="flex flex-wrap items-center gap-3 text-sm">
              <span class="rounded-full bg-[#f5eee4] px-4 py-2 text-stone-600">库存 {{ product.stockQuantity }}</span>
              <span class="rounded-full bg-[#eef3ea] px-4 py-2 text-accent-olive">¥{{ product.price }}</span>
              <button type="button" class="organic-button organic-button--ghost" @click="fillProductForm(product)">编辑</button>
              <button type="button" class="organic-button organic-button--ghost" @click="deleteProduct(product.id)">删除</button>
            </div>
          </div>
          <div class="mt-5 rounded-[1.6rem] bg-[#faf6f1] p-4">
            <p class="text-sm font-semibold text-stone-600">用户评论</p>
            <div class="mt-3 space-y-3">
              <div
                v-for="review in reviewsByProduct.get(product.id) ?? []"
                :key="review.reviewId"
                class="rounded-[1.2rem] bg-white p-4"
              >
                <div class="flex items-center justify-between gap-3">
                  <p class="font-semibold text-stone-700">{{ review.customerName }}</p>
                  <p class="text-sm text-stone-400">{{ review.rating }} 星 · {{ formatDateTime(review.createdAt) }}</p>
                </div>
                <p class="mt-2 text-sm leading-7 text-stone-600">{{ review.content }}</p>
              </div>
              <p v-if="!(reviewsByProduct.get(product.id) ?? []).length" class="text-sm text-stone-400">这个商品暂时还没有评论。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'inventory'" class="content-wrap grid gap-8 pb-16 lg:grid-cols-[0.7fr_1.3fr]">
      <div class="organic-card bg-white">
        <h2 class="font-serif text-3xl font-semibold text-primary">库存调拨</h2>
        <div class="mt-5 space-y-4">
          <select v-model.number="inventoryForm.productId" class="organic-input">
            <option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option>
          </select>
          <select v-model="inventoryForm.type" class="organic-input">
            <option value="INBOUND">入库</option>
            <option value="OUTBOUND">出库</option>
            <option value="ADJUSTMENT">调整</option>
          </select>
          <input v-model.number="inventoryForm.quantity" type="number" min="1" class="organic-input" placeholder="数量" />
          <input v-model="inventoryForm.source" class="organic-input" placeholder="来源，如春季补货/门店领用" />
          <input v-model="inventoryForm.remark" class="organic-input" placeholder="备注" />
          <button type="button" class="organic-button" @click="createInventory">提交库存流水</button>
        </div>
      </div>

      <div class="organic-card bg-white">
        <h2 class="font-serif text-3xl font-semibold text-primary">库存流水记录</h2>
        <div class="mt-5 space-y-3">
          <div v-for="item in inventory" :key="item.id" class="rounded-organic bg-[#faf6f1] p-4">
            <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <p class="font-semibold text-stone-700">{{ item.productName }}</p>
                <p class="mt-1 text-sm text-stone-400">{{ item.source || '系统记录' }} · {{ item.remark || '无备注' }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm font-semibold text-primary">{{ item.type }} {{ item.quantity }}</p>
                <p class="mt-1 text-xs text-stone-400">{{ formatDateTime(item.createdAt) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'inventory-report'" class="content-wrap pb-16">
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">当前总库存</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ inventoryReport.totalStock }}</p>
        </div>
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">累计入库</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ inventoryReport.inbound }}</p>
        </div>
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">累计出库</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ inventoryReport.outbound }}</p>
        </div>
        <div class="organic-card bg-white">
          <p class="text-sm text-stone-400">调整数量</p>
          <p class="mt-3 text-3xl font-semibold text-primary">{{ inventoryReport.adjustment }}</p>
        </div>
      </div>

      <div class="mt-8 grid gap-8 lg:grid-cols-[0.95fr_1.05fr]">
        <div class="organic-card bg-white">
          <p class="text-sm uppercase tracking-[0.22em] text-stone-400">分类库存报表</p>
          <div class="mt-5 space-y-3">
            <div
              v-for="item in inventoryReport.categorySummary"
              :key="item.category"
              class="rounded-organic bg-[#faf6f1] p-4"
            >
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="font-semibold text-stone-700">{{ item.category }}</p>
                  <p class="mt-1 text-sm text-stone-400">商品 {{ item.productCount }} 个</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-semibold text-primary">库存 {{ item.stock }}</p>
                  <p class="mt-1 text-xs text-stone-400">预警 {{ item.lowStockCount }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="organic-card bg-white">
          <p class="text-sm uppercase tracking-[0.22em] text-stone-400">低库存商品报表</p>
          <div class="mt-5 space-y-3">
            <div
              v-for="product in inventoryReport.lowStockProducts"
              :key="product.id"
              class="rounded-organic bg-[#fffaf4] p-4"
            >
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="font-semibold text-stone-700">{{ product.name }}</p>
                  <p class="mt-1 text-sm text-stone-400">{{ product.category }} · 阈值 {{ product.lowStockThreshold }}</p>
                </div>
                <p class="text-sm font-semibold text-primary">库存 {{ product.stockQuantity }}</p>
              </div>
            </div>
            <p v-if="!inventoryReport.lowStockProducts.length" class="text-sm text-stone-400">当前没有低库存商品。</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'orders'" class="content-wrap space-y-6 pb-16">
      <div v-for="order in orders" :key="order.id" class="organic-card bg-white">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p class="text-xs uppercase tracking-[0.18em] text-stone-400">{{ order.code }} · {{ order.username }}</p>
            <h3 class="mt-2 font-serif text-3xl font-semibold text-primary">{{ order.customerName }}</h3>
            <p class="mt-2 text-sm text-stone-500">
              下单时间 {{ formatDateTime(order.createdAt) }} · {{ formatCurrency(order.totalAmount) }} ·
              {{ formatOrderStatus(order.status) }} · {{ formatPaymentStatus(order.paymentStatus) }}
            </p>
          </div>
          <div class="grid gap-3 md:grid-cols-[200px_1fr]">
            <select
              class="organic-input"
              :value="order.status"
              :disabled="order.status === 'RECEIVED'"
              @change="updateOrderStatus(order.id, ($event.target as HTMLSelectElement).value as AdminOrder['status'])"
            >
              <option value="PENDING_PAYMENT">待支付</option>
              <option value="PROCESSING">处理中</option>
              <option value="SHIPPED">已发货</option>
              <option value="DELIVERED">已送达</option>
              <option value="RECEIVED" disabled>已收货（用户确认）</option>
              <option value="CANCELLED">已取消</option>
            </select>
            <div class="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
              <input v-model="getLogisticsDraft(order.id).company" class="organic-input" placeholder="物流公司" />
              <input v-model="getLogisticsDraft(order.id).trackingNumber" class="organic-input" placeholder="物流单号" />
              <button
                type="button"
                class="organic-button"
                :disabled="savingLogisticsOrderId === order.id"
                @click="saveLogistics(order.id)"
              >
                {{ savingLogisticsOrderId === order.id ? '保存中...' : '保存物流' }}
              </button>
            </div>
          </div>
        </div>
        <p v-if="logisticsMessages[order.id]" class="mt-4 text-sm text-[#8b5e34]">
          {{ logisticsMessages[order.id] }}
        </p>
        <p v-if="order.receivedAt" class="mt-2 text-sm text-stone-500">
          用户确认收货时间：{{ formatDateTime(order.receivedAt) }}
        </p>
        <div class="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          <div v-for="item in order.items" :key="item.productName" class="rounded-organic bg-[#faf6f1] p-4">
            <p class="font-semibold text-stone-700">{{ item.productName }}</p>
            <p class="mt-1 text-sm text-stone-500">{{ item.quantity }} 件 · {{ formatCurrency(item.subtotal) }}</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'customers'" class="content-wrap pb-16">
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div v-for="customer in customers" :key="customer.id" class="organic-card bg-white">
          <p class="text-xs uppercase tracking-[0.18em] text-stone-400">{{ customer.username }}</p>
          <h3 class="mt-2 font-serif text-3xl font-semibold text-primary">{{ customer.fullName }}</h3>
          <div class="mt-4 space-y-2 text-sm text-stone-500">
            <p>邮箱：{{ customer.email }}</p>
            <p>手机号：{{ customer.phone }}</p>
            <p>支付订单：{{ customer.paidOrderCount }}</p>
            <p>偏好分类：{{ customer.favoriteCategory }}</p>
            <p>累计消费：{{ formatCurrency(customer.totalSpent) }}</p>
            <p>最近下单：{{ formatDate(customer.lastOrderDate) }}</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'forecast' && forecast" class="content-wrap pb-16">
      <div class="organic-card bg-white">
        <div class="flex items-end justify-between gap-4">
          <div>
            <p class="text-sm tracking-[0.22em] text-stone-400">销量预测</p>
            <h2 class="mt-2 font-serif text-4xl font-semibold text-primary">销量趋势与未来预测</h2>
          </div>
          <p class="text-sm text-stone-500">历史 6 个月 + 预测 3 个月</p>
        </div>
        <div class="mt-8">
          <TrendChart
            :labels="forecast.historyLabels"
            :values="forecast.historyValues"
            :compare-labels="forecast.forecastLabels"
            :compare-values="forecast.forecastValues"
          />
        </div>
      </div>
    </section>
  </PageLayout>
</template>
