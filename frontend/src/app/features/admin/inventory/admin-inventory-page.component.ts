import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AdminInventoryStore } from '../../../core/inventory/admin-inventory.store';
import { InventoryItem } from '../../../core/inventory/inventory.models';
import { InventoryAdjustmentFormComponent } from './inventory-adjustment-form.component';

@Component({
  selector: 'app-admin-inventory-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatCardModule, MatPaginatorModule, MatProgressBarModule, InventoryAdjustmentFormComponent],
  templateUrl: './admin-inventory-page.component.html',
  styleUrl: './admin-inventory-page.component.scss'
})
export class AdminInventoryPageComponent implements OnInit {
  readonly adminInventoryStore = inject(AdminInventoryStore);

  async ngOnInit(): Promise<void> {
    await this.adminInventoryStore.loadInventory();
  }

  async changePage(event: PageEvent): Promise<void> {
    await this.adminInventoryStore.loadInventory({
      page: event.pageIndex,
      size: event.pageSize
    });
  }

  selectInventory(item: InventoryItem): void {
    this.adminInventoryStore.selectInventory(item);
  }
}
