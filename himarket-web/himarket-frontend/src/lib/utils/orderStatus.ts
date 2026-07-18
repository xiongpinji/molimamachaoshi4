import type { TFunction } from 'i18next';

const ORDER_STATUS_COLORS: Record<string, string> = {
  CANCELLED: 'default',
  FAILED: 'red',
  PAID: 'green',
  PENDING_PAYMENT: 'orange',
};

export function getOrderStatusColor(status: string) {
  return ORDER_STATUS_COLORS[status] || 'default';
}

export function getOrderStatusLabel(status: string, t: TFunction) {
  const key = `orderStatusLabels.${status}`;
  const translated = t(key);
  return translated === key ? status : translated;
}
