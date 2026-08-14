import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CatalogStore } from '../../core/catalog/catalog.store';
import { CartStore } from '../../core/orders/cart.store';

@Component({
  selector: 'app-product-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss'
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  readonly catalogStore = inject(CatalogStore);
  readonly cartStore = inject(CartStore);

  async ngOnInit(): Promise<void> {
    const productId = this.route.snapshot.paramMap.get('id');
    if (productId) {
      await this.catalogStore.loadProduct(productId);
    }
  }

  ngOnDestroy(): void {
    this.catalogStore.clearSelectedProduct();
  }

  addSelectedProductToCart(): void {
    const product = this.catalogStore.selectedProduct();
    if (!product) {
      return;
    }

    this.cartStore.addItem({
      productId: product.id,
      sku: product.sku,
      name: product.name,
      price: product.price
    });
  }
}
