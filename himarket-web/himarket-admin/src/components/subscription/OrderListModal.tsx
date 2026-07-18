import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { Button, Descriptions, Drawer, Modal, message } from 'antd';
import { useEffect, useState } from 'react';

import { DataTable } from '@/components/common/DataTable';
import { useLocale } from '@/contexts/LocaleContext';
import { portalApi } from '@/lib/api';
import { formatDateTime } from '@/lib/utils';
import type { ProductOrder, ProductOrderModalProps } from '@/types';

interface ConsumerOrderResponse {
  data: {
    content: ProductOrder[];
    totalElements: number;
  };
}

interface ConsumerOrderDetailResponse {
  data: ProductOrder;
}

interface OrderListModalProps extends ProductOrderModalProps {
  embedded?: boolean;
  useTopLevelApi?: boolean;
}

export function OrderListModal({
  visible,
  consumerId,
  consumerName,
  onCancel,
  useTopLevelApi = false,
  embedded = false,
}: OrderListModalProps) {
  const { t } = useLocale();
  const [orders, setOrders] = useState<ProductOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentOrder, setCurrentOrder] = useState<ProductOrder | null>(null);
  const [productNameSearch, setProductNameSearch] = useState('');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const fetchOrders = (
    page = pagination.current,
    size = pagination.pageSize,
    productName?: string,
  ) => {
    setLoading(true);
    const request = useTopLevelApi
      ? portalApi.getAllOrders({ page, productName, size })
      : portalApi.getConsumerOrders(consumerId, {
          page,
          productName,
          size,
        });
    request
      .then((res: ConsumerOrderResponse) => {
        setOrders(res.data?.content || []);
        setPagination((prev) => ({
          ...prev,
          current: page,
          pageSize: size,
          total: res.data?.totalElements || 0,
        }));
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    if (visible && consumerId) {
      setProductNameSearch('');
      fetchOrders(1, pagination.pageSize);
    }
  }, [visible, consumerId]);

  const openOrderDetail = (orderId: string) => {
    const request = useTopLevelApi
      ? portalApi.getOrderDetail(orderId)
      : portalApi.getConsumerOrderDetail(consumerId, orderId);
    request.then((res: ConsumerOrderDetailResponse) => {
      setCurrentOrder(res.data || null);
      setDetailVisible(true);
    });
  };

  const handleMarkPaid = async () => {
    if (!currentOrder) return;
    setActionLoading('paid');
    try {
      const res = useTopLevelApi
        ? await portalApi.markOrderPaidByAdmin(currentOrder.orderId)
        : await portalApi.markOrderPaid(consumerId, currentOrder.orderId);
      setCurrentOrder(res.data || null);
      fetchOrders(pagination.current, pagination.pageSize, productNameSearch || undefined);
      message.success(t('portal.orders.markPaidSuccess'));
    } finally {
      setActionLoading(null);
    }
  };

  const handleCancelOrder = async () => {
    if (!currentOrder) return;
    setActionLoading('cancel');
    try {
      const res = useTopLevelApi
        ? await portalApi.cancelOrderByAdmin(currentOrder.orderId)
        : await portalApi.cancelOrder(consumerId, currentOrder.orderId);
      setCurrentOrder(res.data || null);
      fetchOrders(pagination.current, pagination.pageSize, productNameSearch || undefined);
      message.success(t('portal.orders.cancelSuccess'));
    } finally {
      setActionLoading(null);
    }
  };

  const columns = [
    {
      dataIndex: 'productName',
      key: 'productName',
      render: (productName: string, record: ProductOrder) => (
        <button
          className="text-blue-600 hover:text-blue-700 bg-transparent border-none p-0 text-left cursor-pointer"
          onClick={() => openOrderDetail(record.orderId)}
          type="button"
        >
          {productName}
        </button>
      ),
      title: t('portal.orders.productName'),
    },
    {
      dataIndex: 'amount',
      key: 'amount',
      render: (amount: number, record: ProductOrder) => `${record.currency} ${amount}`,
      title: t('portal.orders.amount'),
    },
    {
      dataIndex: 'status',
      key: 'status',
      render: (status: ProductOrder['status']) => {
        const labelMap: Record<ProductOrder['status'], string> = {
          CANCELLED: t('portal.orders.cancelled'),
          FAILED: t('portal.orders.failed'),
          PAID: t('portal.orders.paid'),
          PENDING_PAYMENT: t('portal.orders.pending'),
        };
        return <span className="text-xs text-gray-900">{labelMap[status]}</span>;
      },
      title: t('portal.orders.status'),
    },
    {
      dataIndex: 'createAt',
      key: 'createAt',
      render: (date: string) => (
        <span className="text-xs text-gray-500">{formatDateTime(date)}</span>
      ),
      title: t('portal.orders.time'),
    },
  ];

  if (embedded) {
    return (
      <div className="space-y-4">
        <DataTable<ProductOrder>
          columns={columns}
          dataSource={orders}
          loading={loading}
          pagination={{
            current: pagination.current,
            onChange: (page, size) =>
              fetchOrders(page, size ?? pagination.pageSize, productNameSearch || undefined),
            pageSize: pagination.pageSize,
            total: pagination.total,
          }}
          rowKey="orderId"
          search={{
            onChange: (value) => setProductNameSearch(value),
            onSearch: () => fetchOrders(1, pagination.pageSize, productNameSearch || undefined),
            placeholder: t('portal.orders.searchProduct'),
            value: productNameSearch,
          }}
        />

        <Drawer
          onClose={() => setDetailVisible(false)}
          open={detailVisible}
          title={t('portal.orders.detailTitle')}
          width={560}
        >
          {currentOrder && (
            <div className="space-y-4">
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label={t('portal.orders.orderId')}>
                  {currentOrder.orderId}
                </Descriptions.Item>
                <Descriptions.Item label={t('portal.orders.productName')}>
                  {currentOrder.productName}
                </Descriptions.Item>
                <Descriptions.Item label={t('portal.orders.amount')}>
                  {currentOrder.currency} {currentOrder.amount}
                </Descriptions.Item>
                <Descriptions.Item label={t('portal.orders.status')}>
                  {currentOrder.status}
                </Descriptions.Item>
                <Descriptions.Item label={t('portal.orders.time')}>
                  {currentOrder.createAt ? formatDateTime(currentOrder.createAt) : '-'}
                </Descriptions.Item>
                <Descriptions.Item label={t('portal.orders.paidAt')}>
                  {currentOrder.paidAt ? formatDateTime(currentOrder.paidAt) : '-'}
                </Descriptions.Item>
              </Descriptions>

              {currentOrder.paymentRecords && currentOrder.paymentRecords.length > 0 && (
                <Descriptions
                  bordered
                  className="mt-4"
                  column={1}
                  size="small"
                  title={t('portal.orders.paymentRecordsTitle')}
                >
                  {currentOrder.paymentRecords.map((paymentRecord) => (
                    <Descriptions.Item
                      key={paymentRecord.paymentRecordId}
                      label={paymentRecord.paymentRecordId}
                    >
                      <div className="space-y-1 text-sm">
                        <div>
                          {t('portal.orders.paymentProvider')}: {paymentRecord.provider}
                        </div>
                        <div>
                          {t('portal.orders.paymentStatus')}: {paymentRecord.status}
                        </div>
                        <div>
                          {t('portal.orders.amount')}: {paymentRecord.currency}{' '}
                          {paymentRecord.amount}
                        </div>
                        <div>
                          {t('portal.orders.paymentPayload')}:{' '}
                          {paymentRecord.callbackPayload || '-'}
                        </div>
                      </div>
                    </Descriptions.Item>
                  ))}
                </Descriptions>
              )}

              {currentOrder.status === 'PENDING_PAYMENT' && (
                <div className="flex gap-2 justify-end">
                  <Button
                    icon={<CloseOutlined />}
                    loading={actionLoading === 'cancel'}
                    onClick={handleCancelOrder}
                  >
                    {t('portal.orders.cancelAction')}
                  </Button>
                  <Button
                    icon={<CheckOutlined />}
                    loading={actionLoading === 'paid'}
                    onClick={handleMarkPaid}
                    type="primary"
                  >
                    {t('portal.orders.markPaidAction')}
                  </Button>
                </div>
              )}
            </div>
          )}
        </Drawer>
      </div>
    );
  }

  return (
    <Modal
      destroyOnClose
      footer={null}
      onCancel={onCancel}
      open={visible}
      title={t('portal.orders.title', { name: consumerName })}
      width={1100}
    >
      <DataTable<ProductOrder>
        columns={columns}
        dataSource={orders}
        loading={loading}
        pagination={{
          current: pagination.current,
          onChange: (page, size) =>
            fetchOrders(page, size ?? pagination.pageSize, productNameSearch || undefined),
          pageSize: pagination.pageSize,
          total: pagination.total,
        }}
        rowKey="orderId"
        search={{
          onChange: (value) => setProductNameSearch(value),
          onSearch: () => fetchOrders(1, pagination.pageSize, productNameSearch || undefined),
          placeholder: t('portal.orders.searchProduct'),
          value: productNameSearch,
        }}
      />

      <Drawer
        onClose={() => setDetailVisible(false)}
        open={detailVisible}
        title={t('portal.orders.detailTitle')}
        width={560}
      >
        {currentOrder && (
          <div className="space-y-4">
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label={t('portal.orders.orderId')}>
                {currentOrder.orderId}
              </Descriptions.Item>
              <Descriptions.Item label={t('portal.orders.productName')}>
                {currentOrder.productName}
              </Descriptions.Item>
              <Descriptions.Item label={t('portal.orders.amount')}>
                {currentOrder.currency} {currentOrder.amount}
              </Descriptions.Item>
              <Descriptions.Item label={t('portal.orders.status')}>
                {currentOrder.status}
              </Descriptions.Item>
              <Descriptions.Item label={t('portal.orders.time')}>
                {currentOrder.createAt ? formatDateTime(currentOrder.createAt) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label={t('portal.orders.paidAt')}>
                {currentOrder.paidAt ? formatDateTime(currentOrder.paidAt) : '-'}
              </Descriptions.Item>
            </Descriptions>

            {currentOrder.status === 'PENDING_PAYMENT' && (
              <div className="flex gap-2 justify-end">
                <Button
                  icon={<CloseOutlined />}
                  loading={actionLoading === 'cancel'}
                  onClick={handleCancelOrder}
                >
                  {t('portal.orders.cancelAction')}
                </Button>
                <Button
                  icon={<CheckOutlined />}
                  loading={actionLoading === 'paid'}
                  onClick={handleMarkPaid}
                  type="primary"
                >
                  {t('portal.orders.markPaidAction')}
                </Button>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </Modal>
  );
}
