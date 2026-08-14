import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { ProblemDetails } from '../auth/auth.models';
import { CatalogApiService } from './catalog-api.service';
import { Category, Product, ProductFilters } from './catalog.models';

interface CatalogState {
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
  detailLoading: boolean;
  categoriesLoading: boolean;
  error: string | null;
}

const initialState: CatalogState = {
  products: [],
  categories: [],
  selectedProduct: null,
  filters: {},
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0,
  sort: ['name,asc'],
  loading: false,
  detailLoading: false,
  categoriesLoading: false,
  error: null
};

export const CatalogStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    hasResults: computed(() => store.products().length > 0),
    isEmpty: computed(() => !store.loading() && !store.error() && store.products().length === 0),
    selectedCategoryLabel: computed(() => store.selectedProduct()?.category.name ?? '')
  })),
  withMethods((store, catalogApi = inject(CatalogApiService)) => ({
    async loadProducts(overrides?: Partial<Pick<CatalogState, 'filters' | 'page' | 'size' | 'sort'>>): Promise<void> {
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
        patchState(store, {
          loading: false,
          error: extractProblemDetail(error, 'No fue posible cargar el catálogo.')
        });
      }
    },
    async loadCategories(): Promise<void> {
      patchState(store, { categoriesLoading: true });
      try {
        const categories = await firstValueFrom(catalogApi.getCategories());
        patchState(store, { categories, categoriesLoading: false });
      } catch (error) {
        patchState(store, {
          categoriesLoading: false,
          error: extractProblemDetail(error, 'No fue posible cargar las categorías.')
        });
      }
    },
    async loadProduct(productId: string): Promise<void> {
      patchState(store, { detailLoading: true, error: null, selectedProduct: null });
      try {
        const product = await firstValueFrom(catalogApi.getProduct(productId));
        patchState(store, { selectedProduct: product, detailLoading: false });
      } catch (error) {
        patchState(store, {
          detailLoading: false,
          error: extractProblemDetail(error, 'No fue posible cargar el producto.')
        });
      }
    },
    clearSelectedProduct(): void {
      patchState(store, { selectedProduct: null });
    }
  }))
);

function extractProblemDetail(error: unknown, fallback: string): string {
  const maybeProblem = (error as { error?: ProblemDetails })?.error;
  return maybeProblem?.detail ?? maybeProblem?.title ?? fallback;
}
