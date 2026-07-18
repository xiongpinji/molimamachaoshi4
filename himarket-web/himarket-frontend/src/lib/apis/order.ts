import request, { type RespI } from '../request';

import type { ICommercePricingMode } from './typing';

export interface PaymentRecord {
  paymentRecordId: string;
  orderId: string;
  provider: string;
  providerTransactionId?: string | null;
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
  callbackPayload?: string | null;
  createAt?: string;
  updatedAt?: string;
}

export interface ProductOrder {
  orderId: string;
  productId: string;
  productType?: string;
  consumerId: string;
  developerId?: string;
  portalId?: string;
  productName?: string;
  amount: number;
  currency: string;
  pricingMode: ICommercePricingMode;
  status: 'PENDING_PAYMENT' | 'PAID' | 'FAILED' | 'CANCELLED';
  paymentRecords?: PaymentRecord[];
  paidAt?: string | null;
  createAt?: string;
  updatedAt?: string;
}

interface ProductOrderListResponse {
  content: ProductOrder[];
  number: number;
  size: number;
  totalElements: number;
}

interface ProductOrderListParams {
  page?: number;
  size?: number;
  productId?: string;
  productName?: string;
  status?: string;
  activeOnly?: boolean;
}

export function createProductOrder(consumerId: string, productId: string) {
  return request.post<RespI<ProductOrder>, RespI<ProductOrder>>(`/consumers/${consumerId}/orders`, {
    productId,
  });
}

export function markProductOrderPaid(consumerId: string, orderId: string) {
  return request.patch<RespI<ProductOrder>, RespI<ProductOrder>>(
    `/consumers/${consumerId}/orders/${orderId}/paid`,
  );
}

export function cancelProductOrder(consumerId: string, orderId: string) {
  return request.patch<RespI<ProductOrder>, RespI<ProductOrder>>(
    `/consumers/${consumerId}/orders/${orderId}/cancel`,
  );
}

export function getProductOrder(consumerId: string, orderId: string) {
  return request.get<RespI<ProductOrder>, RespI<ProductOrder>>(
    `/consumers/${consumerId}/orders/${orderId}`,
  );
}

export function getProductOrders(consumerId: string, params?: ProductOrderListParams) {
  return request.get<RespI<ProductOrderListResponse>, RespI<ProductOrderListResponse>>(
    `/consumers/${consumerId}/orders`,
    {
      params: {
        activeOnly: params?.activeOnly,
        page: params?.page ?? 1,
        productId: params?.productId,
        productName: params?.productName,
        size: params?.size ?? 10,
        status: params?.status,
      },
    },
  );
}

export async function getActiveProductOrder(consumerId: string, productId: string) {
  const response = await getProductOrders(consumerId, {
    activeOnly: true,
    page: 1,
    productId,
    size: 1,
  });
  return response.data?.content?.[0] ?? null;
}
