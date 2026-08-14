import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuthResponse, LoginRequest, RegisterRequest } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUrl = '/api/v1/auth';

  login(payload: LoginRequest) {
    return this.httpClient.post<AuthResponse>(`${this.baseUrl}/login`, payload);
  }

  register(payload: RegisterRequest) {
    return this.httpClient.post<AuthResponse>(`${this.baseUrl}/register`, payload);
  }
}
