import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { ReportStore } from '../../../core/reports/report.store';

@Component({
  selector: 'app-admin-reports-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, MatCardModule, MatProgressBarModule, MatTableModule],
  templateUrl: './admin-reports-page.component.html',
  styleUrl: './admin-reports-page.component.scss'
})
export class AdminReportsPageComponent implements OnInit {
  readonly reportStore = inject(ReportStore);
  readonly activeProductColumns = ['sku', 'name', 'category', 'price'];
  readonly topProductColumns = ['position', 'sku', 'name', 'quantitySold'];
  readonly topCustomerColumns = ['position', 'customer', 'email', 'orderCount'];

  async ngOnInit(): Promise<void> {
    await this.reportStore.load();
  }
}

