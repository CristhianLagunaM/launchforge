import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface AdminUser { id: string; email: string; firstName: string; lastName: string; enabled: boolean; roles: string[]; }

@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private readonly http = inject(HttpClient);
  list(): Promise<AdminUser[]> { return firstValueFrom(this.http.get<AdminUser[]>('/api/v1/admin/users')); }
  update(id: string, enabled: boolean, role: string): Promise<AdminUser> { return firstValueFrom(this.http.patch<AdminUser>(`/api/v1/admin/users/${id}`, { enabled, role })); }
}
