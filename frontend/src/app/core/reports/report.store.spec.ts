import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReportStore } from './report.store';

describe('ReportStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  });

  it('exposes loading and maps the prepared backend responses', async () => {
    const store = TestBed.inject(ReportStore);
    const http = TestBed.inject(HttpTestingController);

    const loadPromise = store.load();
    expect(store.loading()).toBe(true);

    http.expectOne('/api/v1/reports/active-products').flush([
      { id: 'p-1', sku: 'LF-1', name: 'Landing', category: 'WEB', price: 1200 }
    ]);
    http.expectOne('/api/v1/reports/top-products').flush([
      { productId: 'p-1', sku: 'LF-1', name: 'Landing', quantitySold: 12 }
    ]);
    http.expectOne('/api/v1/reports/top-customers').flush([
      { customerId: 'u-1', email: 'customer@test.dev', firstName: 'Ada', lastName: 'Lovelace', orderCount: 7 }
    ]);
    http.expectOne('/api/v1/reports/dashboard').flush({
      grossRevenue: 1500, netRevenue: 1200, discountTotal: 300, averageTicket: 600, totalOrders: 4,
      ordersByStatus: { pending: 1, confirmed: 1, completed: 1, cancelled: 1 },
      capacity: { available: 12, reserved: 2, outOfStockProducts: 1 },
      monthlyRevenue: [{ period: '2026-08', revenue: 1200, orderCount: 2 }], generatedAt: '2026-08-15T20:00:00Z'
    });
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
    expect(store.activeProducts()[0]?.sku).toBe('LF-1');
    expect(store.topProducts()[0]?.quantitySold).toBe(12);
    expect(store.topCustomers()[0]?.orderCount).toBe(7);
    expect(store.dashboard()?.netRevenue).toBe(1200);
    http.verify();
  });

  it('clears data and exposes backend errors', async () => {
    const store = TestBed.inject(ReportStore);
    const http = TestBed.inject(HttpTestingController);

    const loadPromise = store.load();
    http.expectOne('/api/v1/reports/active-products').flush(
      { title: 'Report failure', detail: 'PostgreSQL is unavailable.' },
      { status: 503, statusText: 'Service Unavailable' }
    );
    http.expectOne('/api/v1/reports/top-products').flush([]);
    http.expectOne('/api/v1/reports/top-customers').flush([]);
    http.expectOne('/api/v1/reports/dashboard').flush({});
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.error()).toBe('PostgreSQL is unavailable.');
    expect(store.emptyState()).toBe(false);
    expect(store.topProducts()).toEqual([]);
    http.verify();
  });
});
