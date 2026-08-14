import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-forbidden',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatCardModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>403 Forbidden</mat-card-title>
        <mat-card-subtitle>Tu usuario está autenticado, pero no tiene este rol.</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>La autorización real ocurre en backend. El guard de Angular solo mejora la experiencia de navegación.</p>
      </mat-card-content>
      <mat-card-actions>
        <a mat-flat-button color="primary" routerLink="/app">Volver al inicio</a>
      </mat-card-actions>
    </mat-card>
  `
})
export class ForbiddenComponent {}
