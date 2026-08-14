import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CreateOrderPayload, Order } from './order.models';

@Injectable({ providedIn: 'root' })
export class OrdersApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly ordersUrl = '/api/v1/orders';

  createOrder(payload: CreateOrderPayload, idempotencyKey: string) {
    return this.httpClient.post<Order>(this.ordersUrl, payload, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey })
    });
  }

  listOrders() {
    return this.httpClient.get<Order[]>(this.ordersUrl);
  }

  getOrder(orderId: string) {
    return this.httpClient.get<Order>(`${this.ordersUrl}/${orderId}`);
  }

  cancelOrder(orderId: string) {
    return this.httpClient.patch<Order>(`${this.ordersUrl}/${orderId}/cancel`, {});
  }
}
