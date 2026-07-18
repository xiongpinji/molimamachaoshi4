import { describe, expect, it } from 'vitest';

import { resolveProductAccessState } from '../productAccessState';

describe('resolveProductAccessState', () => {
  it('为未登录的付费商品返回登录后购买状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: false,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: false,
        isLoggedIn: false,
        isPaid: true,
        subscribable: true,
      }),
    ).toBe('purchase_login');
  });

  it('为未购买的付费商品返回可购买状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: false,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: false,
        isLoggedIn: true,
        isPaid: true,
        subscribable: true,
      }),
    ).toBe('purchase_available');
  });

  it('为存在待支付订单的付费商品返回继续支付状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: false,
        hasPendingPaymentOrder: true,
        hasPendingSubscription: false,
        isLoggedIn: true,
        isPaid: true,
        subscribable: true,
      }),
    ).toBe('purchase_continue_payment');
  });

  it('为存在已支付订单的付费商品返回开通处理中状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: true,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: false,
        isLoggedIn: true,
        isPaid: true,
        subscribable: true,
      }),
    ).toBe('purchase_activation_processing');
  });

  it('为待审批的付费商品返回购买处理中状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: false,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: true,
        isLoggedIn: true,
        isPaid: true,
        subscribable: true,
      }),
    ).toBe('purchase_pending');
  });

  it('为已开通的付费商品返回已开通状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: true,
        hasPaidOrder: false,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: false,
        isLoggedIn: true,
        isPaid: true,
        subscribable: true,
      }),
    ).toBe('purchase_active');
  });

  it('为未登录的免费商品返回登录后订阅状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: false,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: false,
        isLoggedIn: false,
        isPaid: false,
        subscribable: true,
      }),
    ).toBe('subscribe_login');
  });

  it('为不可订阅商品返回开放访问状态', () => {
    expect(
      resolveProductAccessState({
        hasApprovedSubscription: false,
        hasPaidOrder: false,
        hasPendingPaymentOrder: false,
        hasPendingSubscription: false,
        isLoggedIn: true,
        isPaid: true,
        subscribable: false,
      }),
    ).toBe('open_access');
  });
});
