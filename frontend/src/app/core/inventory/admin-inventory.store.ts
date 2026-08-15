import { computed, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { describeHttpError } from '../http/http-error.util';
import { InventoryApiService } from './inventory-api.service';
import { InventoryAdjustmentPayload, InventoryItem } from './inventory.models';

interface AdminInventoryState {
  inventory: InventoryItem[];
  selectedInventory: InventoryItem | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  sort: string[];
  loading: boolean;
  saving: boolean;
  error: string | null;
}

const initialState: AdminInventoryState = {
  inventory: [],
  selectedInventory: null,
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  sort: ['productName,asc'],
  loading: false,
  saving: false,
  error: null
};

export const AdminInventoryStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    emptyState: computed(() => !store.loading() && !store.error() && store.inventory().length === 0)
  })),
  withMethods((store, inventoryApi = inject(InventoryApiService)) => {
    const loadInventory = async (
      overrides?: Partial<Pick<AdminInventoryState, 'page' | 'size' | 'sort'>>
    ): Promise<void> => {
      const page = overrides?.page ?? store.page();
      const size = overrides?.size ?? store.size();
      const sort = overrides?.sort ?? store.sort();

      patchState(store, { loading: true, error: null, page, size, sort });
      try {
        const response = await firstValueFrom(inventoryApi.listInventory(page, size, sort));
        const selectedInventory = store.selectedInventory();
        patchState(store, {
          inventory: response.content,
          selectedInventory: selectedInventory
            ? response.content.find((item) => item.productId === selectedInventory.productId) ?? selectedInventory
            : response.content[0] ?? null,
          page: response.number,
          size: response.size,
          totalElements: response.totalElements,
          totalPages: response.totalPages,
          loading: false
        });
      } catch (error) {
        patchState(store, { loading: false, error: extractProblemDetail(error, 'No fue posible cargar inventario.') });
      }
    };

    return {
      loadInventory,
      selectInventory(item: InventoryItem): void {
        patchState(store, { selectedInventory: item, error: null });
      },
      async adjustInventory(payload: InventoryAdjustmentPayload): Promise<void> {
        const selectedInventory = store.selectedInventory();
        if (!selectedInventory) {
          patchState(store, { error: 'Selecciona un producto para ajustar capacidad.' });
          return;
        }

        patchState(store, { saving: true, error: null });
        try {
          const updated = await firstValueFrom(inventoryApi.adjustInventory(selectedInventory.productId, payload));
          patchState(store, {
            saving: false,
            selectedInventory: updated,
            inventory: store.inventory().map((item) => (item.productId === updated.productId ? updated : item))
          });
        } catch (error) {
        const message = describeHttpError(error, 'No fue posible ajustar inventario.');
        patchState(store, { saving: false, error: message });
        if (error instanceof HttpErrorResponse && error.status === 409) {
          await loadInventory();
        }
        }
      }
    };
  })
);

function extractProblemDetail(error: unknown, fallback: string): string {
  return describeHttpError(error, fallback);
}
