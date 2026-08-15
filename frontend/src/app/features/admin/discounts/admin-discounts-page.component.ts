import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
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
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatNativeDateModule,
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
    startDate: [null as Date | null],
    startTime: ['00:00'],
    endDate: [null as Date | null],
    endTime: ['23:59'],
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
          startDate: toLocalDate(configuration.startAt),
          startTime: toLocalTime(configuration.startAt, '00:00'),
          endDate: toLocalDate(configuration.endAt),
          endTime: toLocalTime(configuration.endAt, '23:59'),
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

  ruleName(code: string): string {
    const names: Record<string, string> = {
      FREQUENT_CUSTOMER: 'Cliente frecuente',
      RANDOM_ORDER: 'Orden aleatoria',
      TIME_RANGE: 'Rango de tiempo'
    };
    return names[code] ?? code;
  }

  ruleDescription(code: string): string {
    const descriptions: Record<string, string> = {
      FREQUENT_CUSTOMER: 'Premia a clientes con compras recurrentes.',
      RANDOM_ORDER: 'Aplica el beneficio aleatorio dentro de la ventana configurada.',
      TIME_RANGE: 'Aplica el descuento durante un periodo específico.'
    };
    return descriptions[code] ?? 'Regla de descuento configurable.';
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
      startAt: toUtcInstant(rawValue.startDate, rawValue.startTime),
      endAt: toUtcInstant(rawValue.endDate, rawValue.endTime),
      minimumOrders: rawValue.minimumOrders,
      lookbackMonths: rawValue.lookbackMonths
    });
  }
}

function toLocalDate(value: string | null): Date | null {
  if (!value) {
    return null;
  }
  return new Date(value);
}

function toLocalTime(value: string | null, fallback: string): string {
  if (!value) {
    return fallback;
  }
  const date = new Date(value);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

function toUtcInstant(date: Date | null | undefined, time: string | null | undefined): string | null {
  if (!date || !time) {
    return null;
  }
  const [hours, minutes] = time.split(':').map(Number);
  const combined = new Date(date.getFullYear(), date.getMonth(), date.getDate(), hours, minutes);
  return combined.toISOString();
}
