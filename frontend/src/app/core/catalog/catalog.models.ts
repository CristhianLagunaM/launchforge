export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  active: boolean;
}

export interface ProductCategoryView {
  id: number;
  name: string;
  slug: string;
  active: boolean;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description: string;
  category: ProductCategoryView;
  price: number;
  active: boolean;
  available: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductFilters {
  name?: string;
  sku?: string;
  category?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  active?: boolean | null;
  available?: boolean | null;
}

export interface ProductUpsertPayload {
  sku: string;
  name: string;
  slug: string;
  description: string;
  categoryId: number;
  price: number;
}

export interface ProductStatusPayload {
  active: boolean;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
