import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  createProductOrder: vi.fn(),
  getActiveProductOrder: vi.fn(),
  getConsumers: vi.fn(),
  getPrimaryConsumer: vi.fn(),
  getProductSubscriptionStatus: vi.fn(),
  getProductSubscriptions: vi.fn(),
  messageError: vi.fn(),
  messageSuccess: vi.fn(),
  messageWarning: vi.fn(),
  navigate: vi.fn(),
  subscribeProduct: vi.fn(),
  unsubscribeProduct: vi.fn(),
}));

vi.mock('./LoginPrompt', () => ({
  LoginPrompt: ({ open }: { open: boolean }) => (open ? <div>login prompt</div> : null),
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ isLoggedIn: true }),
}));

vi.mock('../lib/apis', () => ({
  __esModule: true,
  createProductOrder: (consumerId: string, productId: string) =>
    mocks.createProductOrder(consumerId, productId),
  default: {
    createProductOrder: (consumerId: string, productId: string) =>
      mocks.createProductOrder(consumerId, productId),
    getActiveProductOrder: (consumerId: string, productId: string) =>
      mocks.getActiveProductOrder(consumerId, productId),
    getPrimaryConsumer: () => mocks.getPrimaryConsumer(),
    getProductSubscriptionStatus: (productId: string) =>
      mocks.getProductSubscriptionStatus(productId),
  },
  getActiveProductOrder: (consumerId: string, productId: string) =>
    mocks.getActiveProductOrder(consumerId, productId),
  getConsumers: (...args: unknown[]) => mocks.getConsumers(...args),
  getPrimaryConsumer: () => mocks.getPrimaryConsumer(),
  getProductSubscriptions: (...args: unknown[]) => mocks.getProductSubscriptions(...args),
  subscribeProduct: (...args: unknown[]) => mocks.subscribeProduct(...args),
  unsubscribeProduct: (...args: unknown[]) => mocks.unsubscribeProduct(...args),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mocks.navigate,
  useParams: () => ({ agentProductId: 'product-a' }),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    i18n: { language: 'zh-CN' },
    t: (key: string, options?: Record<string, string>) => {
      if (key === 'updatedAt') return `${options?.date ?? ''} 更新`;
      return key;
    },
  }),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    message: {
      error: mocks.messageError,
      success: mocks.messageSuccess,
      warning: mocks.messageWarning,
    },
  };
});

import { ProductHeader } from './ProductHeader';

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('ProductHeader purchase flow', () => {
  it('付费商品可通过默认消费者创建订单并跳转到订单详情', async () => {
    mocks.getProductSubscriptionStatus.mockResolvedValue({
      fullSubscriptionData: { content: [], totalElements: 0, totalPages: 0 },
      hasSubscription: false,
      subscribedConsumers: [],
    });
    mocks.getPrimaryConsumer.mockResolvedValue({
      data: {
        consumerId: 'consumer-a',
        name: 'Primary Consumer',
      },
    });
    mocks.getActiveProductOrder.mockResolvedValue(null);
    mocks.createProductOrder.mockResolvedValue({
      data: {
        consumerId: 'consumer-a',
        orderId: 'order-a',
        status: 'PENDING_PAYMENT',
      },
    });

    render(
      <ProductHeader
        commerceConfig={{ amount: 99.9, currency: 'CNY', enabled: true, pricingMode: 'ONE_TIME' }}
        description="paid agent"
        name="Legal Assistant"
        productType="AGENT_API"
        subscribable={true}
        updatedAt="2026-07-14T00:00:00"
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('purchase.available')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'purchase.action' }));

    await waitFor(() => {
      expect(mocks.createProductOrder).toHaveBeenCalledWith('consumer-a', 'product-a');
    });

    expect(mocks.navigate).toHaveBeenCalledWith('/orders/order-a');
  });

  it('存在待支付活跃订单时显示继续支付并跳转原订单', async () => {
    mocks.getProductSubscriptionStatus.mockResolvedValue({
      fullSubscriptionData: { content: [], totalElements: 0, totalPages: 0 },
      hasSubscription: false,
      subscribedConsumers: [],
    });
    mocks.getPrimaryConsumer.mockResolvedValue({ data: { consumerId: 'consumer-a' } });
    mocks.getActiveProductOrder.mockResolvedValue({
      orderId: 'order-a',
      productId: 'product-a',
      productName: 'Legal Assistant',
      amount: 99.9,
      currency: 'CNY',
      pricingMode: 'ONE_TIME',
      status: 'PENDING_PAYMENT',
    });

    render(
      <ProductHeader
        commerceConfig={{ amount: 99.9, currency: 'CNY', enabled: true, pricingMode: 'ONE_TIME' }}
        description="paid agent"
        name="Legal Assistant"
        productType="AGENT_API"
        subscribable={true}
        updatedAt="2026-07-14T00:00:00"
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('purchase.continuePayment')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'purchase.viewOrder' }));

    expect(mocks.navigate).toHaveBeenCalledWith('/orders/order-a');
  });

  it('创建订单失败后会回查活跃订单并跳转原订单', async () => {
    mocks.getProductSubscriptionStatus.mockResolvedValue({
      fullSubscriptionData: { content: [], totalElements: 0, totalPages: 0 },
      hasSubscription: false,
      subscribedConsumers: [],
    });
    mocks.getPrimaryConsumer.mockResolvedValue({ data: { consumerId: 'consumer-a' } });
    mocks.getActiveProductOrder.mockResolvedValueOnce(null).mockResolvedValueOnce({
      orderId: 'order-b',
      productId: 'product-a',
      productName: 'Legal Assistant',
      amount: 99.9,
      currency: 'CNY',
      pricingMode: 'ONE_TIME',
      status: 'PENDING_PAYMENT',
    });
    mocks.createProductOrder.mockRejectedValue(new Error('duplicate active order'));

    render(
      <ProductHeader
        commerceConfig={{ amount: 99.9, currency: 'CNY', enabled: true, pricingMode: 'ONE_TIME' }}
        description="paid agent"
        name="Legal Assistant"
        productType="AGENT_API"
        subscribable={true}
        updatedAt="2026-07-14T00:00:00"
      />,
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'purchase.action' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'purchase.action' }));

    await waitFor(() => {
      expect(mocks.navigate).toHaveBeenCalledWith('/orders/order-b');
    });
    expect(mocks.messageWarning).toHaveBeenCalledWith('purchase.existingOrder');
  });
});
