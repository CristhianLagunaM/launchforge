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
        <p class="eyebrow">LaunchForge workspace</p>
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
    .home-shell { display: grid; gap: 1.5rem; margin: 0 auto; max-width: 76rem; padding: clamp(2rem, 7vw, 6rem) 1rem; }
    .welcome-panel { background: linear-gradient(120deg, rgba(10,25,40,.95), rgba(20,13,48,.9)); border: 1px solid rgba(114,231,255,.2); border-radius: 1.5rem; overflow: hidden; padding: clamp(2rem, 6vw, 4.5rem); position: relative; }
    .welcome-panel::after { background: radial-gradient(circle, rgba(114,231,255,.35), transparent 65%); content: ''; height: 18rem; position: absolute; right: -5rem; top: -8rem; width: 18rem; }
    .eyebrow { color: #72e7ff; font-size: .75rem; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; }
    h1 { font-size: clamp(2.4rem, 6vw, 4.5rem); font-weight: 300; letter-spacing: -.04em; margin: .4rem 0; }
    .welcome-panel > p:last-child { color: #aeb9ca; font-size: 1.05rem; }
    .action-grid { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(min(100%, 15rem), 1fr)); }
    .action-card { align-items: center; background: rgba(12,18,30,.88); border: 1px solid rgba(255,255,255,.09); border-radius: 1rem; color: #edf7ff; display: flex; gap: 1rem; min-height: 7rem; padding: 1.25rem; transition: transform .2s ease, border-color .2s ease; }
    .action-card:hover { border-color: rgba(114,231,255,.4); text-decoration: none; transform: translateY(-4px); }
    .action-card mat-icon { color: #72e7ff; font-size: 2rem; height: 2rem; width: 2rem; }
    .action-card span { display: grid; gap: .35rem; }
    .action-card small { color: #92a0b5; line-height: 1.4; }
  `]
})
export class HomeComponent {
  readonly authStore = inject(AuthStore);
}
