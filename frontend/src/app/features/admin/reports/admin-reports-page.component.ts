import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { ReportStore } from '../../../core/reports/report.store';

@Component({
  selector: 'app-admin-reports-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule
  ],
  templateUrl: './admin-reports-page.component.html',
  styleUrl: './admin-reports-page.component.scss'
})
export class AdminReportsPageComponent implements OnInit {
  readonly reportStore = inject(ReportStore);
  readonly activeProductColumns = ['sku', 'name', 'category', 'price'];

  async ngOnInit(): Promise<void> {
    await this.reportStore.load();
  }

  productBarWidth(quantitySold: number): number {
    return this.relativeWidth(
      quantitySold,
      this.reportStore.topProducts().map((item) => item.quantitySold)
    );
  }

  customerBarWidth(orderCount: number): number {
    return this.relativeWidth(
      orderCount,
      this.reportStore.topCustomers().map((item) => item.orderCount)
    );
  }

  revenueBarHeight(revenue: number): number {
    return this.relativeWidth(
      revenue,
      this.reportStore.dashboard()?.monthlyRevenue.map((item) => item.revenue) ?? []
    );
  }

  monthLabel(period: string): string {
    const [year, month] = period.split('-').map(Number);

    if (!year || !month) {
      return period;
    }

    return new Intl.DateTimeFormat('es-CO', {
      month: 'short',
      timeZone: 'UTC'
    })
      .format(new Date(Date.UTC(year, month - 1, 1)))
      .replace('.', '');
  }

  private relativeWidth(value: number, values: number[]): number {
    const maximum = Math.max(...values, 0);

    return maximum === 0
      ? 0
      : Math.max((value / maximum) * 100, 8);
  }
}
