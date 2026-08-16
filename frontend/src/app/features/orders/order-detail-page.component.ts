import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { OrdersStore } from '../../core/orders/orders.store';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-order-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatCardModule, MatProgressBarModule],
  templateUrl: './order-detail-page.component.html',
  styleUrl: './order-detail-page.component.scss'
})
export class OrderDetailPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  readonly ordersStore = inject(OrdersStore);
  readonly authStore = inject(AuthStore);

  statusLabel(status: string): string { return ({ CREATED: 'Pendiente de confirmación', CONFIRMED: 'Confirmada', CANCELLED: 'Cancelada', COMPLETED: 'Completada' } as Record<string, string>)[status] ?? status; }
  discountLabel(code: string): string { return ({ TIME_RANGE: 'Promoción por horario', RANDOM_ORDER: 'Orden seleccionada', FREQUENT_CUSTOMER: 'Cliente frecuente' } as Record<string, string>)[code] ?? code; }

  async ngOnInit(): Promise<void> {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      await this.ordersStore.loadOrder(orderId);
    }
  }

  ngOnDestroy(): void {
    this.ordersStore.clearSelectedOrder();
    this.ordersStore.clearLatestCreatedOrder();
  }
}
