import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuditFilters, AuditPage } from './audit.models';

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);

  search(filters: AuditFilters) {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    for (const key of ['action', 'resourceType', 'actor', 'from', 'to'] as const) {
      const value = filters[key];
      if (value) params = params.set(key, value);
    }
    return this.http.get<AuditPage>('/api/v1/audit', { params });
  }
}
