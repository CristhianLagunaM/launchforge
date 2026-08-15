import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetails } from '../auth/auth.models';

export interface InventoryCapacityConflict {
  productId: string;
  sku: string;
  productName: string;
  availableQuantity: number;
  requestedQuantity: number;
}

export function describeInventoryCapacityConflict(error: unknown): InventoryCapacityConflict | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const problem = error.error as ProblemDetails | undefined;
  if (
    !problem?.type?.endsWith('/inventory/insufficient-capacity') ||
    !problem.productId ||
    !problem.sku ||
    !problem.productName ||
    !Number.isInteger(problem.availableQuantity) ||
    !Number.isInteger(problem.requestedQuantity)
  ) {
    return null;
  }
  return {
    productId: problem.productId,
    sku: problem.sku,
    productName: problem.productName,
    availableQuantity: problem.availableQuantity as number,
    requestedQuantity: problem.requestedQuantity as number
  };
}

export function describeHttpError(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'No hay conexión con el servidor. Revisa tu red e intenta nuevamente.';
  }

  const problem = error.error as ProblemDetails | undefined;
  const problemType = problem?.type ?? '';
  const problemTitle = problem?.title?.toLowerCase() ?? '';

  if (problemType.endsWith('/inventory/insufficient-capacity') || problemTitle === 'insufficient inventory') {
    return 'No hay capacidad suficiente para completar uno de los productos. Ajusta la cantidad en el carrito e intenta nuevamente.';
  }

  if (problemType.endsWith('/orders/product-inactive')) {
    return 'Uno de los productos ya no está disponible. Retíralo del carrito para continuar.';
  }

  const detail = problem?.detail ?? problem?.title;
  if (detail) {
    return detail;
  }

  switch (error.status) {
    case 401: return 'Tu sesión expiró. Inicia sesión nuevamente.';
    case 403: return 'No tienes permisos para realizar esta operación.';
    case 409: return 'Los datos cambiaron mientras trabajabas. Recarga y vuelve a intentarlo.';
    case 422:
    case 400: return 'Revisa los datos ingresados e intenta nuevamente.';
    default: return fallback;
  }
}
