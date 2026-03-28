<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { formatRole } from '@/lib/format'
import PageLayout from '@/components/layout/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const saving = ref(false)
const message = ref('')
const avatarPreview = ref<string | null>(null)
const avatarFile = ref<File | null>(null)

const form = reactive({
  fullName: '',
  email: '',
  phone: '',
  address: '',
  avatarColor: '#8b9d77',
})

const fillForm = () => {
  if (!authStore.user) return
  form.fullName = authStore.user.fullName
  form.email = authStore.user.email ?? ''
  form.phone = authStore.user.phone ?? ''
  form.address = authStore.user.address ?? ''
  form.avatarColor = authStore.user.avatarColor ?? '#8b9d77'
  avatarPreview.value = authStore.user.avatarImageUrl
}

const onAvatarChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  avatarFile.value = file
  avatarPreview.value = file ? URL.createObjectURL(file) : authStore.user?.avatarImageUrl ?? null
}

const submit = async () => {
  saving.value = true
  message.value = ''
  try {
    const payload = new FormData()
    payload.append('fullName', form.fullName)
    payload.append('email', form.email)
    payload.append('phone', form.phone)
    payload.append('address', form.address)
    payload.append('avatarColor', form.avatarColor)
    if (avatarFile.value) {
      payload.append('avatarFile', avatarFile.value)
    }
    await authStore.updateProfile(payload)
    fillForm()
    message.value = '个人信息已保存。'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await authStore.refreshProfile()
  fillForm()
})
</script>

<template>
  <PageLayout>
    <section class="content-wrap py-12">
      <div class="organic-card bg-[#fffaf4]">
        <p class="text-sm tracking-[0.22em] text-stone-400">个人中心</p>
        <h1 class="mt-3 font-serif text-4xl font-semibold text-primary">我的个人信息</h1>
        <p class="mt-3 text-sm leading-7 text-stone-500">你可以在这里查看并修改自己的资料，也可以更换头像。</p>
      </div>
    </section>

    <section class="content-wrap grid gap-8 pb-16 lg:grid-cols-[0.72fr_1.28fr]">
      <div class="organic-card bg-white">
        <h2 class="font-serif text-3xl font-semibold text-primary">头像设置</h2>
        <div class="mt-6 flex flex-col items-center gap-4">
          <img
            v-if="avatarPreview"
            :src="avatarPreview"
            alt="头像预览"
            class="h-36 w-36 rounded-full object-cover"
          />
          <div
            v-else
            class="flex h-36 w-36 items-center justify-center rounded-full text-4xl font-semibold text-white"
            :style="{ backgroundColor: form.avatarColor }"
          >
            {{ authStore.user?.fullName?.slice(0, 1) ?? '游' }}
          </div>
          <input type="file" accept=".jpg,.jpeg,.png,.webp" class="organic-input" @change="onAvatarChange" />
          <input v-model="form.avatarColor" type="color" class="h-12 w-full rounded-[1.2rem] border border-stone-200 bg-white px-3 py-2" />
          <p class="text-sm text-stone-500">支持 jpg、jpeg、png、webp。没有上传图片时，会显示颜色头像。</p>
        </div>
      </div>

      <div class="organic-card bg-white">
        <h2 class="font-serif text-3xl font-semibold text-primary">资料编辑</h2>
        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <input v-model="form.fullName" class="organic-input" placeholder="姓名" />
          <input :value="authStore.user?.username ?? ''" class="organic-input bg-stone-50" placeholder="用户名" disabled />
          <input v-model="form.email" class="organic-input" placeholder="邮箱" />
          <input v-model="form.phone" class="organic-input" placeholder="手机号" />
          <input v-model="form.address" class="organic-input md:col-span-2" placeholder="地址" />
          <input :value="formatRole(authStore.user?.role ?? '')" class="organic-input bg-stone-50 md:col-span-2" placeholder="角色" disabled />
        </div>
        <div class="mt-6 flex items-center gap-4">
          <button type="button" class="organic-button" :disabled="saving" @click="submit">
            {{ saving ? '保存中...' : '保存个人信息' }}
          </button>
          <span class="text-sm text-accent-olive">{{ message }}</span>
        </div>
      </div>
    </section>
  </PageLayout>
</template>
