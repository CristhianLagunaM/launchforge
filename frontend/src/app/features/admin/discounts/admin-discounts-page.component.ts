import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AdminDiscountStore } from '../../../core/discounts/admin-discount.store';
import { DiscountConfiguration } from '../../../core/discounts/discount.models';

@Component({
  selector: 'app-admin-discounts-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule
  ],
  templateUrl: './admin-discounts-page.component.html',
  styleUrl: './admin-discounts-page.component.scss'
})
export class AdminDiscountsPageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  readonly adminDiscountStore = inject(AdminDiscountStore);

  readonly configurationForm = this.formBuilder.group({
    enabled: this.formBuilder.nonNullable.control(false),
    percentage: this.formBuilder.nonNullable.control(0, [Validators.min(0), Validators.max(100)]),
    startAt: [''],
    endAt: [''],
    minimumOrders: [null as number | null],
    lookbackMonths: [null as number | null]
  });

  constructor() {
    effect(() => {
      const configuration = this.adminDiscountStore.selectedConfiguration();
      if (!configuration) {
        return;
      }
      this.configurationForm.reset(
        {
          enabled: configuration.enabled,
          percentage: configuration.percentage,
          startAt: toLocalDateTime(configuration.startAt),
          endAt: toLocalDateTime(configuration.endAt),
          minimumOrders: configuration.minimumOrders,
          lookbackMonths: configuration.lookbackMonths
        },
        { emitEvent: false }
      );
    });
  }

  async ngOnInit(): Promise<void> {
    await this.adminDiscountStore.loadConfigurations();
  }

  selectConfiguration(configuration: DiscountConfiguration): void {
    this.adminDiscountStore.selectConfiguration(configuration);
  }

  async save(): Promise<void> {
    if (this.configurationForm.invalid) {
      this.configurationForm.markAllAsTouched();
      return;
    }

    const rawValue = this.configurationForm.getRawValue();
    await this.adminDiscountStore.saveConfiguration({
      enabled: rawValue.enabled,
      percentage: Number(rawValue.percentage),
      startAt: toUtcInstant(rawValue.startAt),
      endAt: toUtcInstant(rawValue.endAt),
      minimumOrders: rawValue.minimumOrders,
      lookbackMonths: rawValue.lookbackMonths
    });
  }
}

function toLocalDateTime(value: string | null): string {
  if (!value) {
    return '';
  }
  return value.slice(0, 16);
}

function toUtcInstant(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  return new Date(value).toISOString();
}
