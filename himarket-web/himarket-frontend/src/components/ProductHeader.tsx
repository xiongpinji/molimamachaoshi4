import {
  ApiOutlined,
  CheckCircleFilled,
  ClockCircleFilled,
  DeleteOutlined,
  ExclamationCircleFilled,
  InfoCircleOutlined,
  PlusOutlined,
  RobotOutlined,
  SearchOutlined,
  BulbOutlined,
} from '@ant-design/icons';
import {
  Typography,
  Button,
  Modal,
  Select,
  message,
  Popconfirm,
  Input,
  Pagination,
  Spin,
  Table,
  Popover,
} from 'antd';
import React, { useState, useEffect, useImperativeHandle, forwardRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';

import { LoginPrompt } from './LoginPrompt';
import { useAuth } from '../hooks/useAuth';
import {
  getConsumers,
  subscribeProduct,
  unsubscribeProduct,
  getProductSubscriptions,
} from '../lib/apis';
import APIs, { type ISubscription, type ProductOrder } from '../lib/apis';
import { resolveProductAccessState } from '../lib/utils/productAccessState';
import { formatCommercePrice } from '../lib/utils/productCardTags';

import type { getProductSubscriptionStatus } from '../lib/apis';
import type { ICommerceConfig, IMCPConfig, IProductIcon, IAgentConfig } from '../lib/apis/typing';
import type { Consumer } from '../types/consumer';

const { Paragraph, Title } = Typography;

export interface ProductHeaderHandle {
  showManageModal: () => void;
  showPurchaseFlow: () => void;
}

interface ProductHeaderProps {
  name: string;
  description: string;
  icon?: IProductIcon;
  defaultIcon?: string;
  mcpConfig?: IMCPConfig;
  agentConfig?: IAgentConfig;
  commerceConfig?: ICommerceConfig;
  updatedAt?: string;
  productType?: 'REST_API' | 'MCP_SERVER' | 'AGENT_API' | 'MODEL_API' | 'AGENT_SKILL';
  subscribable?: boolean;
  onSubscriptionStatusChange?: (hasSubscription: boolean) => void;
}

type UnwrapPromise<T> = T extends Promise<infer U> ? U : T;

const hasIconValue = (icon?: IProductIcon): icon is IProductIcon => Boolean(icon?.value?.trim());

// 处理产品图标的函数
const getIconUrl = (icon?: IProductIcon, defaultIcon?: string): string => {
  const fallback = defaultIcon || '/logo.svg';

  if (!hasIconValue(icon)) {
    return fallback;
  }

  switch (icon.type) {
    case 'URL':
      return icon.value || fallback;
    case 'BASE64':
      // 如果value已经包含data URL前缀，直接使用；否则添加前缀
      return icon.value
        ? icon.value.startsWith('data:')
          ? icon.value
          : `data:image/png;base64,${icon.value}`
        : fallback;
    default:
      return fallback;
  }
};

export const ProductHeader = forwardRef<ProductHeaderHandle, ProductHeaderProps>(
  (
    {
      defaultIcon = '/default-icon.png',
      description,
      icon,
      name,
      commerceConfig,
      onSubscriptionStatusChange,
      productType,
      subscribable,
      updatedAt,
    },
    ref,
  ) => {
    const { agentProductId, apiProductId, mcpProductId, modelProductId } = useParams();
    const navigate = useNavigate();

    const { isLoggedIn } = useAuth();
    const { i18n, t } = useTranslation('productHeader');
    const { t: tLoginPrompt } = useTranslation('loginPrompt');
    const [loginPromptOpen, setLoginPromptOpen] = useState(false);
    const [isManageModalVisible, setIsManageModalVisible] = useState(false);
    const [isApplyingSubscription, setIsApplyingSubscription] = useState(false);
    const [selectedConsumerId, setSelectedConsumerId] = useState<string>('');
    const [consumers, setConsumers] = useState<Consumer[]>([]);

    // 分页相关state
    const [currentPage, setCurrentPage] = useState(1);
    const pageSize = 5; // 每页显示5个订阅

    // 分开管理不同的loading状态
    const [consumersLoading, setConsumersLoading] = useState(false);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [purchaseLoading, setPurchaseLoading] = useState(false);
    const [activeOrderLoading, setActiveOrderLoading] = useState(false);
    const [imageLoadFailed, setImageLoadFailed] = useState(false);

    // 订阅状态相关的state
    const [subscriptionStatus, setSubscriptionStatus] =
      useState<UnwrapPromise<ReturnType<typeof getProductSubscriptionStatus>>>();
    const [subscriptionLoading, setSubscriptionLoading] = useState(false);
    const [activeOrder, setActiveOrder] = useState<ProductOrder | null>(null);

    // 订阅详情分页数据（用于管理弹窗）
    const [subscriptionDetails, setSubscriptionDetails] = useState<{
      content: ISubscription[];
      totalElements: number;
      totalPages: number;
    }>({ content: [], totalElements: 0, totalPages: 0 });
    const [detailsLoading, setDetailsLoading] = useState(false);

    // 搜索相关state
    const [searchKeyword, setSearchKeyword] = useState('');

    const shouldShowSubscribeButton = subscribable !== false;
    const priceText = formatCommercePrice(commerceConfig, t);
    const pricingModeText =
      commerceConfig?.pricingMode ? t(`commerce.pricingMode.${commerceConfig.pricingMode}`) : undefined;
    const commerceLabel = commerceConfig?.displayLabel || t('commerce.displayLabelFallback');
    const isPaid = Boolean(commerceConfig?.enabled && priceText);
    const hasApprovedSubscription = Boolean(
      subscriptionStatus?.subscribedConsumers?.some((item) => item.subscription?.status === 'APPROVED'),
    );
    const hasPendingSubscription = Boolean(
      subscriptionStatus?.subscribedConsumers?.some((item) => item.subscription?.status === 'PENDING'),
    );
    const hasPendingPaymentOrder = activeOrder?.status === 'PENDING_PAYMENT';
    const hasPaidOrder = activeOrder?.status === 'PAID';
    const accessState = resolveProductAccessState({
      hasApprovedSubscription,
      hasPendingPaymentOrder,
      hasPaidOrder,
      hasPendingSubscription,
      isLoggedIn,
      isPaid,
      subscribable: shouldShowSubscribeButton,
    });

    // 获取产品ID - 根据产品类型获取正确的参数
    const productId = apiProductId || mcpProductId || agentProductId || modelProductId || '';

    // 查询订阅状态
    const fetchSubscriptionStatus = React.useCallback(async () => {
      if (!productId || !shouldShowSubscribeButton) return;

      setSubscriptionLoading(true);
      try {
        const status = await APIs.getProductSubscriptionStatus(productId);
        setSubscriptionStatus(status);
      } catch (error) {
        console.error('获取订阅状态失败:', error);
      } finally {
        setSubscriptionLoading(false);
      }
    }, [productId, shouldShowSubscribeButton]);

    const fetchActiveOrder = React.useCallback(async () => {
      if (!productId || !isLoggedIn || !isPaid) {
        setActiveOrder(null);
        return;
      }

      setActiveOrderLoading(true);
      try {
        const primaryConsumerResponse = await APIs.getPrimaryConsumer();
        const primaryConsumerId = primaryConsumerResponse.data?.consumerId;
        if (!primaryConsumerId) {
          setActiveOrder(null);
          return;
        }
        const order = await APIs.getActiveProductOrder(primaryConsumerId, productId);
        setActiveOrder(order);
      } catch (error) {
        console.error('获取活跃订单失败:', error);
        setActiveOrder(null);
      } finally {
        setActiveOrderLoading(false);
      }
    }, [isLoggedIn, isPaid, productId]);

    // 暴露给父组件的方法
    useImperativeHandle(ref, () => ({
      showManageModal,
      showPurchaseFlow: () => {
        if (
          accessState === 'purchase_continue_payment' ||
          accessState === 'purchase_activation_processing'
        ) {
          handleViewOrder();
          return;
        }
        if (isPaid && !hasApprovedSubscription) {
          void handlePurchase();
          return;
        }
        showManageModal();
      },
    }));

    // 订阅状态变化时通知父组件
    useEffect(() => {
      if (subscriptionStatus !== undefined && onSubscriptionStatusChange) {
        onSubscriptionStatusChange(hasApprovedSubscription);
      }
    }, [hasApprovedSubscription, onSubscriptionStatusChange, subscriptionStatus]);

    // 获取订阅详情（用于管理弹窗）
    const fetchSubscriptionDetails = async (
      page: number = 1,
      search: string = '',
    ): Promise<void> => {
      if (!productId) return Promise.resolve();

      setDetailsLoading(true);
      try {
        const response = await getProductSubscriptions(productId, {
          consumerName: search.trim() || undefined,
          page: page,
          size: pageSize,
        });

        setSubscriptionDetails({
          content: response.data.content || [],
          totalElements: response.data.totalElements || 0,
          totalPages: response.data.totalPages || 0,
        });
      } catch (error) {
        console.error('获取订阅详情失败:', error);
        message.error(t('message.subscriptionDetailsFailed'));
      } finally {
        setDetailsLoading(false);
      }
    };

    useEffect(() => {
      fetchSubscriptionStatus();
    }, [fetchSubscriptionStatus]);

    useEffect(() => {
      fetchActiveOrder();
    }, [fetchActiveOrder]);

    // 获取消费者列表
    const fetchConsumers = async () => {
      try {
        setConsumersLoading(true);
        const response = await getConsumers({ page: 1, size: 100 });
        if (response.data) {
          setConsumers(response.data.content || response.data);
        }
      } catch (error) {
        console.warn(error);
        // message.error('获取消费者列表失败');
      } finally {
        setConsumersLoading(false);
      }
    };

    // 开始申请订阅流程
    const startApplyingSubscription = () => {
      setIsApplyingSubscription(true);
      setSelectedConsumerId('');
      fetchConsumers();
    };

    // 取消申请订阅
    const cancelApplyingSubscription = () => {
      setIsApplyingSubscription(false);
      setSelectedConsumerId('');
    };

    // 提交申请订阅
    const handleApplySubscription = async () => {
      if (!selectedConsumerId) {
        message.warning(t('message.selectConsumer'));
        return;
      }

      try {
        setSubmitLoading(true);
        await subscribeProduct(selectedConsumerId, productId);
        message.success(t('message.applySuccess'));

        // 重置状态
        setIsApplyingSubscription(false);
        setSelectedConsumerId('');

        // 重新获取订阅状态和详情数据
        await fetchSubscriptionStatus();
        await fetchSubscriptionDetails(currentPage, '');
      } catch (error) {
        console.error('申请订阅失败:', error);
        message.error(t('message.applyFailed'));
      } finally {
        setSubmitLoading(false);
      }
    };

    const handlePurchase = async () => {
      if (!productId) {
        return;
      }

      try {
        setPurchaseLoading(true);
        const primaryConsumerResponse = await APIs.getPrimaryConsumer();
        const primaryConsumerId = primaryConsumerResponse.data?.consumerId;

        if (!primaryConsumerId) {
          message.warning(t('purchase.primaryConsumerRequired'));
          return;
        }

        const orderResponse = await APIs.createProductOrder(primaryConsumerId, productId);
        const orderId = orderResponse.data?.orderId;

        if (!orderId) {
          throw new Error('Order ID missing');
        }

        navigate(`/orders/${orderId}`);
      } catch (error) {
        console.error('购买失败:', error);
        const primaryConsumerResponse = await APIs.getPrimaryConsumer().catch(() => null);
        const primaryConsumerId = primaryConsumerResponse?.data?.consumerId;
        if (primaryConsumerId) {
          const existingOrder = await APIs.getActiveProductOrder(primaryConsumerId, productId);
          if (existingOrder?.orderId) {
            message.warning(t('purchase.existingOrder'));
            setActiveOrder(existingOrder);
            navigate(`/orders/${existingOrder.orderId}`);
            return;
          }
        }
        message.error(t('purchase.failed'));
      } finally {
        setPurchaseLoading(false);
      }
    };

    const handleViewOrder = () => {
      if (activeOrder?.orderId) {
        navigate(`/orders/${activeOrder.orderId}`);
      }
    };

    // 显示管理弹窗
    const showManageModal = () => {
      setIsManageModalVisible(true);

      // 优先使用已缓存的数据，避免重复查询
      if (subscriptionStatus?.fullSubscriptionData) {
        setSubscriptionDetails({
          content: subscriptionStatus.fullSubscriptionData.content,
          totalElements: subscriptionStatus.fullSubscriptionData.totalElements,
          totalPages: subscriptionStatus.fullSubscriptionData.totalPages,
        });
        // 重置分页到第一页
        setCurrentPage(1);
        setSearchKeyword('');
      } else {
        // 如果没有缓存数据，则重新获取
        fetchSubscriptionDetails(1, '');
      }
    };

    // 处理搜索输入变化
    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value;
      setSearchKeyword(value);
      // 只更新状态，不触发搜索
    };

    // 执行搜索
    const handleSearch = (value?: string) => {
      // 如果传入了value参数，使用该参数；否则使用当前的searchKeyword
      const keyword = value !== undefined ? value : searchKeyword;
      const trimmedKeyword = keyword.trim();
      setCurrentPage(1);

      // 总是调用API进行搜索，不使用缓存
      fetchSubscriptionDetails(1, trimmedKeyword);
    };

    // 处理回车键搜索
    const handleSearchKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        handleSearch();
      }
    };

    // 隐藏管理弹窗
    const handleManageCancel = () => {
      setIsManageModalVisible(false);
      // 重置申请订阅状态
      setIsApplyingSubscription(false);
      setSelectedConsumerId('');
      // 重置分页和搜索
      setCurrentPage(1);
      setSearchKeyword('');
      // 清空订阅详情数据
      setSubscriptionDetails({ content: [], totalElements: 0, totalPages: 0 });
    };

    // 取消订阅
    const handleUnsubscribe = async (consumerId: string) => {
      try {
        await unsubscribeProduct(consumerId, productId);
        message.success(t('message.unsubscribeSuccess'));

        // 重新获取订阅状态和详情数据
        await fetchSubscriptionStatus();
        await fetchSubscriptionDetails(currentPage, '');
      } catch (error) {
        console.error('取消订阅失败:', error);
        message.error(t('message.unsubscribeFailed'));
      }
    };

    return (
      <>
        <div className="rounded-[14px] border border-[#DDE5F0] bg-white/90 p-5 shadow-[0_18px_50px_rgba(15,23,42,0.06)] backdrop-blur-sm">
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
              <div className="flex min-w-0 items-start gap-4">
                {(!hasIconValue(icon) || imageLoadFailed) &&
                (productType === 'REST_API' ||
                  productType === 'AGENT_API' ||
                  productType === 'MODEL_API') ? (
                  <div className="flex h-14 w-14 flex-shrink-0 items-center justify-center rounded-[12px] border border-[#E1E7F0] bg-[#F7F9FC] text-gray-800">
                    {productType === 'REST_API' ? (
                      <ApiOutlined className="text-2xl" />
                    ) : productType === 'AGENT_API' ? (
                      <RobotOutlined className="text-2xl" />
                    ) : (
                      <BulbOutlined className="text-2xl" />
                    )}
                  </div>
                ) : (
                  <img
                    alt={name}
                    className="h-14 w-14 flex-shrink-0 rounded-[12px] border border-[#E1E7F0] bg-[#F7F9FC] object-cover"
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      if (
                        productType === 'REST_API' ||
                        productType === 'AGENT_API' ||
                        productType === 'MODEL_API'
                      ) {
                        setImageLoadFailed(true);
                      } else {
                        // 确保有一个最终的fallback图片，避免无限循环请求
                        const fallbackIcon = defaultIcon || '/logo.svg';
                        const currentUrl = new URL(target.src, window.location.href).href;
                        const fallbackUrl = new URL(fallbackIcon, window.location.href).href;
                        if (currentUrl !== fallbackUrl) {
                          target.src = fallbackIcon;
                        }
                      }
                    }}
                    src={getIconUrl(icon, defaultIcon)}
                  />
                )}
                <div className="min-w-0 flex-1">
                  <Title
                    className="!mb-1 !break-words !text-2xl !font-semibold !leading-tight !text-gray-950"
                    level={2}
                  >
                    {name}
                  </Title>
                  {updatedAt && (
                    <div className="text-sm text-gray-500">
                      {t('updatedAt', {
                        date: new Date(updatedAt)
                          .toLocaleDateString(i18n.language, {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                          })
                          .replace(/\//g, '.'),
                      })}
                    </div>
                  )}
                </div>
              </div>

              <div className="flex flex-shrink-0 flex-wrap items-center gap-3 lg:justify-end">
                {accessState === 'open_access' ? (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-gray-100 px-3 py-1 text-xs font-medium text-gray-500">
                    <InfoCircleOutlined className="text-gray-400" style={{ fontSize: '12px' }} />
                    {t('subscribe.openAccess')}
                  </span>
                ) : accessState === 'purchase_login' ? (
                  <Button
                    className="rounded-[10px]"
                    onClick={() => setLoginPromptOpen(true)}
                    type="primary"
                  >
                    {t('purchase.loginRequired')}
                  </Button>
                ) : accessState === 'subscribe_login' ? (
                  <Button
                    className="rounded-[10px]"
                    onClick={() => setLoginPromptOpen(true)}
                    type="primary"
                  >
                    {t('subscribe.loginRequired')}
                  </Button>
                ) : subscriptionLoading || activeOrderLoading ? (
                  <Button loading>{t('loading')}</Button>
                ) : (
                  <>
                    {hasApprovedSubscription ? (
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-green-100 bg-green-50 px-2.5 py-1 text-xs font-medium text-green-600">
                        <CheckCircleFilled className="text-green-500" style={{ fontSize: '10px' }} />
                        {isPaid ? t('purchase.active') : t('subscribe.subscribed')}
                      </span>
                    ) : accessState === 'purchase_continue_payment' ? (
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-100 bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-600">
                        <ClockCircleFilled className="text-amber-500" style={{ fontSize: '10px' }} />
                        {t('purchase.continuePayment')}
                      </span>
                    ) : accessState === 'purchase_activation_processing' ? (
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-blue-100 bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-600">
                        <ClockCircleFilled className="text-blue-500" style={{ fontSize: '10px' }} />
                        {t('purchase.activationProcessing')}
                      </span>
                    ) : hasPendingSubscription ? (
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-blue-100 bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-600">
                        <ClockCircleFilled className="text-blue-500" style={{ fontSize: '10px' }} />
                        {isPaid ? t('purchase.pending') : t('subscribe.pending')}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-500">
                        <div className="h-1.5 w-1.5 rounded-full bg-gray-400"></div>
                        {isPaid ? t('purchase.available') : t('subscribe.notSubscribed')}
                      </span>
                    )}

                    <Button
                      className="rounded-[10px]"
                      loading={purchaseLoading}
                      onClick={
                        accessState === 'purchase_continue_payment' ||
                        accessState === 'purchase_activation_processing'
                          ? handleViewOrder
                          : isPaid && !hasApprovedSubscription
                            ? handlePurchase
                            : showManageModal
                      }
                      type="primary"
                    >
                      {accessState === 'purchase_continue_payment' ||
                      accessState === 'purchase_activation_processing'
                        ? t('purchase.viewOrder')
                        : isPaid && !hasApprovedSubscription
                          ? t('purchase.action')
                          : isPaid
                            ? t('purchase.manage')
                            : t('subscribe.manage')}
                    </Button>
                  </>
                )}
              </div>
            </div>

            {description && (
              <Paragraph className="!mb-0 max-w-5xl break-words text-sm leading-6 text-gray-600">
                {description}
              </Paragraph>
            )}

            {priceText && (
              <div className="flex flex-wrap items-center gap-3 rounded-[12px] border border-[#F6D7A8] bg-[#FFF8EC] px-4 py-3">
                <div>
                  <div className="text-xs font-medium text-[#8A5A10]">{t('commerce.priceTitle')}</div>
                  <div className="mt-1 text-lg font-semibold text-[#8A4B08]">{priceText}</div>
                </div>
                <span className="inline-flex items-center rounded-full border border-[#F0C98A] bg-white/80 px-2.5 py-1 text-xs font-medium text-[#A25B00]">
                  {commerceLabel}
                </span>
                {pricingModeText && (
                  <span className="text-xs text-[#8A5A10]">{pricingModeText}</span>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 订阅管理弹窗 */}
        <Modal
          footer={null}
          onCancel={handleManageCancel}
          open={isManageModalVisible}
          style={{ maxWidth: 'calc(100vw - 32px)' }}
          styles={{
            body: {
              padding: '0px',
            },
            header: {
              borderRadius: '8px 8px 0 0',
              marginBottom: 0,
              paddingBottom: '8px',
            },
          }}
          title={t('modal.title')}
          width={640}
        >
          <div className="px-4 py-4 sm:px-6">
            {/* 搜索和操作栏 */}
            <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <Input
                allowClear
                className="w-full rounded-lg sm:w-[250px]"
                onChange={handleSearchChange}
                onPressEnter={handleSearchKeyPress}
                placeholder={t('modal.searchConsumer')}
                prefix={<SearchOutlined className="text-gray-400" />}
                value={searchKeyword}
              />
              <Popover
                content={
                  <div className="w-64">
                    <div className="mb-3 text-sm font-medium text-gray-700">
                      {t('modal.selectConsumer')}
                    </div>
                    <Select
                      filterOption={(input, option) =>
                        (option?.children as unknown as string)
                          ?.toLowerCase()
                          .includes(input.toLowerCase())
                      }
                      loading={consumersLoading}
                      notFoundContent={consumersLoading ? t('loading') : t('modal.noConsumers')}
                      onChange={setSelectedConsumerId}
                      placeholder={t('modal.consumerPlaceholder')}
                      showSearch
                      style={{ width: '100%' }}
                      value={selectedConsumerId}
                    >
                      {consumers
                        .filter((consumer) => {
                          const isAlreadySubscribed = subscriptionStatus?.subscribedConsumers?.some(
                            (item) => item.consumer.consumerId === consumer.consumerId,
                          );
                          return !isAlreadySubscribed;
                        })
                        .map((consumer) => (
                          <Select.Option key={consumer.consumerId} value={consumer.consumerId}>
                            {consumer.name}
                          </Select.Option>
                        ))}
                    </Select>
                    <div className="mt-4 flex justify-end gap-2">
                      <Button
                        className="rounded-lg"
                        onClick={cancelApplyingSubscription}
                        size="small"
                      >
                        {t('modal.cancel')}
                      </Button>
                      <Button
                        className="rounded-lg"
                        disabled={!selectedConsumerId}
                        loading={submitLoading}
                        onClick={handleApplySubscription}
                        size="small"
                        type="primary"
                      >
                        {t('modal.confirm')}
                      </Button>
                    </div>
                  </div>
                }
                onOpenChange={(open) => {
                  if (!open) cancelApplyingSubscription();
                }}
                open={isApplyingSubscription}
                placement="bottomRight"
                trigger="click"
              >
                <Button
                  className="w-full rounded-[10px] sm:w-auto"
                  icon={<PlusOutlined />}
                  onClick={startApplyingSubscription}
                  type="primary"
                >
                  {t('subscribe.action')}
                </Button>
              </Popover>
            </div>

            {/* 订阅列表表格 */}
            <div className="overflow-x-auto rounded-lg border border-gray-200">
              {detailsLoading ? (
                <div className="p-8 text-center">
                  <Spin size="large" />
                </div>
              ) : subscriptionDetails.content && subscriptionDetails.content.length > 0 ? (
                <Table
                  className="subscription-table"
                  columns={[
                    {
                      dataIndex: 'consumerName',
                      key: 'consumerName',
                      render: (text: string) => (
                        <span className="text-xs text-gray-800">{text}</span>
                      ),
                      title: t('table.consumer'),
                    },
                    {
                      dataIndex: 'status',
                      key: 'status',
                      render: (status: string) => (
                        <div className="flex items-center">
                          {status === 'APPROVED' ? (
                            <>
                              <CheckCircleFilled className="mr-1.5 text-xs text-green-500" />
                              <span className="text-xs text-gray-700">
                                {t('table.status.approved')}
                              </span>
                            </>
                          ) : status === 'PENDING' ? (
                            <>
                              <ClockCircleFilled className="mr-1.5 text-xs text-blue-500" />
                              <span className="text-xs text-gray-700">
                                {t('table.status.pending')}
                              </span>
                            </>
                          ) : (
                            <>
                              <ExclamationCircleFilled className="mr-1.5 text-xs text-red-500" />
                              <span className="text-xs text-gray-700">
                                {t('table.status.rejected')}
                              </span>
                            </>
                          )}
                        </div>
                      ),
                      title: t('table.statusTitle'),
                      width: 120,
                    },
                    {
                      align: 'center',
                      key: 'action',
                      render: (_: unknown, record: ISubscription) => (
                        <Popconfirm
                          cancelText={t('modal.cancel')}
                          okText={t('modal.confirm')}
                          onConfirm={() => handleUnsubscribe(record.consumerId)}
                          title={t('table.unsubscribeConfirm')}
                        >
                          <Button
                            className="rounded border-gray-300"
                            icon={<DeleteOutlined className="text-xs text-red-500" />}
                            size="small"
                          />
                        </Popconfirm>
                      ),
                      title: t('table.action'),
                      width: 100,
                    },
                  ]}
                  dataSource={
                    searchKeyword.trim()
                      ? subscriptionDetails.content
                      : subscriptionDetails.content.slice(
                          (currentPage - 1) * pageSize,
                          currentPage * pageSize,
                        )
                  }
                  pagination={false}
                  rowKey="subscriptionId"
                  size="small"
                />
              ) : (
                <div className="p-8 text-center text-gray-500">
                  {searchKeyword ? t('table.noMatchedRecords') : t('table.noRecords')}
                </div>
              )}
            </div>

            {/* 分页 */}
            {subscriptionDetails.totalElements > 0 && (
              <div className="mt-3 flex justify-end">
                <Pagination
                  current={currentPage}
                  hideOnSinglePage={true}
                  onChange={(page) => {
                    setCurrentPage(page);

                    if (searchKeyword.trim()) {
                      fetchSubscriptionDetails(page, searchKeyword);
                    }
                  }}
                  pageSize={pageSize}
                  showQuickJumper={false}
                  showSizeChanger={false}
                  showTotal={(total) => t('pagination.total', { total })}
                  size="small"
                  total={subscriptionDetails.totalElements}
                />
              </div>
            )}
          </div>
        </Modal>
        <LoginPrompt
          contextMessage={tLoginPrompt('contextSubscribeProduct')}
          onClose={() => setLoginPromptOpen(false)}
          open={loginPromptOpen}
        />
      </>
    );
  },
);
