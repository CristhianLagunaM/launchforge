import { DatePipe, JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { AuditStore } from '../../../core/audit/audit.store';

@Component({
  selector: 'app-admin-audit-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, JsonPipe, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatPaginatorModule, MatProgressBarModule, MatTableModule],
  templateUrl: './admin-audit-page.component.html',
  styleUrl: './admin-audit-page.component.scss'
})
export class AdminAuditPageComponent implements OnInit {
  readonly store = inject(AuditStore);
  readonly columns = ['createdAt', 'action', 'resource', 'actor', 'correlationId', 'metadata'];
  readonly filters = new FormGroup({
    action: new FormControl('', { nonNullable: true }),
    resourceType: new FormControl('', { nonNullable: true }),
    actor: new FormControl('', { nonNullable: true }),
    from: new FormControl('', { nonNullable: true }),
    to: new FormControl('', { nonNullable: true })
  });

  async ngOnInit(): Promise<void> { await this.search(0); }

  async search(page = 0, size = this.store.size()): Promise<void> {
    const values = this.filters.getRawValue();
    await this.store.load({
      action: values.action.trim() || undefined,
      resourceType: values.resourceType.trim() || undefined,
      actor: values.actor.trim() || undefined,
      from: toIso(values.from), to: toIso(values.to), page, size
    });
  }

  async pageChanged(event: PageEvent): Promise<void> { await this.search(event.pageIndex, event.pageSize); }

  async clear(): Promise<void> { this.filters.reset(); await this.search(0); }
}

function toIso(value: string): string | undefined {
  return value ? new Date(value).toISOString() : undefined;
}
