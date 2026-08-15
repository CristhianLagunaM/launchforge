import { computed, inject } from '@angular/core';
import { patchState as patchSignalState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { CartItem } from './order.models';
import { AuthStore } from '../auth/auth.store';

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
  withMethods((store, authStore = inject(AuthStore)) => {
    const updateCart = (items: CartItem[], checkoutIdempotencyKey: string | null): void => {
      const state = { items, checkoutIdempotencyKey };
      patchSignalState(store, state);
      persistState(state);
    };

    return {
    addItem(item: Omit<CartItem, 'quantity'>, quantity = 1): void {
      if (!authStore.isAuthenticated() || !isValidQuantity(quantity)) {
        return;
      }
      const existing = store.items().find((cartItem) => cartItem.productId === item.productId);
      if (existing) {
        const items = store.items().map((cartItem) =>
            cartItem.productId === item.productId ? { ...cartItem, quantity: cartItem.quantity + quantity } : cartItem
          );
        updateCart(items, null);
        return;
      }

      updateCart([...store.items(), { ...item, quantity }], null);
    },
    updateQuantity(productId: string, quantity: number): void {
      if (quantity === 0) {
        updateCart(store.items().filter((item) => item.productId !== productId), null);
        return;
      }

      if (!isValidQuantity(quantity)) {
        return;
      }

      updateCart(store.items().map((item) => (item.productId === productId ? { ...item, quantity } : item)), null);
    },
    removeItem(productId: string): void {
      updateCart(store.items().filter((item) => item.productId !== productId), null);
    },
    clearCart(): void {
      updateCart([], null);
    },
    ensureCheckoutKey(): string {
      const existing = store.checkoutIdempotencyKey();
      if (existing) {
        return existing;
      }

      const nextKey = crypto.randomUUID();
      updateCart(store.items(), nextKey);
      return nextKey;
    },
    completeCheckout(): void {
      updateCart([], null);
    }
    };
  }),
  withHooks({
    onInit(store) {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return;
      }

      patchSignalState(store, JSON.parse(raw) as CartState);
    },
    onDestroy() {
      // no-op
    }
  })
);

function persistState(state: CartState): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function isValidQuantity(quantity: number): boolean {
  return Number.isSafeInteger(quantity) && quantity > 0 && quantity <= 999;
}
