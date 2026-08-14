import { computed } from '@angular/core';
import { patchState as patchSignalState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { CartItem } from './order.models';

const STORAGE_KEY = 'launchforge-cart';

interface CartState {
  items: CartItem[];
  checkoutIdempotencyKey: string | null;
}

const initialState: CartState = {
  items: [],
  checkoutIdempotencyKey: null
};

export const CartStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    itemCount: computed(() => store.items().reduce((total, item) => total + item.quantity, 0)),
    subtotal: computed(() => store.items().reduce((total, item) => total + item.price * item.quantity, 0)),
    isEmpty: computed(() => store.items().length === 0)
  })),
  withMethods((store) => ({
    addItem(item: Omit<CartItem, 'quantity'>, quantity = 1): void {
      const existing = store.items().find((cartItem) => cartItem.productId === item.productId);
      if (existing) {
        patchState(store, {
          items: store.items().map((cartItem) =>
            cartItem.productId === item.productId ? { ...cartItem, quantity: cartItem.quantity + quantity } : cartItem
          )
        });
        return;
      }

      patchState(store, {
        items: [...store.items(), { ...item, quantity }]
      });
    },
    updateQuantity(productId: string, quantity: number): void {
      if (quantity <= 0) {
        patchState(store, {
          items: store.items().filter((item) => item.productId !== productId)
        });
        return;
      }

      patchState(store, {
        items: store.items().map((item) => (item.productId === productId ? { ...item, quantity } : item))
      });
    },
    removeItem(productId: string): void {
      patchState(store, {
        items: store.items().filter((item) => item.productId !== productId)
      });
    },
    clearCart(): void {
      patchState(store, initialState);
    },
    ensureCheckoutKey(): string {
      const existing = store.checkoutIdempotencyKey();
      if (existing) {
        return existing;
      }

      const nextKey = crypto.randomUUID();
      patchState(store, { checkoutIdempotencyKey: nextKey });
      return nextKey;
    },
    completeCheckout(): void {
      patchState(store, initialState);
    }
  })),
  withHooks({
    onInit(store) {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return;
      }

      patchState(store, JSON.parse(raw) as CartState);
    },
    onDestroy() {
      // no-op
    }
  })
);

function persistState(state: CartState): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function patchState(store: any, partialState: Partial<CartState>): void {
  patchSignalState(store, partialState);
  persistState({
    items: store.items(),
    checkoutIdempotencyKey: store.checkoutIdempotencyKey()
  });
}
