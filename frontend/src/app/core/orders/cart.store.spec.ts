import { TestBed } from '@angular/core/testing';
import { CartStore } from './cart.store';

describe('CartStore', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  it('adds products and persists the cart in localStorage', () => {
    const store = TestBed.inject(CartStore);

    store.addItem(
      {
        productId: '22222222-2222-2222-2222-222222222221',
        sku: 'LF-LANDING-001',
        name: 'Landing Page Launch',
        price: 1200
      },
      2
    );

    expect(store.items()).toHaveLength(1);
    expect(store.items()[0].quantity).toBe(2);
    expect(store.subtotal()).toBe(2400);

    const persisted = JSON.parse(localStorage.getItem('launchforge-cart') ?? '{}') as {
      items?: Array<{ quantity: number }>;
    };
    expect(persisted.items?.[0]?.quantity).toBe(2);
  });

  it('reuses the same idempotency key until checkout completes', () => {
    const store = TestBed.inject(CartStore);

    const firstKey = store.ensureCheckoutKey();
    const secondKey = store.ensureCheckoutKey();

    expect(firstKey).toBe(secondKey);

    store.completeCheckout();

    expect(store.checkoutIdempotencyKey()).toBeNull();
    expect(store.items()).toHaveLength(0);
  });
});
