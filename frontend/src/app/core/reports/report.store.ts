import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { ProblemDetails } from '../auth/auth.models';
import { ReportApiService } from './report-api.service';
import { ActiveProductReport, TopCustomerReport, TopProductReport } from './report.models';

interface ReportState {
  activeProducts: ActiveProductReport[];
  topProducts: TopProductReport[];
  topCustomers: TopCustomerReport[];
  loading: boolean;
  error: string | null;
}

const initialState: ReportState = {
  activeProducts: [],
  topProducts: [],
  topCustomers: [],
  loading: false,
  error: null
};

export const ReportStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    emptyState: computed(() =>
      !store.loading()
      && !store.error()
      && store.activeProducts().length === 0
      && store.topProducts().length === 0
      && store.topCustomers().length === 0)
  })),
  withMethods((store, reportApi = inject(ReportApiService)) => ({
    async load(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const [activeProducts, topProducts, topCustomers] = await Promise.all([
          firstValueFrom(reportApi.activeProducts()),
          firstValueFrom(reportApi.topProducts()),
          firstValueFrom(reportApi.topCustomers())
        ]);
        patchState(store, { activeProducts, topProducts, topCustomers, loading: false });
      } catch (error) {
        patchState(store, {
          activeProducts: [],
          topProducts: [],
          topCustomers: [],
          loading: false,
          error: extractProblemDetail(error)
        });
      }
    }
  }))
);

function extractProblemDetail(error: unknown): string {
  const problem = (error as { error?: ProblemDetails })?.error;
  return problem?.detail ?? problem?.title ?? 'No fue posible cargar los reportes.';
}

