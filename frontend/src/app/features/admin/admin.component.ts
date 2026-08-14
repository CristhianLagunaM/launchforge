import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-admin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Zona ADMIN</mat-card-title>
        <mat-card-subtitle>Ruta protegida por JWT backend y roleGuard frontend.</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>Esta pantalla existe para validar autorización por rol en Fase 2.</p>
      </mat-card-content>
    </mat-card>
  `
})
export class AdminComponent {}
