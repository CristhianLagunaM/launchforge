import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CatalogStore } from '../../core/catalog/catalog.store';
import { catalogCategoryIcon } from '../../core/catalog/catalog-icon.util';
import { CartStore } from '../../core/orders/cart.store';
import { Product } from '../../core/catalog/catalog.models';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-catalog-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSnackBarModule
  ],
  templateUrl: './catalog-page.component.html',
  styleUrl: './catalog-page.component.scss'
})
export class CatalogPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly authStore = inject(AuthStore);
  readonly catalogStore = inject(CatalogStore);
  readonly cartStore = inject(CartStore);

  readonly filtersForm = this.formBuilder.nonNullable.group({
    name: [''],
    sku: [''],
    category: [''],
    minPrice: [''],
    maxPrice: [''],
    available: ['']
  });

  async ngOnInit(): Promise<void> {
    await this.catalogStore.loadCategories();
    await this.catalogStore.loadProducts();
  }

  async search(): Promise<void> {
    await this.catalogStore.loadProducts({
      filters: this.toFilters(),
      page: 0
    });
  }

  async resetFilters(): Promise<void> {
    this.filtersForm.reset({
      name: '',
      sku: '',
      category: '',
      minPrice: '',
      maxPrice: '',
      available: ''
    });
    await this.catalogStore.loadProducts({ filters: {}, page: 0 });
  }

  async changePage(event: PageEvent): Promise<void> {
    await this.catalogStore.loadProducts({
      filters: this.toFilters(),
      page: event.pageIndex,
      size: event.pageSize
    });
  }

  addToCart(product: Product): void {
    if (!this.authStore.isAuthenticated()) {
      void this.router.navigate(['/login'], { queryParams: { redirectUrl: '/catalog' } });
      return;
    }
    if (!product.available) {
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

  private toFilters() {
    const raw = this.filtersForm.getRawValue();
    return {
      name: raw.name || undefined,
      sku: raw.sku || undefined,
      category: raw.category || undefined,
      minPrice: raw.minPrice ? Number(raw.minPrice) : null,
      maxPrice: raw.maxPrice ? Number(raw.maxPrice) : null,
      available: raw.available === '' ? null : raw.available === 'true'
    };
  }
}
