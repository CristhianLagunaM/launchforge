import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { describeHttpError } from './http-error.util';

describe('describeHttpError', () => {
  it('hides inventory identifiers behind an actionable customer message', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: {
        type: 'https://launchforge/errors/inventory/insufficient-capacity',
        title: 'Insufficient inventory',
        detail: 'Insufficient inventory for product 22222222-2222-2222-2222-222222222222. Available: 2, requested: 5.',
        productId: '22222222-2222-2222-2222-222222222222',
        sku: 'LF-CORP-001',
        productName: 'Corporate Website Suite',
        availableQuantity: 2,
        requestedQuantity: 5
      }
    });

    const description = describeHttpError(error, 'Fallback');

    expect(description).toBe(
      'No hay capacidad suficiente para completar uno de los productos. Ajusta la cantidad en el carrito e intenta nuevamente.'
    );
    expect(description).not.toContain('22222222');
  });
});
