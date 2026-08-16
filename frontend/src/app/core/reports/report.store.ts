import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { ProblemDetails } from '../auth/auth.models';
import { ReportApiService } from './report-api.service';
import { ActiveProductReport, DashboardReport, TopCustomerReport, TopProductReport } from './report.models';

interface ReportState {
  activeProducts: ActiveProductReport[];
  topProducts: TopProductReport[];
  topCustomers: TopCustomerReport[];
  dashboard: DashboardReport | null;
  loading: boolean;
  error: string | null;
}

const initialState: ReportState = {
  activeProducts: [],
  topProducts: [],
  topCustomers: [],
  dashboard: null,
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
      && store.topCustomers().length === 0
      && store.dashboard() === null)
  })),
  withMethods((store, reportApi = inject(ReportApiService)) => ({
    async load(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const [activeProducts, topProducts, topCustomers, dashboard] = await Promise.all([
          firstValueFrom(reportApi.activeProducts()),
          firstValueFrom(reportApi.topProducts()),
          firstValueFrom(reportApi.topCustomers()),
          firstValueFrom(reportApi.dashboard())
        ]);
        patchState(store, { activeProducts, topProducts, topCustomers, dashboard, loading: false });
      } catch (error) {
        patchState(store, {
          activeProducts: [],
          topProducts: [],
          topCustomers: [],
          dashboard: null,
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
