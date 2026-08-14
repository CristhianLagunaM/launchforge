import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { DiscountConfiguration, DiscountConfigurationUpdatePayload } from './discount.models';

@Injectable({ providedIn: 'root' })
export class DiscountApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly discountsUrl = '/api/v1/discount-configurations';

  listConfigurations() {
    return this.httpClient.get<DiscountConfiguration[]>(this.discountsUrl);
  }

  updateConfiguration(code: string, payload: DiscountConfigurationUpdatePayload) {
    return this.httpClient.patch<DiscountConfiguration>(`${this.discountsUrl}/${code}`, payload);
  }
}
