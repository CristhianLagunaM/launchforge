import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { OrdersStore } from '../../core/orders/orders.store';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-orders-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink, MatButtonModule, MatCardModule, MatProgressBarModule],
  templateUrl: './orders-page.component.html',
  styleUrl: './orders-page.component.scss'
})
export class OrdersPageComponent implements OnInit {
  readonly ordersStore = inject(OrdersStore);
  readonly authStore = inject(AuthStore);
  statusLabel(status: string): string { return ({ CREATED: 'Pendiente de confirmación', CONFIRMED: 'Confirmada', CANCELLED: 'Cancelada', COMPLETED: 'Completada' } as Record<string, string>)[status] ?? status; }
  readonly query = signal('');
  readonly filteredOrders = computed(() => { const q = this.query().trim().toLowerCase(); return !q ? this.ordersStore.orders() : this.ordersStore.orders().filter((o) => `${o.orderNumber} ${o.status}`.toLowerCase().includes(q)); });

  async ngOnInit(): Promise<void> {
    await (this.authStore.isAdmin() ? this.ordersStore.loadAllOrders() : this.ordersStore.loadOrders());
  }
}
