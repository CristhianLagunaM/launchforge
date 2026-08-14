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
}

export interface OrderItem {
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export type OrderStatus = 'CREATED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

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
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}
