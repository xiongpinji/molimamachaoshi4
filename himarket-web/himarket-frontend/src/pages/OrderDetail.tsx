import { ArrowLeftOutlined } from '@ant-design/icons';
import { Button, Descriptions, Skeleton, Tag, message } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';

import { Layout } from '../components/Layout';
import APIs, { type ProductOrder, type IProductDetail } from '../lib/apis';
import { getOrderStatusColor, getOrderStatusLabel } from '../lib/utils/orderStatus';

function OrderDetailPage() {
  const { t } = useTranslation('profile');
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [order, setOrder] = useState<ProductOrder | null>(null);
  const [primaryConsumerId, setPrimaryConsumerId] = useState<string | null>(null);
  const [product, setProduct] = useState<IProductDetail | null>(null);

  const loadOrderDetail = useCallback(async (consumerId: string, currentOrderId: string) => {
    const detail = await APIs.getProductOrder(consumerId, currentOrderId);
    const resolvedOrder = detail.data || null;
    setOrder(resolvedOrder);
    if (resolvedOrder?.productId) {
      const productDetail = await APIs.getProduct({ id: resolvedOrder.productId });
      setProduct(productDetail.data || null);
    } else {
      setProduct(null);
    }
  }, []);

  useEffect(() => {
    APIs.getPrimaryConsumer()
      .then(async (response) => {
        const consumerId = response.data?.consumerId || null;
        setPrimaryConsumerId(consumerId);
        if (!consumerId || !orderId) {
          setLoading(false);
          return;
        }
        await loadOrderDetail(consumerId, orderId);
      })
      .catch(() => {
        setOrder(null);
        setProduct(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [loadOrderDetail, orderId]);

  const productDetailPath = useMemo(() => {
    if (!order?.productId) {
      return null;
    }
    const productType = product?.type || order.productType;
    switch (productType) {
      case 'MODEL_API':
        return `/models/${order.productId}`;
      case 'MCP_SERVER':
        return `/mcp/${order.productId}`;
      case 'REST_API':
        return `/apis/${order.productId}`;
      case 'AGENT_SKILL':
        return `/skills/${order.productId}`;
      case 'WORKER':
        return `/workers/${order.productId}`;
      case 'AGENT_API':
      default:
        return `/agents/${order.productId}`;
    }
  }, [order, product]);

  const handleGoToUse = () => {
    const productType = product?.type || order?.productType;
    if (!productType || !productDetailPath) {
      return;
    }
    if (productType === 'MODEL_API' && product) {
      navigate('/chat', { state: { selectedProduct: product } });
      return;
    }
    navigate(productDetailPath);
  };

  const handleConfirmPayment = async () => {
    if (!primaryConsumerId || !orderId) {
      return;
    }

    try {
      setPaying(true);
      await APIs.markProductOrderPaid(primaryConsumerId, orderId);
      await loadOrderDetail(primaryConsumerId, orderId);
      message.success(t('orderPaySuccess'));
    } catch (error) {
      console.error('支付确认失败:', error);
      message.error(t('orderPayFailed'));
    } finally {
      setPaying(false);
    }
  };

  const handleCancelOrder = async () => {
    if (!primaryConsumerId || !orderId) {
      return;
    }

    try {
      setCancelling(true);
      await APIs.cancelProductOrder(primaryConsumerId, orderId);
      await loadOrderDetail(primaryConsumerId, orderId);
      message.success(t('orderCancelSuccess'));
    } catch (error) {
      console.error('订单取消失败:', error);
      message.error(t('orderCancelFailed'));
    } finally {
      setCancelling(false);
    }
  };

  return (
    <Layout>
      <div className="w-full">
        <section className="min-h-[calc(100vh-96px)] rounded-2xl border border-white/40 bg-white/90 p-6 shadow-xs backdrop-blur-xl">
          <button className="mb-4 inline-flex items-center gap-2 text-sm text-gray-600" onClick={() => navigate('/profile')} type="button">
            <ArrowLeftOutlined />
            {t('backToOrders')}
          </button>

          {loading ? (
            <Skeleton active paragraph={{ rows: 6 }} />
          ) : order ? (
            <div className="space-y-6">
              <div>
                <h1 className="text-2xl font-semibold text-gray-900">{t('orderDetailTitle')}</h1>
                <p className="mt-2 text-sm text-gray-500">{order.productName || order.orderId}</p>
              </div>

              <Descriptions bordered column={1} size="middle">
                <Descriptions.Item label={t('orderId')}>{order.orderId}</Descriptions.Item>
                <Descriptions.Item label={t('orderProduct')}>{order.productName || '-'}</Descriptions.Item>
                <Descriptions.Item label={t('orderAmount')}>
                  {order.currency} {order.amount}
                </Descriptions.Item>
                <Descriptions.Item label={t('orderStatus')}>
                  <Tag color={getOrderStatusColor(order.status)}>
                    {getOrderStatusLabel(order.status, t)}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label={t('orderPricingMode')}>{order.pricingMode}</Descriptions.Item>
                <Descriptions.Item label={t('orderPaidAt')}>{order.paidAt || '-'}</Descriptions.Item>
                <Descriptions.Item label={t('orderCreatedAt')}>{order.createAt || '-'}</Descriptions.Item>
                <Descriptions.Item label={t('orderConsumerId')}>{primaryConsumerId || '-'}</Descriptions.Item>
              </Descriptions>

              {order.paymentRecords && order.paymentRecords.length > 0 && (
                <Descriptions bordered className="mt-4" column={1} size="small" title={t('paymentRecordsTitle')}>
                  {order.paymentRecords.map((paymentRecord) => (
                    <Descriptions.Item key={paymentRecord.paymentRecordId} label={paymentRecord.paymentRecordId}>
                      <div className="space-y-1 text-sm">
                        <div>{t('paymentProvider')}: {paymentRecord.provider}</div>
                        <div>{t('paymentStatus')}: {paymentRecord.status}</div>
                        <div>{t('orderAmount')}: {paymentRecord.currency} {paymentRecord.amount}</div>
                        <div>{t('paymentPayload')}: {paymentRecord.callbackPayload || '-'}</div>
                      </div>
                    </Descriptions.Item>
                  ))}
                </Descriptions>
              )}

              {order.status === 'PENDING_PAYMENT' && (
                <div className="flex flex-wrap gap-3">
                  <Button loading={paying} onClick={handleConfirmPayment} type="primary">
                    {t('confirmPayment')}
                  </Button>
                  <Button loading={cancelling} onClick={handleCancelOrder}>
                    {t('cancelOrder')}
                  </Button>
                  {productDetailPath && (
                    <Button onClick={() => navigate(productDetailPath)}>{t('goToProduct')}</Button>
                  )}
                </div>
              )}

              {order.status === 'PAID' && (
                <div className="flex flex-wrap gap-3">
                  {productDetailPath && (
                    <Button onClick={() => navigate(productDetailPath)}>{t('goToProduct')}</Button>
                  )}
                  <Button onClick={handleGoToUse} type="primary">
                    {t('goToUse')}
                  </Button>
                </div>
              )}
            </div>
          ) : (
            <div className="rounded-[16px] border border-dashed border-[#DDE5F0] bg-[#FBFCFF] px-6 py-10 text-center text-sm text-gray-500">
              {t('orderNotFound')}
            </div>
          )}
        </section>
      </div>
    </Layout>
  );
}

export default OrderDetailPage;
