import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { InventoryAdjustmentPayload, InventoryItem, InventoryPageResponse } from './inventory.models';

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly inventoryUrl = '/api/v1/inventory';

  listInventory(page: number, size: number, sort: string[]) {
    let params = new HttpParams().set('page', page).set('size', size);

    for (const sortValue of sort) {
      params = params.append('sort', sortValue);
    }

    return this.httpClient.get<InventoryPageResponse>(this.inventoryUrl, { params });
  }

  getInventory(productId: string) {
    return this.httpClient.get<InventoryItem>(`${this.inventoryUrl}/${productId}`);
  }

  adjustInventory(productId: string, payload: InventoryAdjustmentPayload) {
    return this.httpClient.patch<InventoryItem>(`${this.inventoryUrl}/${productId}`, payload);
  }
}
