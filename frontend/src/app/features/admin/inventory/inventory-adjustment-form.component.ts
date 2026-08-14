import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { InventoryAdjustmentPayload, InventoryItem } from '../../../core/inventory/inventory.models';

@Component({
  selector: 'app-inventory-adjustment-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './inventory-adjustment-form.component.html',
  styleUrl: './inventory-adjustment-form.component.scss'
})
export class InventoryAdjustmentFormComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() inventory: InventoryItem | null = null;
  @Input() saving = false;

  @Output() submitAdjustment = new EventEmitter<InventoryAdjustmentPayload>();

  readonly form = this.formBuilder.nonNullable.group({
    operation: ['INCREASE' as InventoryAdjustmentPayload['operation'], [Validators.required]],
    quantity: [1, [Validators.required, Validators.min(1)]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['inventory'] && !changes['inventory'].firstChange) {
      this.form.reset({
        operation: 'INCREASE',
        quantity: 1
      });
    }
  }

  submit(): void {
    if (this.form.invalid || !this.inventory) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitAdjustment.emit({
      operation: this.form.getRawValue().operation,
      quantity: this.form.getRawValue().quantity,
      version: this.inventory.version
    });
  }
}
