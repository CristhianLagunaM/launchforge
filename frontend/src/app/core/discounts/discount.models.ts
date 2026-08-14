export interface DiscountConfiguration {
  id: string;
  code: string;
  type: string;
  enabled: boolean;
  percentage: number;
  startAt: string | null;
  endAt: string | null;
  minimumOrders: number | null;
  lookbackMonths: number | null;
  createdAt: string;
  updatedAt: string;
  updatedBy: string | null;
}

export interface DiscountConfigurationUpdatePayload {
  enabled: boolean;
  percentage: number;
  startAt: string | null;
  endAt: string | null;
  minimumOrders: number | null;
  lookbackMonths: number | null;
}
