import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
  inject
} from '@angular/core';

import {
  FormBuilder,
  FormGroupDirective,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  Category,
  Product,
  ProductUpsertPayload
} from '../../../core/catalog/catalog.models';

@Component({
  selector: 'app-product-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss'
})
export class ProductFormComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @ViewChild(FormGroupDirective)
  private formDirective?: FormGroupDirective;

  @Input() categories: Category[] = [];
  @Input() product: Product | null = null;
  @Input() saving = false;

  @Output() saveProduct =
    new EventEmitter<ProductUpsertPayload>();

  @Output() cancelEdit =
    new EventEmitter<void>();

  readonly form = this.formBuilder.nonNullable.group({
    sku: [
      '',
      [
        Validators.required,
        Validators.maxLength(50)
      ]
    ],
    name: [
      '',
      [
        Validators.required,
        Validators.maxLength(180)
      ]
    ],
    slug: [
      '',
      [
        Validators.required,
        Validators.maxLength(200)
      ]
    ],
    description: [
      '',
      [
        Validators.required
      ]
    ],
    categoryId: [
      0,
      [
        Validators.min(1)
      ]
    ],
    price: [
      0,
      [
        Validators.required,
        Validators.min(0)
      ]
    ]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['product']) {
      return;
    }

    const product =
      changes['product'].currentValue as Product | null;

    if (!product) {
      this.resetForm();
      return;
    }

    this.form.reset({
      sku: product.sku,
      name: product.name,
      slug: product.slug,
      description: product.description,
      categoryId: product.category.id,
      price: product.price
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saveProduct.emit(
      this.form.getRawValue()
    );
  }

  resetForm(): void {
    const initialValues = {
      sku: '',
      name: '',
      slug: '',
      description: '',
      categoryId: 0,
      price: 0
    };

    if (this.formDirective) {
      this.formDirective.resetForm(initialValues);
    } else {
      this.form.reset(initialValues);
    }

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }
}