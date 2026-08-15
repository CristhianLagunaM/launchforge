import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReportStore } from './report.store';

describe('ReportStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  });

  it('exposes loading and maps the three prepared backend responses', async () => {
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
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
    expect(store.activeProducts()[0]?.sku).toBe('LF-1');
    expect(store.topProducts()[0]?.quantitySold).toBe(12);
    expect(store.topCustomers()[0]?.orderCount).toBe(7);
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
    await loadPromise;

    expect(store.loading()).toBe(false);
    expect(store.error()).toBe('PostgreSQL is unavailable.');
    expect(store.emptyState()).toBe(false);
    expect(store.topProducts()).toEqual([]);
    http.verify();
  });
});

