import { PageResponse } from '../catalog/catalog.models';

export type InventoryAdjustmentOperation = 'INCREASE' | 'DECREASE' | 'RESTORE';

export interface InventoryItem {
  productId: string;
  sku: string;
  productName: string;
  productActive: boolean;
  availableQuantity: number;
  reservedQuantity: number;
  version: number;
  updatedAt: string;
}

export interface InventoryAdjustmentPayload {
  operation: InventoryAdjustmentOperation;
  quantity: number;
  version: number;
}

export type InventoryPageResponse = PageResponse<InventoryItem>;
