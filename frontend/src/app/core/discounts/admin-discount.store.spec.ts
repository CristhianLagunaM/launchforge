import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminDiscountStore } from './admin-discount.store';

describe('AdminDiscountStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
  });

  it('loads discount configurations and selects the first one', async () => {
    const store = TestBed.inject(AdminDiscountStore);
    const httpTestingController = TestBed.inject(HttpTestingController);

    const loadPromise = store.loadConfigurations();

    const request = httpTestingController.expectOne('/api/v1/discount-configurations');
    expect(request.request.method).toBe('GET');
    request.flush([
      {
        id: '55555555-5555-5555-5555-555555555551',
        code: 'TIME_RANGE',
        type: 'TIME_RANGE',
        enabled: true,
        percentage: 10,
        startAt: '2026-08-01T00:00:00Z',
        endAt: '2026-08-31T23:59:59Z',
        minimumOrders: null,
        lookbackMonths: null,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
        updatedBy: null
      }
    ]);

    await loadPromise;

    expect(store.configurations()).toHaveLength(1);
    expect(store.selectedConfiguration()?.code).toBe('TIME_RANGE');
    httpTestingController.verify();
  });
});
