export const formatCurrency = (value: number) =>
  new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(value)

export const formatDateTime = (value: string | null) => {
  if (!value) return '待更新'
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export const formatDate = (value: string | null) => {
  if (!value) return '待更新'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}

export const formatOrderStatus = (value: string) => {
  const labels: Record<string, string> = {
    PENDING_PAYMENT: '待支付',
    PROCESSING: '处理中',
    SHIPPED: '已发货',
    DELIVERED: '已送达',
    RECEIVED: '已收货',
    CANCELLED: '已取消',
  }
  return labels[value] ?? value
}

export const formatPaymentStatus = (value: string) => {
  const labels: Record<string, string> = {
    UNPAID: '未支付',
    PAID: '已支付',
  }
  return labels[value] ?? value
}

export const formatRole = (value: string) => {
  const labels: Record<string, string> = {
    CONSUMER: '消费者',
    FARM_ADMIN: '农场管理员',
    PLATFORM_ADMIN: '平台管理员',
  }
  return labels[value] ?? value
}
