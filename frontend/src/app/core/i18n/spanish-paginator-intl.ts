import { MatPaginatorIntl } from '@angular/material/paginator';

export function spanishPaginatorIntl(): MatPaginatorIntl {
  const paginator = new MatPaginatorIntl();
  paginator.itemsPerPageLabel = 'Elementos por página';
  paginator.nextPageLabel = 'Página siguiente';
  paginator.previousPageLabel = 'Página anterior';
  paginator.firstPageLabel = 'Primera página';
  paginator.lastPageLabel = 'Última página';
  paginator.getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0 || pageSize === 0) {
      return `0 de ${length}`;
    }
    const start = page * pageSize;
    const end = Math.min(start + pageSize, length);
    return `${start + 1}–${end} de ${length}`;
  };
  return paginator;
}
