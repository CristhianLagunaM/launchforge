import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogStore } from './catalog.store';

describe('CatalogStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
  });

  it('loads products using database-backed search params', async () => {
    const store = TestBed.inject(CatalogStore);
    const httpTestingController = TestBed.inject(HttpTestingController);

    const loadPromise = store.loadProducts({
      filters: { name: 'landing', category: 'web', available: true },
      page: 0,
      size: 12,
      sort: ['name,asc']
    });

    const request = httpTestingController.expectOne((req) => req.url === '/api/v1/products');
    expect(request.request.params.get('name')).toBe('landing');
    expect(request.request.params.get('category')).toBe('web');
    expect(request.request.params.get('available')).toBe('true');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('12');
    expect(request.request.params.getAll('sort')).toEqual(['name,asc']);

    request.flush({
      content: [
        {
          id: '22222222-2222-2222-2222-222222222221',
          sku: 'LF-LANDING-001',
          name: 'Landing Page Launch',
          slug: 'landing-page-launch',
          description: 'High-conversion landing page.',
          category: { id: 1, name: 'WEB', slug: 'web', active: true },
          price: 1200,
          active: true,
          available: true,
          createdAt: '2026-01-05T10:00:00Z',
          updatedAt: '2026-01-05T10:00:00Z'
        }
      ],
      number: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false
    });

    await loadPromise;

    expect(store.products()).toHaveLength(1);
    expect(store.products()[0].sku).toBe('LF-LANDING-001');
    expect(store.totalElements()).toBe(1);
    httpTestingController.verify();
  });

  it('loads categories for filter rendering', async () => {
    const store = TestBed.inject(CatalogStore);
    const httpTestingController = TestBed.inject(HttpTestingController);

    const loadPromise = store.loadCategories();
    const request = httpTestingController.expectOne('/api/v1/categories');
    request.flush([
      { id: 1, name: 'WEB', slug: 'web', description: 'Web presence packages.', active: true }
    ]);

    await loadPromise;

    expect(store.categories()).toHaveLength(1);
    expect(store.categories()[0].slug).toBe('web');
    httpTestingController.verify();
  });
});
