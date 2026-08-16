import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ActiveProductReport, DashboardReport, TopCustomerReport, TopProductReport } from './report.models';

@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly reportsUrl = '/api/v1/reports';

  activeProducts() {
    return this.httpClient.get<ActiveProductReport[]>(`${this.reportsUrl}/active-products`);
  }

  topProducts() {
    return this.httpClient.get<TopProductReport[]>(`${this.reportsUrl}/top-products`);
  }

  topCustomers() {
    return this.httpClient.get<TopCustomerReport[]>(`${this.reportsUrl}/top-customers`);
  }

  dashboard() {
    return this.httpClient.get<DashboardReport>(`${this.reportsUrl}/dashboard`);
  }
}
