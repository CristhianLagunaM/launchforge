import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Category, PageResponse, Product, ProductFilters, ProductStatusPayload, ProductUpsertPayload } from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly productsUrl = '/api/v1/products';
  private readonly categoriesUrl = '/api/v1/categories';

  listProducts(filters: ProductFilters, page: number, size: number, sort: string[]) {
    let params = new HttpParams().set('page', page).set('size', size);

    for (const sortValue of sort) {
      params = params.append('sort', sortValue);
    }

    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }

    return this.httpClient.get<PageResponse<Product>>(this.productsUrl, { params });
  }

  getProduct(id: string) {
    return this.httpClient.get<Product>(`${this.productsUrl}/${id}`);
  }

  getCategories() {
    return this.httpClient.get<Category[]>(this.categoriesUrl);
  }

  createProduct(payload: ProductUpsertPayload) {
    return this.httpClient.post<Product>(this.productsUrl, payload);
  }

  updateProduct(id: string, payload: ProductUpsertPayload) {
    return this.httpClient.put<Product>(`${this.productsUrl}/${id}`, payload);
  }

  changeProductStatus(id: string, payload: ProductStatusPayload) {
    return this.httpClient.patch<Product>(`${this.productsUrl}/${id}/status`, payload);
  }

  deleteProduct(id: string) {
    return this.httpClient.delete<void>(`${this.productsUrl}/${id}`);
  }
}
