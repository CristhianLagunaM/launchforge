import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatCardModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Sesión autenticada</mat-card-title>
        <mat-card-subtitle>Panel interno con JWT stateless, method security y catálogo administrable.</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p><strong>Usuario:</strong> {{ authStore.user()?.email }}</p>
        <p><strong>Roles:</strong> {{ authStore.roles().join(', ') }}</p>
        <p><strong>Expira:</strong> {{ authStore.expiresAt() }}</p>
      </mat-card-content>
      <mat-card-actions>
        <a mat-flat-button color="primary" routerLink="/products">Ver catálogo público</a>
        @if (authStore.isAdmin()) {
          <a mat-button routerLink="/app/admin/products">Administrar productos</a>
          <a mat-button routerLink="/app/admin/inventory">Administrar inventario</a>
        }
      </mat-card-actions>
    </mat-card>
  `
})
export class HomeComponent {
  readonly authStore = inject(AuthStore);
}
