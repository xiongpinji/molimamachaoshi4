import { describe, expect, it } from 'vitest';

import { getOrderStatusColor, getOrderStatusLabel } from '../orderStatus';

describe('orderStatus helpers', () => {
  it('maps paid status to green', () => {
    expect(getOrderStatusColor('PAID')).toBe('green');
  });

  it('falls back to raw status when translation key is unavailable', () => {
    const t = (key: string) => key;
    expect(getOrderStatusLabel('PAID', t as never)).toBe('PAID');
  });

  it('uses translated label when provided', () => {
    const t = (key: string) => (key === 'orderStatusLabels.PAID' ? '已支付' : key);
    expect(getOrderStatusLabel('PAID', t as never)).toBe('已支付');
  });
});
