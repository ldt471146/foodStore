export type Role = 'CONSUMER' | 'FARM_ADMIN' | 'PLATFORM_ADMIN'

export interface UserSummary {
  id: number
  username: string
  fullName: string
  email: string | null
  phone: string | null
  address: string | null
  avatarColor: string | null
  avatarImageUrl: string | null
  role: Role
}

export interface ProductSummary {
  id: number
  name: string
  category: string
  price: number
  rating: number
  imageUrl: string | null
  publisherName: string
  origin: string | null
  farmName: string | null
  stockQuantity: number
  featured: boolean
  organic: boolean
}

export interface ProductDetail extends ProductSummary {
  description: string
  traceabilityCode: string | null
  certificate: string | null
  plantingDate: string | null
  harvestDate: string | null
  unit: string
  lowStockThreshold: number
}

export interface Review {
  id: number
  userName: string
  rating: number
  content: string
  createdAt: string
}

export interface ReviewEligibility {
  canReview: boolean
  reviewed: boolean
  message: string
  orderItemId: number | null
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  imageUrl: string | null
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  publisherName: string
  unitPrice: number
  quantity: number
  subtotal: number
  canReview: boolean
  reviewed: boolean
  reviewMessage: string
}

export interface Order {
  id: number
  code: string
  status: 'PENDING_PAYMENT' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'RECEIVED' | 'CANCELLED'
  paymentStatus: 'UNPAID' | 'PAID'
  totalAmount: number
  recipientName: string
  recipientPhone: string
  recipientAddress: string
  logisticsCompany: string | null
  trackingNumber: string | null
  createdAt: string
  paidAt: string | null
  shippedAt: string | null
  deliveredAt: string | null
  receivedAt: string | null
  items: OrderItem[]
}

export interface DashboardData {
  totalRevenue: number
  totalOrders: number
  totalCustomers: number
  pendingOrders: number
  topProducts: Array<{
    productId: number
    name: string
    category: string
    soldQuantity: number
    sales: number
  }>
  lowStockProducts: ProductSummary[]
}

export interface InventoryMovement {
  id: number
  productId: number
  productName: string
  type: 'INBOUND' | 'OUTBOUND' | 'ADJUSTMENT'
  quantity: number
  source: string | null
  remark: string | null
  createdAt: string
}

export interface CustomerProfile {
  id: number
  fullName: string
  username: string
  email: string
  phone: string
  paidOrderCount: number
  totalSpent: number
  favoriteCategory: string
  lastOrderDate: string | null
}

export interface ForecastData {
  historyLabels: string[]
  historyValues: number[]
  forecastLabels: string[]
  forecastValues: number[]
}
