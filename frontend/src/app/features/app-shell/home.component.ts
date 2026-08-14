import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Sesión autenticada</mat-card-title>
        <mat-card-subtitle>Backend stateless con JWT y method security.</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p><strong>Usuario:</strong> {{ authStore.user()?.email }}</p>
        <p><strong>Roles:</strong> {{ authStore.roles().join(', ') }}</p>
        <p><strong>Expira:</strong> {{ authStore.expiresAt() }}</p>
      </mat-card-content>
    </mat-card>
  `
})
export class HomeComponent {
  readonly authStore = inject(AuthStore);
}
