import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { ProblemDetails } from '../auth/auth.models';
import { AuditApiService } from './audit-api.service';
import { AuditFilters, AuditLogEntry } from './audit.models';

interface AuditState {
  entries: AuditLogEntry[];
  totalElements: number;
  page: number;
  size: number;
  loading: boolean;
  error: string | null;
}

const initialState: AuditState = { entries: [], totalElements: 0, page: 0, size: 20, loading: false, error: null };

export const AuditStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({ empty: computed(() => !store.loading() && !store.error() && store.entries().length === 0) })),
  withMethods((store, api = inject(AuditApiService)) => ({
    async load(filters: Partial<AuditFilters> = {}): Promise<void> {
      const request = { ...filters, page: filters.page ?? store.page(), size: filters.size ?? store.size() };
      patchState(store, { loading: true, error: null });
      try {
        const response = await firstValueFrom(api.search(request));
        patchState(store, {
          entries: response.content,
          totalElements: response.totalElements,
          page: response.number,
          size: response.size,
          loading: false
        });
      } catch (error) {
        const problem = (error as { error?: ProblemDetails })?.error;
        patchState(store, {
          entries: [], totalElements: 0, loading: false,
          error: problem?.detail ?? problem?.title ?? 'No fue posible consultar la auditoría.'
        });
      }
    }
  }))
);
