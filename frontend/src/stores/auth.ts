import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import api, { setAuthToken } from '@/lib/api'
import type { Role, UserSummary } from '@/types'

const TOKEN_KEY = 'farmshop-token'
const USER_KEY = 'farmshop-user'

type AuthPayload = {
  token: string
  user: UserSummary
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserSummary | null>(null)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const isConsumer = computed(() => user.value?.role === 'CONSUMER')
  const isFarmAdmin = computed(() => user.value?.role === 'FARM_ADMIN')
  const isPlatformAdmin = computed(() => user.value?.role === 'PLATFORM_ADMIN')
  const isAdmin = computed(() =>
    user.value?.role === 'FARM_ADMIN' || user.value?.role === 'PLATFORM_ADMIN',
  )

  const defaultRoute = computed(() => {
    if (user.value?.role === 'FARM_ADMIN' || user.value?.role === 'PLATFORM_ADMIN') {
      return '/admin'
    }
    return '/home'
  })

  const persist = () => {
    if (token.value && user.value) {
      localStorage.setItem(TOKEN_KEY, token.value)
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      setAuthToken(token.value)
      return
    }
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setAuthToken(null)
  }

  const restore = () => {
    const cachedToken = localStorage.getItem(TOKEN_KEY)
    const cachedUser = localStorage.getItem(USER_KEY)
    if (!cachedToken || !cachedUser) return
    token.value = cachedToken
    user.value = JSON.parse(cachedUser) as UserSummary
    setAuthToken(token.value)
  }

  const consumePayload = (payload: AuthPayload) => {
    token.value = payload.token
    user.value = payload.user
    persist()
  }

  const login = async (username: string, password: string) => {
    const { data } = await api.post<AuthPayload>('/auth/login', { username, password })
    consumePayload(data)
  }

  const register = async (payload: {
    username: string
    password: string
    fullName: string
    email: string
    phone: string
    address: string
    avatarColor: string
    role: Role
  }) => {
    const { data } = await api.post<AuthPayload>('/auth/register', payload)
    consumePayload(data)
  }

  const forgotPassword = async (email: string, newPassword: string) => {
    await api.post('/auth/forgot-password', { email, newPassword })
  }

  const refreshProfile = async () => {
    const { data } = await api.get<UserSummary>('/auth/me')
    user.value = data
    persist()
  }

  const updateProfile = async (payload: FormData) => {
    const { data } = await api.put<UserSummary>('/auth/me', payload, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    user.value = data
    persist()
  }

  const logout = () => {
    token.value = null
    user.value = null
    persist()
  }

  return {
    token,
    user,
    isAuthenticated,
    isConsumer,
    isFarmAdmin,
    isPlatformAdmin,
    isAdmin,
    defaultRoute,
    restore,
    login,
    register,
    forgotPassword,
    refreshProfile,
    updateProfile,
    logout,
  }
})
