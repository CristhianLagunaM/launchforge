import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { ProblemDetails } from '../auth/auth.models';
import { DiscountApiService } from './discount-api.service';
import { DiscountConfiguration, DiscountConfigurationUpdatePayload } from './discount.models';

interface AdminDiscountState {
  configurations: DiscountConfiguration[];
  selectedConfiguration: DiscountConfiguration | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
}

const initialState: AdminDiscountState = {
  configurations: [],
  selectedConfiguration: null,
  loading: false,
  saving: false,
  error: null
};

export const AdminDiscountStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    emptyState: computed(() => !store.loading() && !store.error() && store.configurations().length === 0)
  })),
  withMethods((store, discountApi = inject(DiscountApiService)) => ({
    async loadConfigurations(): Promise<void> {
      patchState(store, { loading: true, error: null });
      try {
        const configurations = await firstValueFrom(discountApi.listConfigurations());
        patchState(store, {
          configurations,
          selectedConfiguration: store.selectedConfiguration()
            ? configurations.find((configuration) => configuration.code === store.selectedConfiguration()?.code) ?? configurations[0] ?? null
            : configurations[0] ?? null,
          loading: false
        });
      } catch (error) {
        patchState(store, {
          loading: false,
          error: extractProblemDetail(error, 'No fue posible cargar la configuración de descuentos.')
        });
      }
    },
    selectConfiguration(configuration: DiscountConfiguration): void {
      patchState(store, { selectedConfiguration: configuration, error: null });
    },
    async saveConfiguration(payload: DiscountConfigurationUpdatePayload): Promise<void> {
      const selectedConfiguration = store.selectedConfiguration();
      if (!selectedConfiguration) {
        patchState(store, { error: 'Selecciona una regla de descuento para editar.' });
        return;
      }

      patchState(store, { saving: true, error: null });
      try {
        const updated = await firstValueFrom(discountApi.updateConfiguration(selectedConfiguration.code, payload));
        patchState(store, {
          saving: false,
          selectedConfiguration: updated,
          configurations: store.configurations().map((configuration) =>
            configuration.code === updated.code ? updated : configuration
          )
        });
      } catch (error) {
        patchState(store, { saving: false, error: extractProblemDetail(error, 'No fue posible guardar la configuración.') });
      }
    }
  }))
);

function extractProblemDetail(error: unknown, fallback: string): string {
  const maybeProblem = (error as { error?: ProblemDetails })?.error;
  return maybeProblem?.detail ?? maybeProblem?.title ?? fallback;
}
