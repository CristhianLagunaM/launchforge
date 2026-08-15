export interface ActiveProductReport {
  id: string;
  sku: string;
  name: string;
  category: string;
  price: number;
}

export interface TopProductReport {
  productId: string;
  sku: string;
  name: string;
  quantitySold: number;
}

export interface TopCustomerReport {
  customerId: string;
  email: string;
  firstName: string;
  lastName: string;
  orderCount: number;
}

