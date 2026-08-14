import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { AdminProductStore } from '../../../core/catalog/admin-product.store';
import { Product } from '../../../core/catalog/catalog.models';
import { ProductFormComponent } from './product-form.component';

@Component({
  selector: 'app-admin-products-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    ProductFormComponent
  ],
  templateUrl: './admin-products-page.component.html',
  styleUrl: './admin-products-page.component.scss'
})
export class AdminProductsPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  readonly adminProductStore = inject(AdminProductStore);

  readonly filtersForm = this.formBuilder.nonNullable.group({
    name: [''],
    sku: [''],
    active: ['']
  });

  async ngOnInit(): Promise<void> {
    await this.adminProductStore.loadCategories();
    await this.adminProductStore.loadProducts();
  }

  async search(): Promise<void> {
    await this.adminProductStore.loadProducts({
      filters: {
        name: this.filtersForm.getRawValue().name || undefined,
        sku: this.filtersForm.getRawValue().sku || undefined,
        active: this.filtersForm.getRawValue().active === '' ? null : this.filtersForm.getRawValue().active === 'true'
      },
      page: 0
    });
  }

  async changePage(event: PageEvent): Promise<void> {
    await this.adminProductStore.loadProducts({
      page: event.pageIndex,
      size: event.pageSize
    });
  }

  editProduct(product: Product): void {
    this.adminProductStore.selectProduct(product);
  }

  newProduct(): void {
    this.adminProductStore.selectProduct(null);
  }
}
