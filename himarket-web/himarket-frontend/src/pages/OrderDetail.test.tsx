import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  cancelProductOrder: vi.fn(),
  getPrimaryConsumer: vi.fn(),
  getProduct: vi.fn(),
  getProductOrder: vi.fn(),
  handlePaymentCallback: vi.fn(),
  markProductOrderPaid: vi.fn(),
  messageError: vi.fn(),
  messageSuccess: vi.fn(),
  navigate: vi.fn(),
  params: { orderId: 'order-a' },
}));

beforeAll(() => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn().mockImplementation((query) => ({
      addEventListener: vi.fn(),
      addListener: vi.fn(),
      dispatchEvent: vi.fn(),
      matches: false,
      media: query,
      onchange: null,
      removeEventListener: vi.fn(),
      removeListener: vi.fn(),
    })),
  });
});

vi.mock('../components/Layout', () => ({
  Layout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('../lib/apis', () => ({
  __esModule: true,
  default: {
    cancelProductOrder: (...args: unknown[]) => mocks.cancelProductOrder(...args),
    getPrimaryConsumer: () => mocks.getPrimaryConsumer(),
    getProduct: (...args: unknown[]) => mocks.getProduct(...args),
    getProductOrder: (...args: unknown[]) => mocks.getProductOrder(...args),
    handlePaymentCallback: (...args: unknown[]) => mocks.handlePaymentCallback(...args),
    markProductOrderPaid: (...args: unknown[]) => mocks.markProductOrderPaid(...args),
  },
}));

vi.mock('../lib/utils/orderStatus', () => ({
  getOrderStatusColor: (status: string) => (status === 'PAID' ? 'green' : 'orange'),
  getOrderStatusLabel: (status: string) =>
    status === 'PAID' ? '已支付' : status === 'PENDING_PAYMENT' ? '待支付' : status,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    message: {
      error: mocks.messageError,
      success: mocks.messageSuccess,
    },
  };
});

vi.mock('react-router-dom', () => ({
  useNavigate: () => mocks.navigate,
  useParams: () => mocks.params,
}));

import OrderDetailPage from './OrderDetail';

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('OrderDetailPage', () => {
  it('展示订单支付记录', async () => {
    mocks.getPrimaryConsumer.mockResolvedValue({ data: { consumerId: 'consumer-a' } });
    mocks.getProductOrder.mockResolvedValue({
      data: {
        amount: 99.9,
        consumerId: 'consumer-a',
        createAt: '2026-07-14T00:00:00',
        currency: 'CNY',
        orderId: 'order-a',
        paidAt: '2026-07-14T01:00:00',
        paymentRecords: [
          {
            amount: 99.9,
            callbackPayload: 'manual-confirmation',
            createAt: '2026-07-14T01:00:00',
            currency: 'CNY',
            orderId: 'order-a',
            paymentRecordId: 'pay-a',
            provider: 'MANUAL',
            status: 'SUCCEEDED',
          },
        ],
        pricingMode: 'ONE_TIME',
        productId: 'product-a',
        productName: 'Legal Assistant',
        status: 'PAID',
      },
    });
    mocks.getProduct.mockResolvedValue({
      data: { productId: 'product-a', type: 'AGENT_API' },
    });

    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Legal Assistant').length).toBeGreaterThan(0);
      expect(screen.getByText('paymentRecordsTitle')).toBeInTheDocument();
    });

    expect(screen.getByText((_, node) => node?.textContent === 'paymentPayload: manual-confirmation')).toBeInTheDocument();
    expect(screen.getByText((_, node) => node?.textContent === 'paymentStatus: SUCCEEDED')).toBeInTheDocument();
  });

  it('待支付订单可在详情页确认支付并写入回调占位结果', async () => {
    mocks.getPrimaryConsumer.mockResolvedValue({ data: { consumerId: 'consumer-a' } });
    mocks.getProductOrder.mockResolvedValue({
      data: {
        amount: 99.9,
        consumerId: 'consumer-a',
        createAt: '2026-07-14T00:00:00',
        currency: 'CNY',
        orderId: 'order-a',
        pricingMode: 'ONE_TIME',
        productId: 'product-a',
        productName: 'Legal Assistant',
        status: 'PENDING_PAYMENT',
      },
    });
    mocks.getProduct.mockResolvedValue({ data: { productId: 'product-a', type: 'AGENT_API' } });
    mocks.markProductOrderPaid.mockResolvedValue({
      data: {
        amount: 99.9,
        consumerId: 'consumer-a',
        currency: 'CNY',
        orderId: 'order-a',
        paidAt: '2026-07-14T01:00:00',
        paymentRecords: [
          {
            amount: 99.9,
            callbackPayload: 'manual-confirmation',
            createAt: '2026-07-14T01:00:00',
            currency: 'CNY',
            orderId: 'order-a',
            paymentRecordId: 'pay-a',
            provider: 'MANUAL',
            status: 'SUCCEEDED',
          },
        ],
        pricingMode: 'ONE_TIME',
        productId: 'product-a',
        productName: 'Legal Assistant',
        status: 'PAID',
      },
    });

    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('confirmPayment')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('confirmPayment'));

    await waitFor(() => {
      expect(mocks.markProductOrderPaid).toHaveBeenCalledWith('consumer-a', 'order-a');
    });

    await waitFor(() => {
      expect(mocks.getProductOrder).toHaveBeenCalledTimes(2);
      expect(mocks.messageSuccess).toHaveBeenCalledWith('orderPaySuccess');
    });
  });

  it('待支付订单可取消并刷新为已取消状态', async () => {
    mocks.getPrimaryConsumer.mockResolvedValue({ data: { consumerId: 'consumer-a' } });
    mocks.getProductOrder
      .mockResolvedValueOnce({
        data: {
          amount: 99.9,
          consumerId: 'consumer-a',
          currency: 'CNY',
          orderId: 'order-a',
          pricingMode: 'ONE_TIME',
          productId: 'product-a',
          productName: 'Legal Assistant',
          status: 'PENDING_PAYMENT',
        },
      })
      .mockResolvedValueOnce({
        data: {
          amount: 99.9,
          consumerId: 'consumer-a',
          currency: 'CNY',
          orderId: 'order-a',
          pricingMode: 'ONE_TIME',
          productId: 'product-a',
          productName: 'Legal Assistant',
          status: 'CANCELLED',
        },
      });
    mocks.getProduct.mockResolvedValue({ data: { productId: 'product-a', type: 'AGENT_API' } });
    mocks.cancelProductOrder.mockResolvedValue({
      data: {
        amount: 99.9,
        consumerId: 'consumer-a',
        currency: 'CNY',
        orderId: 'order-a',
        pricingMode: 'ONE_TIME',
        productId: 'product-a',
        productName: 'Legal Assistant',
        status: 'CANCELLED',
      },
    });

    render(<OrderDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('cancelOrder')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('cancelOrder'));

    await waitFor(() => {
      expect(mocks.cancelProductOrder).toHaveBeenCalledWith('consumer-a', 'order-a');
    });

    await waitFor(() => {
      expect(mocks.messageSuccess).toHaveBeenCalledWith('orderCancelSuccess');
      expect(mocks.getProductOrder).toHaveBeenCalledTimes(2);
    });
  });
});
