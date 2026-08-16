import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { describeHttpError } from '../http/http-error.util';
import { CatalogApiService } from './catalog-api.service';
import { Category, Product, ProductFilters, ProductUpsertPayload } from './catalog.models';

interface AdminProductState {
  products: Product[];
  categories: Category[];
  selectedProduct: Product | null;
  filters: ProductFilters;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  sort: string[];
  loading: boolean;
  saving: boolean;
  error: string | null;
}

const initialState: AdminProductState = {
  products: [],
  categories: [],
  selectedProduct: null,
  filters: { active: null },
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  sort: ['updatedAt,desc'],
  loading: false,
  saving: false,
  error: null
};

export const AdminProductStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    isEditing: computed(() => !!store.selectedProduct()),
    emptyState: computed(() => !store.loading() && !store.error() && store.products().length === 0)
  })),
  withMethods((store, catalogApi = inject(CatalogApiService)) => {
    const loadProducts = async (
      overrides?: Partial<Pick<AdminProductState, 'filters' | 'page' | 'size' | 'sort'>>
    ): Promise<void> => {
      const filters = overrides?.filters ?? store.filters();
      const page = overrides?.page ?? store.page();
      const size = overrides?.size ?? store.size();
      const sort = overrides?.sort ?? store.sort();

      patchState(store, { loading: true, error: null, filters, page, size, sort });
      try {
        const response = await firstValueFrom(catalogApi.listProducts(filters, page, size, sort));
        patchState(store, {
          products: response.content,
          page: response.number,
          size: response.size,
          totalElements: response.totalElements,
          totalPages: response.totalPages,
          loading: false
        });
      } catch (error) {
        patchState(store, { loading: false, error: extractProblemDetail(error, 'No fue posible cargar productos.') });
      }
    };

    return {
      loadProducts,
      async loadCategories(): Promise<void> {
        try {
          const categories = await firstValueFrom(catalogApi.getCategories());
          patchState(store, { categories });
        } catch (error) {
          patchState(store, { error: extractProblemDetail(error, 'No fue posible cargar categorías.') });
        }
      },
      selectProduct(product: Product | null): void {
        patchState(store, { selectedProduct: product, error: null });
      },
      async saveProduct(payload: ProductUpsertPayload): Promise<boolean> {
        patchState(store, { saving: true, error: null });

        try {
          const selectedProduct = store.selectedProduct();

          if (selectedProduct) {
            await firstValueFrom(
              catalogApi.updateProduct(selectedProduct.id, payload)
            );
          } else {
            await firstValueFrom(
              catalogApi.createProduct(payload)
            );
          }

          patchState(store, {
            saving: false,
            selectedProduct: null
          });

          await loadProducts({ page: 0 });

          return true;
        } catch (error) {
          patchState(store, {
            saving: false,
            error: extractProblemDetail(
              error,
              'No fue posible guardar el producto.'
            )
          });

          return false;
        }
      },
      async toggleStatus(product: Product): Promise<void> {
        patchState(store, { saving: true, error: null });
        try {
          await firstValueFrom(catalogApi.changeProductStatus(product.id, { active: !product.active }));
          patchState(store, { saving: false });
          await loadProducts();
        } catch (error) {
          patchState(store, { saving: false, error: extractProblemDetail(error, 'No fue posible cambiar el estado.') });
        }
      },
      async deleteProduct(productId: string): Promise<void> {
        patchState(store, { saving: true, error: null });
        try {
          await firstValueFrom(catalogApi.deleteProduct(productId));
          patchState(store, { saving: false, selectedProduct: null });
          await loadProducts();
        } catch (error) {
          patchState(store, { saving: false, error: extractProblemDetail(error, 'No fue posible eliminar el producto.') });
        }
      }
    };
  })
);

function extractProblemDetail(error: unknown, fallback: string): string {
  return describeHttpError(error, fallback);
}
