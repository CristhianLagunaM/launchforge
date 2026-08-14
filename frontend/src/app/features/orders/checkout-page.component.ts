import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CartStore } from '../../core/orders/cart.store';
import { OrdersStore } from '../../core/orders/orders.store';

@Component({
  selector: 'app-checkout-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, RouterLink, MatButtonModule, MatCardModule],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss'
})
export class CheckoutPageComponent {
  private readonly router = inject(Router);
  readonly cartStore = inject(CartStore);
  readonly ordersStore = inject(OrdersStore);

  async submitOrder(): Promise<void> {
    if (this.cartStore.isEmpty() || this.ordersStore.submitting()) {
      return;
    }

    const idempotencyKey = this.cartStore.ensureCheckoutKey();
    const order = await this.ordersStore.createOrder(
      {
        items: this.cartStore.items().map((item) => ({
          productId: item.productId,
          quantity: item.quantity
        }))
      },
      idempotencyKey
    );

    if (order) {
      this.cartStore.completeCheckout();
      await this.router.navigate(['/orders', order.id]);
    }
  }
}
