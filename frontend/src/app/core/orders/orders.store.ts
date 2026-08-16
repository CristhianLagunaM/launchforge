import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { describeHttpError, describeInventoryCapacityConflict, InventoryCapacityConflict } from '../http/http-error.util';
import { OrdersApiService } from './orders-api.service';
import { CreateOrderPayload, Order } from './order.models';

interface OrdersState {
  orders: Order[];
  selectedOrder: Order | null;
  loading: boolean;
  submitting: boolean;
  error: string | null;
  latestCreatedOrder: Order | null;
  capacityConflict: InventoryCapacityConflict | null;
}

const initialState: OrdersState = {
  orders: [],
  selectedOrder: null,
  loading: false,
  submitting: false,
  error: null,
  latestCreatedOrder: null,
  capacityConflict: null
};

export const OrdersStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    emptyState: computed(() => !store.loading() && !store.error() && store.orders().length === 0)
  })),
  withMethods((store, ordersApi = inject(OrdersApiService)) => ({
    async loadOrders(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const orders = await firstValueFrom(ordersApi.listOrders());
        patchState(store, { orders, loading: false });
      } catch (error) {
        patchState(store, { loading: false, error: describeHttpError(error, 'No fue posible cargar órdenes.') });
      }
    },
    async loadAllOrders(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try { patchState(store, { orders: await firstValueFrom(ordersApi.listAllOrders()), loading: false }); }
      catch (error) { patchState(store, { loading: false, error: describeHttpError(error, 'No fue posible cargar las órdenes administrativas.') }); }
    },
    async loadOrder(orderId: string): Promise<void> {
      patchState(store, { loading: true, error: null, selectedOrder: null });
      try {
        const order = await firstValueFrom(ordersApi.getOrder(orderId));
        patchState(store, { selectedOrder: order, loading: false });
      } catch (error) {
        patchState(store, { loading: false, error: describeHttpError(error, 'No fue posible cargar la orden.') });
      }
    },
    async createOrder(payload: CreateOrderPayload, idempotencyKey: string): Promise<Order | null> {
      patchState(store, { submitting: true, error: null, latestCreatedOrder: null, capacityConflict: null });
      try {
        const order = await firstValueFrom(ordersApi.createOrder(payload, idempotencyKey));
        patchState(store, {
          submitting: false,
          latestCreatedOrder: order,
          capacityConflict: null,
          selectedOrder: order,
          orders: [order, ...store.orders().filter((existing) => existing.id !== order.id)]
        });
        return order;
      } catch (error) {
        patchState(store, {
          submitting: false,
          error: describeHttpError(error, 'No fue posible crear la orden.'),
          capacityConflict: describeInventoryCapacityConflict(error)
        });
        return null;
      }
    },
    async cancelOrder(orderId: string): Promise<void> {
      patchState(store, { submitting: true, error: null });
      try {
        const order = await firstValueFrom(ordersApi.cancelOrder(orderId));
        patchState(store, {
          submitting: false,
          selectedOrder: store.selectedOrder()?.id === order.id ? order : store.selectedOrder(),
          orders: store.orders().map((existing) => (existing.id === order.id ? order : existing))
        });
      } catch (error) {
        patchState(store, { submitting: false, error: describeHttpError(error, 'No fue posible cancelar la orden.') });
      }
    },
    async confirmOrder(orderId: string): Promise<void> {
      patchState(store, { submitting: true, error: null });
      try {
        const order = await firstValueFrom(ordersApi.confirmOrder(orderId));
        patchState(store, { submitting: false, selectedOrder: order, orders: store.orders().map((existing) => existing.id === order.id ? order : existing) });
      } catch (error) { patchState(store, { submitting: false, error: describeHttpError(error, 'No fue posible confirmar la orden.') }); }
    },
    async completeOrder(orderId: string): Promise<void> {
      patchState(store, { submitting: true, error: null });
      try { const order = await firstValueFrom(ordersApi.completeOrder(orderId)); patchState(store, { submitting: false, selectedOrder: order, orders: store.orders().map((existing) => existing.id === order.id ? order : existing) }); }
      catch (error) { patchState(store, { submitting: false, error: describeHttpError(error, 'No fue posible completar la orden.') }); }
    },
    clearSelectedOrder(): void {
      patchState(store, { selectedOrder: null });
    },
    clearLatestCreatedOrder(): void {
      patchState(store, { latestCreatedOrder: null });
    },
    clearCreationFeedback(): void {
      patchState(store, { error: null, capacityConflict: null });
    }
  }))
);
