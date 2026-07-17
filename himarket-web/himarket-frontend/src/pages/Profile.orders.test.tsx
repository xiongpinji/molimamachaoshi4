import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  getDeveloperInfo: vi.fn(),
  getPrimaryConsumer: vi.fn(),
  getProductOrders: vi.fn(),
  navigate: vi.fn(),
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
  Object.defineProperty(window, 'ResizeObserver', {
    configurable: true,
    value: class {
      disconnect() {}
      observe() {}
      unobserve() {}
    },
  });
});

vi.mock('../components/Layout', () => ({
  Layout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('../components/ChangePasswordForm', () => ({
  ChangePasswordForm: () => <div>change password form</div>,
}));

vi.mock('../hooks/useAuth', () => ({
  notifyAuthInvalidated: vi.fn(),
}));

vi.mock('../lib/userInfoCache', () => ({
  clearCachedUserInfo: vi.fn(),
}));

vi.mock('../lib/apis', () => ({
  __esModule: true,
  default: {
    getDeveloperInfo: () => mocks.getDeveloperInfo(),
    getPrimaryConsumer: () => mocks.getPrimaryConsumer(),
    getProductOrders: (...args: unknown[]) => mocks.getProductOrders(...args),
  },
}));

vi.mock('../lib/utils', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('../lib/utils');
  return {
    ...actual,
    formatDateTime: (value: string) => value,
  };
});

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        'orderStatusLabels.CANCELLED': '已取消',
        'orderStatusLabels.FAILED': '支付失败',
        'orderStatusLabels.PAID': '已支付',
        'orderStatusLabels.PENDING_PAYMENT': '待支付',
      };
      return map[key] || key;
    },
  }),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mocks.navigate,
}));

import Profile from './Profile';

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('Profile order history', () => {
  it('切换到订单历史后展示主消费者订单', async () => {
    mocks.getDeveloperInfo.mockResolvedValue({
      data: {
        createAt: '2026-07-14T00:00:00',
        email: 'dev@example.com',
        username: 'dev-a',
      },
    });
    mocks.getPrimaryConsumer.mockResolvedValue({
      data: {
        consumerId: 'consumer-a',
        name: 'Primary Consumer',
      },
    });
    mocks.getProductOrders.mockResolvedValue({
      data: {
        content: [
          {
            amount: 99.9,
            consumerId: 'consumer-a',
            currency: 'CNY',
            orderId: 'order-a',
            pricingMode: 'ONE_TIME',
            productName: 'Legal Assistant',
            status: 'PAID',
          },
          {
            amount: 29.9,
            consumerId: 'consumer-a',
            currency: 'CNY',
            orderId: 'order-b',
            pricingMode: 'ONE_TIME',
            productName: 'Tax Copilot',
            status: 'PENDING_PAYMENT',
          },
        ],
        number: 1,
        size: 10,
        totalElements: 2,
      },
    });

    render(<Profile />);

    await waitFor(() => {
      expect(screen.getByText('purchaseHistory')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('purchaseHistory'));

    await waitFor(() => {
      expect(mocks.getProductOrders).toHaveBeenCalledWith('consumer-a', { page: 1, size: 10 });
    });

    await waitFor(() => {
      expect(screen.getByText('Legal Assistant')).toBeInTheDocument();
      expect(screen.getByText('Tax Copilot')).toBeInTheDocument();
      expect(screen.getByText('已支付')).toBeInTheDocument();
      expect(screen.getByText('待支付')).toBeInTheDocument();
    });
  });

  it('支持按关键字筛选订单并点击进入详情', async () => {
    mocks.getDeveloperInfo.mockResolvedValue({
      data: {
        createAt: '2026-07-14T00:00:00',
        email: 'dev@example.com',
        username: 'dev-a',
      },
    });
    mocks.getPrimaryConsumer.mockResolvedValue({
      data: {
        consumerId: 'consumer-a',
        name: 'Primary Consumer',
      },
    });
    mocks.getProductOrders.mockResolvedValue({
      data: {
        content: [
          {
            amount: 99.9,
            consumerId: 'consumer-a',
            currency: 'CNY',
            orderId: 'order-a',
            pricingMode: 'ONE_TIME',
            productName: 'Legal Assistant',
            status: 'PAID',
          },
          {
            amount: 29.9,
            consumerId: 'consumer-a',
            currency: 'CNY',
            orderId: 'order-b',
            pricingMode: 'ONE_TIME',
            productName: 'Tax Copilot',
            status: 'PENDING_PAYMENT',
          },
        ],
        number: 1,
        size: 10,
        totalElements: 2,
      },
    });

    render(<Profile />);

    await waitFor(() => {
      expect(screen.getByText('purchaseHistory')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('purchaseHistory'));

    await waitFor(() => {
      expect(screen.getByText('Tax Copilot')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText('orderSearchPlaceholder'), {
      target: { value: 'Legal' },
    });

    await waitFor(() => {
      expect(screen.getByText('Legal Assistant')).toBeInTheDocument();
      expect(screen.queryByText('Tax Copilot')).not.toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Legal Assistant'));

    expect(mocks.navigate).toHaveBeenCalledWith('/orders/order-a');
  });

  it('支持按状态筛选并显示分页', async () => {
    mocks.getDeveloperInfo.mockResolvedValue({
      data: {
        createAt: '2026-07-14T00:00:00',
        email: 'dev@example.com',
        username: 'dev-a',
      },
    });
    mocks.getPrimaryConsumer.mockResolvedValue({
      data: {
        consumerId: 'consumer-a',
        name: 'Primary Consumer',
      },
    });
    mocks.getProductOrders.mockResolvedValue({
      data: {
        content: [
          {
            amount: 99.9,
            consumerId: 'consumer-a',
            currency: 'CNY',
            orderId: 'order-a',
            pricingMode: 'ONE_TIME',
            productName: 'Legal Assistant',
            status: 'PAID',
          },
          {
            amount: 29.9,
            consumerId: 'consumer-a',
            currency: 'CNY',
            orderId: 'order-b',
            pricingMode: 'ONE_TIME',
            productName: 'Tax Copilot',
            status: 'PENDING_PAYMENT',
          },
        ],
        number: 1,
        size: 10,
        totalElements: 11,
      },
    });

    render(<Profile />);

    await waitFor(() => {
      expect(screen.getByText('purchaseHistory')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('purchaseHistory'));

    await waitFor(() => {
      expect(screen.getByText('Legal Assistant')).toBeInTheDocument();
    });

    fireEvent.mouseDown(screen.getByRole('combobox'));
    const paidOptions = screen.getAllByText('已支付');
    expect(paidOptions.length).toBeGreaterThan(1);
    fireEvent.click(paidOptions[1]!);

    await waitFor(() => {
      expect(screen.getByText('Legal Assistant')).toBeInTheDocument();
      expect(screen.queryByText('Tax Copilot')).not.toBeInTheDocument();
    });

    expect(screen.getByRole('list')).toBeInTheDocument();
  });
});
