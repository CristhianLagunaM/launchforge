import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  template: `
    <section class="home-shell">
      <header class="welcome-panel">
        <p class="eyebrow">Espacio de trabajo LaunchForge</p>
        <h1>Hola, {{ authStore.user()?.firstName }}.</h1>
        <p>Tu próxima solución digital está lista para tomar forma.</p>
      </header>
      <div class="action-grid">
        <a class="action-card" routerLink="/catalog"><mat-icon>explore</mat-icon><span><strong>Explorar catálogo</strong><small>Encuentra el paquete ideal para tu idea.</small></span></a>
        <a class="action-card" routerLink="/cart"><mat-icon>shopping_cart</mat-icon><span><strong>Continuar compra</strong><small>Revisa las soluciones que seleccionaste.</small></span></a>
        <a class="action-card" routerLink="/orders"><mat-icon>receipt_long</mat-icon><span><strong>Mis órdenes</strong><small>Consulta tus solicitudes y su progreso.</small></span></a>
        @if (authStore.isAdmin()) {
          <a class="action-card" routerLink="/admin/products"><mat-icon>dashboard</mat-icon><span><strong>Administración</strong><small>Gestiona el catálogo y la operación.</small></span></a>
        }
      </div>
    </section>
  `,
  styles: [`
    :host { display: block; min-height: calc(100vh - 8rem); }
    .home-shell { display: grid; gap: 1rem; margin: 0 auto; max-width: 88rem; padding: clamp(2rem, 6vw, 5rem) 1rem; }
    .welcome-panel { background: #121516; border: 1px solid #303536; border-radius: 8px; overflow: hidden; padding: clamp(2rem, 6vw, 4.5rem); position: relative; }
    .welcome-panel::after { background: repeating-linear-gradient(90deg, transparent 0 63px, rgba(255,255,255,.025) 64px); border-left: 1px solid #34393a; content: ''; inset: 0 0 0 62%; position: absolute; }
    .eyebrow { color: #72e7ff; font-size: .75rem; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; }
    h1 { font-size: clamp(2.4rem, 6vw, 4.5rem); font-weight: 300; letter-spacing: -.04em; margin: .4rem 0; }
    .welcome-panel > p:last-child { color: #aeb9ca; font-size: 1.05rem; }
    .action-grid { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(min(100%, 15rem), 1fr)); }
    .action-card { align-items: center; background: #121214; border: 1px solid #2b2b2f; border-radius: 6px; color: #edf7ff; display: flex; gap: 1rem; min-height: 6.25rem; padding: 1rem; transition: transform .15s ease, border-color .15s ease; }
    .action-card:hover { border-color: #4a4a50; text-decoration: none; transform: translateY(-2px); }
    .action-card mat-icon { color: #72e7ff; font-size: 2rem; height: 2rem; width: 2rem; }
    .action-card span { display: grid; gap: .35rem; }
    .action-card small { color: #92a0b5; line-height: 1.4; }
  `]
})
export class HomeComponent {
  readonly authStore = inject(AuthStore);
}
