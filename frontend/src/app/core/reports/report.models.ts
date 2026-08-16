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

export interface MonthlyRevenueReport {
  period: string;
  revenue: number;
  orderCount: number;
}

export interface DashboardReport {
  grossRevenue: number;
  netRevenue: number;
  discountTotal: number;
  averageTicket: number;
  totalOrders: number;
  ordersByStatus: {
    pending: number;
    confirmed: number;
    completed: number;
    cancelled: number;
  };
  capacity: {
    available: number;
    reserved: number;
    outOfStockProducts: number;
  };
  monthlyRevenue: MonthlyRevenueReport[];
  generatedAt: string;
}
