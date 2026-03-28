import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import api from '@/lib/api'
import type { CartItem } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const itemCount = computed(() =>
    items.value.reduce((count: number, item: CartItem) => count + item.quantity, 0),
  )

  const totalAmount = computed(() =>
    items.value.reduce((sum: number, item: CartItem) => sum + item.subtotal, 0),
  )

  const loadCart = async () => {
    const { data } = await api.get<CartItem[]>('/cart')
    items.value = data
  }

  const addToCart = async (productId: number, quantity = 1) => {
    await api.post('/cart/items', { productId, quantity })
    await loadCart()
  }

  const updateQuantity = async (itemId: number, quantity: number) => {
    await api.patch(`/cart/items/${itemId}`, { quantity })
    await loadCart()
  }

  const removeItem = async (itemId: number) => {
    await api.delete(`/cart/items/${itemId}`)
    await loadCart()
  }

  const clear = () => {
    items.value = []
  }

  return {
    items,
    itemCount,
    totalAmount,
    loadCart,
    addToCart,
    updateQuantity,
    removeItem,
    clear,
  }
})
