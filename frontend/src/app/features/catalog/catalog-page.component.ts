import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { CatalogStore } from '../../core/catalog/catalog.store';

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
    MatSelectModule
  ],
  templateUrl: './catalog-page.component.html',
  styleUrl: './catalog-page.component.scss'
})
export class CatalogPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  readonly catalogStore = inject(CatalogStore);

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
