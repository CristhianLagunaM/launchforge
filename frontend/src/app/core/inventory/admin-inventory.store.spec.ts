import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminInventoryStore } from './admin-inventory.store';

describe('AdminInventoryStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
  });

  it('loads paginated inventory rows', async () => {
    const store = TestBed.inject(AdminInventoryStore);
    const httpTestingController = TestBed.inject(HttpTestingController);

    const loadPromise = store.loadInventory({
      page: 0,
      size: 10,
      sort: ['productName,asc']
    });

    const request = httpTestingController.expectOne((req) => req.url === '/api/v1/inventory');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.getAll('sort')).toEqual(['productName,asc']);

    request.flush({
      content: [
        {
          productId: '22222222-2222-2222-2222-222222222221',
          sku: 'LF-LANDING-001',
          productName: 'Landing Page Launch',
          productActive: true,
          availableQuantity: 8,
          reservedQuantity: 1,
          version: 0,
          updatedAt: '2026-08-01T00:00:00Z'
        }
      ],
      number: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false
    });

    await loadPromise;

    expect(store.inventory()).toHaveLength(1);
    expect(store.selectedInventory()?.sku).toBe('LF-LANDING-001');
    httpTestingController.verify();
  });

  it('reloads inventory after an optimistic locking conflict', async () => {
    const store = TestBed.inject(AdminInventoryStore);
    const http = TestBed.inject(HttpTestingController);
    const initialLoad = store.loadInventory();
    http.expectOne((request) => request.url === '/api/v1/inventory').flush({
      content: [{ productId: 'product-1', sku: 'LF-1', productName: 'Landing', productActive: true,
        availableQuantity: 2, reservedQuantity: 0, version: 1, updatedAt: '2026-08-01T00:00:00Z' }],
      number: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true, empty: false
    });
    await initialLoad;

    const adjustment = store.adjustInventory({ operation: 'INCREASE', quantity: 1, version: 1 });
    http.expectOne('/api/v1/inventory/product-1').flush({ title: 'Conflict', status: 409 }, { status: 409, statusText: 'Conflict' });
    await Promise.resolve();
    http.expectOne((request) => request.url === '/api/v1/inventory').flush({
      content: [{ productId: 'product-1', sku: 'LF-1', productName: 'Landing', productActive: true,
        availableQuantity: 3, reservedQuantity: 0, version: 2, updatedAt: '2026-08-01T00:01:00Z' }],
      number: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true, empty: false
    });
    await adjustment;

    expect(store.selectedInventory()?.version).toBe(2);
    http.verify();
  });
});
