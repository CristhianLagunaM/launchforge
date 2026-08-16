export interface CartItem {
  productId: string;
  sku: string;
  name: string;
  price: number;
  quantity: number;
}

export interface CreateOrderItemPayload {
  productId: string;
  quantity: number;
}

export interface CreateOrderPayload {
  items: CreateOrderItemPayload[];

  requirementDescription: string;
  projectObjective: string;
  contactEmail: string;
  contactPhone?: string;
  desiredDeliveryDate?: string;
  referencesUrl?: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface OrderDiscount {
  code: string;
  percentage: number;
  baseAmount: number;
  amount: number;
  reason: string;
  applicationOrder: number;
}

export type OrderStatus =
  | 'CREATED'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'COMPLETED';

export interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  customerEmail: string;
  status: OrderStatus;
  subtotal: number;
  discountTotal: number;
  total: number;
  idempotencyKey: string | null;

  requirementDescription: string;
  projectObjective: string;
  contactEmail: string;
  contactPhone: string | null;
  desiredDeliveryDate: string | null;
  referencesUrl: string | null;

  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  discounts: OrderDiscount[];
}
