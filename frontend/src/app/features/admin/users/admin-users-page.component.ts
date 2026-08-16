import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AdminUser, AdminUsersService } from '../../../core/users/admin-users.service';

@Component({ selector: 'app-admin-users-page', changeDetection: ChangeDetectionStrategy.OnPush, imports: [FormsModule, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule], styles: [`.admin-page{max-width:1100px;margin:auto}.toolbar{display:flex;gap:12px;align-items:center;margin:20px 0}.toolbar input{flex:1;min-width:0;padding:.75rem .9rem}.user-list{display:grid;gap:8px}.user-row{display:grid;grid-template-columns:minmax(320px,2fr) minmax(130px,1fr) minmax(120px,1fr) auto;align-items:center;padding:11px 18px}.user-row > div:first-child{display:flex;flex-direction:column;gap:4px;min-width:0}.user-row small{color:#52525B;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.actions{display:flex;gap:6px;justify-content:flex-end;white-space:nowrap}.actions button{color:#8E8E93}.actions button:hover{color:#F4F4F5}@media(max-width:800px){.user-row{grid-template-columns:1fr 1fr}.actions{grid-column:1/-1;justify-content:flex-start}}`], template: `
<main class="admin-page"><p class="eyebrow">ADMIN</p><h1>Gestión de usuarios</h1><p>Activa o bloquea cuentas y administra únicamente sus roles.</p>
<div class="toolbar"><mat-icon>search</mat-icon><input aria-label="Buscar usuarios" placeholder="Buscar por nombre, correo o rol" [ngModel]="query()" (ngModelChange)="query.set($event)" /></div>
@if (loading()) { <mat-progress-bar mode="indeterminate" /> }
<div class="user-list"><mat-card appearance="outlined" class="user-row"><strong>Usuario</strong><strong>Rol</strong><strong>Estado</strong><strong>Acciones</strong></mat-card>
@for (user of filteredUsers(); track user.id) { <mat-card appearance="outlined" class="user-row"><div><strong>{{ user.firstName }} {{ user.lastName }}</strong><small>{{ user.email }}</small></div><span>{{ user.roles.join(', ') }}</span><span>{{ user.enabled ? 'Activo' : 'Bloqueado' }}</span><div class="actions"><button mat-button (click)="toggle(user)">{{ user.enabled ? 'Bloquear' : 'Activar' }}</button><button mat-button (click)="toggleRole(user)">{{ user.roles.includes('ADMIN') ? 'CUSTOMER' : 'ADMIN' }}</button></div></mat-card> }
</div>
</main>` })
export class AdminUsersPageComponent implements OnInit {
  private readonly service = inject(AdminUsersService); readonly users = signal<AdminUser[]>([]); readonly query = signal(''); readonly loading = signal(false);
  readonly filteredUsers = computed(() => { const q = this.query().trim().toLowerCase(); return !q ? this.users() : this.users().filter((u) => `${u.firstName} ${u.lastName} ${u.email} ${u.roles.join(' ')}`.toLowerCase().includes(q)); });
  async ngOnInit(): Promise<void> { await this.reload(); }
  async toggle(user: AdminUser): Promise<void> { await this.save(user, !user.enabled, user.roles[0] ?? 'CUSTOMER'); }
  async toggleRole(user: AdminUser): Promise<void> { await this.save(user, user.enabled, user.roles.includes('ADMIN') ? 'CUSTOMER' : 'ADMIN'); }
  private async save(user: AdminUser, enabled: boolean, role: string): Promise<void> { this.loading.set(true); try { await this.service.update(user.id, enabled, role); await this.reload(); } finally { this.loading.set(false); } }
  private async reload(): Promise<void> { this.loading.set(true); try { this.users.set(await this.service.list()); } finally { this.loading.set(false); } }
}
