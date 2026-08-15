import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuditApiService } from './audit-api.service';
import { AuditStore } from './audit.store';

describe('AuditStore', () => {
  const api = { search: vi.fn() };

  beforeEach(() => {
    api.search.mockReset();
    TestBed.configureTestingModule({ providers: [AuditStore, { provide: AuditApiService, useValue: api }] });
  });

  it('loads a paged audit response', async () => {
    api.search.mockReturnValue(of({
      content: [{ id: '1', actorUserId: null, actorEmail: null, action: 'ORDER_CREATED', resourceType: 'ORDER',
        resourceId: 'o1', correlationId: 'c1', ipAddress: '127.0.0.1', metadata: {}, createdAt: '2026-08-15T00:00:00Z' }],
      totalElements: 1, totalPages: 1, number: 0, size: 20
    }));
    const store = TestBed.inject(AuditStore);

    await store.load({ action: 'ORDER_CREATED', page: 0, size: 20 });

    expect(store.entries()).toHaveLength(1);
    expect(store.totalElements()).toBe(1);
    expect(store.error()).toBeNull();
  });

  it('exposes backend errors and clears results', async () => {
    api.search.mockReturnValue(throwError(() => ({ error: { detail: 'Audit unavailable' } })));
    const store = TestBed.inject(AuditStore);

    await store.load();

    expect(store.entries()).toEqual([]);
    expect(store.error()).toBe('Audit unavailable');
  });
});
