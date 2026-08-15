import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catalogCategoryIcon } from '../../core/catalog/catalog-icon.util';
import { CatalogStore } from '../../core/catalog/catalog.store';
import { CartStore } from '../../core/orders/cart.store';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-product-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule, MatSnackBarModule],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss'
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly authStore = inject(AuthStore);
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
    if (!product || !product.available) {
      return;
    }

    if (!this.authStore.isAuthenticated()) {
      void this.router.navigate(['/login'], { queryParams: { redirectUrl: `/products/${product.id}` } });
      return;
    }

    this.cartStore.addItem({
      productId: product.id,
      sku: product.sku,
      name: product.name,
      price: product.price
    });
    this.snackBar.open(`${product.name} se agregó al carrito`, 'Cerrar', {
      duration: 3200,
      politeness: 'polite',
      panelClass: ['cart-success-snackbar']
    });
  }

  productIcon(categorySlug: string): string {
    return catalogCategoryIcon(categorySlug);
  }
}
