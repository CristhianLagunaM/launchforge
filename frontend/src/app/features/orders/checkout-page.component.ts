import { CurrencyPipe } from '@angular/common';

import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject
} from '@angular/core';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  Router,
  RouterLink
} from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { CartStore } from '../../core/orders/cart.store';
import { OrdersStore } from '../../core/orders/orders.store';

@Component({
  selector: 'app-checkout-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss'
})
export class CheckoutPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly cartStore = inject(CartStore);
  readonly ordersStore = inject(OrdersStore);

  readonly requirementsForm =
    this.formBuilder.nonNullable.group({
      requirementDescription: [
        '',
        [
          Validators.required,
          Validators.maxLength(3000)
        ]
      ],

      projectObjective: [
        '',
        [
          Validators.required,
          Validators.maxLength(1000)
        ]
      ],

      contactEmail: [
        '',
        [
          Validators.required,
          Validators.email,
          Validators.maxLength(180)
        ]
      ],

      contactPhone: [
        '',
        [
          Validators.maxLength(40)
        ]
      ],

      desiredDeliveryDate: [''],

      referencesUrl: [
        '',
        [
          Validators.maxLength(2000)
        ]
      ]
    });

  ngOnInit(): void {
    this.ordersStore.clearCreationFeedback();
  }

  async submitOrder(): Promise<void> {
    if (
      this.cartStore.isEmpty() ||
      this.ordersStore.submitting()
    ) {
      return;
    }

    if (this.requirementsForm.invalid) {
      this.requirementsForm.markAllAsTouched();
      return;
    }

    const requirements =
      this.requirementsForm.getRawValue();

    const idempotencyKey =
      this.cartStore.ensureCheckoutKey();

    const order =
      await this.ordersStore.createOrder(
        {
          items: this.cartStore
            .items()
            .map((item) => ({
              productId: item.productId,
              quantity: item.quantity
            })),

          requirementDescription:
            requirements.requirementDescription,

          projectObjective:
            requirements.projectObjective,

          contactEmail:
            requirements.contactEmail,

          contactPhone:
            requirements.contactPhone || undefined,

          desiredDeliveryDate:
            requirements.desiredDeliveryDate || undefined,

          referencesUrl:
            requirements.referencesUrl || undefined
        },
        idempotencyKey
      );

    if (order) {
      this.cartStore.completeCheckout();

      await this.router.navigate([
        '/orders',
        order.id
      ]);
    }
  }
}
