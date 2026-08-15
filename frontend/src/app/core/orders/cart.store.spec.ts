import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartStore } from './cart.store';

describe('CartStore', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('launchforge.auth.session', JSON.stringify({
      accessToken: 'test-token', issuedAt: '2026-08-15T00:00:00Z', expiresAt: '2099-08-15T01:00:00Z',
      user: { id: 'user-1', email: 'customer@launchforge.dev', firstName: 'Customer', lastName: 'Test', roles: ['CUSTOMER'] }
    }));
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
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

  it('starts a new checkout intention when cart contents change', () => {
    const store = TestBed.inject(CartStore);
    store.addItem({ productId: 'product-1', sku: 'LF-1', name: 'Landing', price: 100 });
    const previousKey = store.ensureCheckoutKey();

    store.updateQuantity('product-1', 2);

    expect(store.checkoutIdempotencyKey()).toBeNull();
    expect(store.ensureCheckoutKey()).not.toBe(previousKey);
  });

  it('rejects invalid quantities', () => {
    const store = TestBed.inject(CartStore);
    store.addItem({ productId: 'product-1', sku: 'LF-1', name: 'Landing', price: 100 }, -1);
    expect(store.isEmpty()).toBe(true);
  });

  it('rejects products when there is no authenticated session', () => {
    localStorage.removeItem('launchforge.auth.session');
    const store = TestBed.inject(CartStore);
    store.addItem({ productId: 'product-1', sku: 'LF-1', name: 'Landing', price: 100 });
    expect(store.isEmpty()).toBe(true);
  });
});
