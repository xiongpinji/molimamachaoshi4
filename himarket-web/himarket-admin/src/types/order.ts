export interface PaymentRecord {
  paymentRecordId: string;
  orderId: string;
  provider: string;
  providerTransactionId?: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
  callbackPayload?: string;
  createAt?: string;
  updatedAt?: string;
}

export interface ProductOrder {
  orderId: string;
  consumerId: string;
  productId: string;
  productName: string;
  amount: number;
  currency: string;
  pricingMode: 'ONE_TIME' | 'PERIODIC';
  status: 'PENDING_PAYMENT' | 'PAID' | 'FAILED' | 'CANCELLED';
  paymentRecords?: PaymentRecord[];
  paidAt?: string;
  createAt?: string;
  updatedAt?: string;
}

export interface ProductOrderModalProps {
  visible: boolean;
  consumerId: string;
  consumerName: string;
  onCancel: () => void;
}
