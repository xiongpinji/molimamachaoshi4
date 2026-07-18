import {
  ArrowRightOutlined,
  CalendarOutlined,
  IdcardOutlined,
  MailOutlined,
  SafetyCertificateOutlined,
  ShoppingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Input, Pagination, Select, Skeleton, Tag, Typography, message } from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

import { ChangePasswordForm } from '../components/ChangePasswordForm';
import { Layout } from '../components/Layout';
import { notifyAuthInvalidated } from '../hooks/useAuth';
import APIs, { type IDeveloperInfo } from '../lib/apis';
import { clearCachedUserInfo } from '../lib/userInfoCache';
import { formatDateTime } from '../lib/utils';
import { getOrderStatusColor, getOrderStatusLabel } from '../lib/utils/orderStatus';

type ProfileSection = 'identity' | 'orders' | 'profile' | 'security';

const { Title } = Typography;

const getInitials = (name: string) => {
  if (!name) return 'U';
  if (/[\u4e00-\u9fa5]/.test(name)) {
    return name.charAt(0);
  }
  return name.charAt(0).toUpperCase();
};

const Profile: React.FC = () => {
  const { t } = useTranslation('profile');
  const navigate = useNavigate();
  const [activeSection, setActiveSection] = useState<ProfileSection>('profile');
  const [developerInfo, setDeveloperInfo] = useState<IDeveloperInfo | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [changePasswordLoading, setChangePasswordLoading] = useState(false);
  const [primaryConsumerId, setPrimaryConsumerId] = useState<string | null>(null);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [orders, setOrders] = useState<
    Array<{
      orderId: string;
      productName?: string;
      amount: number;
      currency: string;
      status: string;
    }>
  >([]);
  const [ordersPage, setOrdersPage] = useState(1);
  const [ordersPageSize] = useState(10);
  const [ordersTotal, setOrdersTotal] = useState(0);
  const [orderSearchKeyword, setOrderSearchKeyword] = useState('');
  const [orderStatusFilter, setOrderStatusFilter] = useState<string | undefined>();

  useEffect(() => {
    APIs.getDeveloperInfo()
      .then((response) => {
        setDeveloperInfo(response.data || null);
      })
      .catch(() => {
        setDeveloperInfo(null);
      })
      .finally(() => {
        setProfileLoading(false);
      });

    APIs.getPrimaryConsumer()
      .then((response) => {
        setPrimaryConsumerId(response.data?.consumerId || null);
      })
      .catch(() => {
        setPrimaryConsumerId(null);
      });
  }, []);

  useEffect(() => {
    if (activeSection !== 'orders' || !primaryConsumerId) {
      return;
    }

    setOrdersLoading(true);
    APIs.getProductOrders(primaryConsumerId, { page: ordersPage, size: ordersPageSize })
      .then((response) => {
        setOrders(response.data?.content || []);
        setOrdersTotal(response.data?.totalElements || 0);
      })
      .catch(() => {
        setOrders([]);
        setOrdersTotal(0);
      })
      .finally(() => {
        setOrdersLoading(false);
      });
  }, [activeSection, ordersPage, ordersPageSize, primaryConsumerId]);

  const handleChangePassword = async (values: { newPassword: string; oldPassword: string }) => {
    setChangePasswordLoading(true);
    try {
      await APIs.changeDeveloperPassword(values);
      message.success(t('changePasswordSuccess'), 1);
      localStorage.removeItem('access_token');
      clearCachedUserInfo();
      notifyAuthInvalidated();
      navigate('/login');
    } finally {
      setChangePasswordLoading(false);
    }
  };

  const displayName = developerInfo?.username || developerInfo?.email || t('unknownDeveloper');
  const displayEmail = developerInfo?.email;
  const avatar = developerInfo?.avatarUrl;
  const loadingProfile = profileLoading;

  const navItems = [
    { icon: <UserOutlined />, key: 'profile' as const, label: t('profileInfo') },
    { icon: <ShoppingOutlined />, key: 'orders' as const, label: t('purchaseHistory') },
    { icon: <SafetyCertificateOutlined />, key: 'security' as const, label: t('accountSecurity') },
    {
      comingSoon: true,
      icon: <IdcardOutlined />,
      key: 'identity' as const,
      label: t('thirdPartyAccounts'),
    },
  ];

  const profileFacts = [
    {
      icon: <UserOutlined />,
      label: t('username'),
      value: developerInfo?.username || t('notSet'),
    },
    {
      icon: <MailOutlined />,
      label: t('email'),
      value: displayEmail || t('notSet'),
    },
    {
      icon: <CalendarOutlined />,
      label: t('joinedAt'),
      value: developerInfo?.createAt ? formatDateTime(developerInfo.createAt) : '-',
    },
  ];

  const filteredOrders = useMemo(() => {
    return orders.filter((order) => {
      const matchesKeyword =
        !orderSearchKeyword ||
        (order.productName || '').toLowerCase().includes(orderSearchKeyword.toLowerCase()) ||
        order.orderId.toLowerCase().includes(orderSearchKeyword.toLowerCase());
      const matchesStatus = !orderStatusFilter || order.status === orderStatusFilter;
      return matchesKeyword && matchesStatus;
    });
  }, [orderSearchKeyword, orderStatusFilter, orders]);

  const renderProfileContent = () => (
    <>
      <div>
        <h1 className="m-0 text-xl font-semibold text-gray-950">{t('profileInfo')}</h1>
        <p className="mt-2 text-sm leading-6 text-gray-500">{t('profileInfoDescription')}</p>
      </div>

      {loadingProfile ? (
        <Skeleton active className="mt-6" paragraph={{ rows: 6 }} />
      ) : (
        <dl className="mt-6 grid gap-3 md:grid-cols-2">
          {profileFacts.map((item) => (
            <div
              className="min-w-0 rounded-[14px] border border-[#E3EAF4] bg-[#FBFCFF] px-4 py-3"
              key={item.label}
            >
              <dt className="flex items-center gap-2 text-xs font-medium text-gray-500">
                <span className="text-sm text-gray-400">{item.icon}</span>
                <span>{item.label}</span>
              </dt>
              <dd className="mt-2 truncate text-sm font-normal text-gray-700">{item.value}</dd>
            </div>
          ))}
        </dl>
      )}
    </>
  );

  const renderOrdersContent = () => (
    <>
      <div>
        <h1 className="m-0 text-xl font-semibold text-gray-950">{t('purchaseHistory')}</h1>
        <p className="mt-2 text-sm leading-6 text-gray-500">{t('purchaseHistoryDescription')}</p>
      </div>

      <div className="mt-6 flex flex-col gap-3 md:flex-row">
        <Input
          allowClear
          className="md:w-80"
          onChange={(e) => setOrderSearchKeyword(e.target.value)}
          placeholder={t('orderSearchPlaceholder')}
          value={orderSearchKeyword}
        />
        <Select
          allowClear
          className="md:w-56"
          onChange={(value) => setOrderStatusFilter(value)}
          placeholder={t('orderStatusFilterPlaceholder')}
          value={orderStatusFilter}
        >
          <Select.Option value="PENDING_PAYMENT">
            {t('orderStatusLabels.PENDING_PAYMENT')}
          </Select.Option>
          <Select.Option value="PAID">{t('orderStatusLabels.PAID')}</Select.Option>
          <Select.Option value="FAILED">{t('orderStatusLabels.FAILED')}</Select.Option>
          <Select.Option value="CANCELLED">{t('orderStatusLabels.CANCELLED')}</Select.Option>
        </Select>
      </div>

      {ordersLoading ? (
        <Skeleton active className="mt-6" paragraph={{ rows: 4 }} />
      ) : filteredOrders.length > 0 ? (
        <>
          <div className="mt-6 space-y-3">
            {filteredOrders.map((order) => (
              <button
                className="w-full rounded-[14px] border border-[#E3EAF4] bg-[#FBFCFF] px-4 py-3 text-left transition-colors hover:border-[#C8D5E6]"
                key={order.orderId}
                onClick={() => navigate(`/orders/${order.orderId}`)}
                type="button"
              >
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-gray-900">
                      {order.productName || order.orderId}
                    </div>
                    <div className="mt-1 text-xs text-gray-500">{order.orderId}</div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-semibold text-gray-900">
                      {order.currency} {order.amount}
                    </div>
                    <div className="mt-1">
                      <Tag color={getOrderStatusColor(order.status)}>
                        {getOrderStatusLabel(order.status, t)}
                      </Tag>
                    </div>
                  </div>
                </div>
              </button>
            ))}
          </div>
          {ordersTotal > ordersPageSize && (
            <div className="mt-6 flex justify-end">
              <Pagination
                current={ordersPage}
                onChange={setOrdersPage}
                pageSize={ordersPageSize}
                showSizeChanger={false}
                total={ordersTotal}
              />
            </div>
          )}
        </>
      ) : (
        <div className="mt-6 rounded-[16px] border border-dashed border-[#DDE5F0] bg-[#FBFCFF] px-6 py-10 text-center text-sm text-gray-500">
          {t('noOrders')}
        </div>
      )}
    </>
  );

  const renderSecurityContent = () => (
    <>
      <div>
        <h1 className="m-0 text-xl font-semibold text-gray-950">{t('accountSecurity')}</h1>
        <p className="mt-2 text-sm leading-6 text-gray-500">{t('passwordDescription')}</p>
      </div>

      <ChangePasswordForm loading={changePasswordLoading} onSubmit={handleChangePassword} />
    </>
  );

  const renderIdentityContent = () => (
    <>
      <div>
        <h1 className="m-0 text-xl font-semibold text-gray-950">{t('thirdPartyAccounts')}</h1>
        <p className="mt-2 text-sm leading-6 text-gray-500">{t('thirdPartyDescription')}</p>
      </div>

      <div className="mt-6 rounded-[16px] border border-dashed border-[#DDE5F0] bg-[#FBFCFF] px-6 py-10 text-center">
        <div className="inline-flex rounded-[10px] bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-500">
          {t('comingSoon')}
        </div>
      </div>
    </>
  );

  const renderActiveContent = () => {
    if (activeSection === 'security') return renderSecurityContent();
    if (activeSection === 'identity') return renderIdentityContent();
    if (activeSection === 'orders') return renderOrdersContent();
    return renderProfileContent();
  };

  return (
    <Layout>
      <div className="w-full">
        <section className="min-h-[calc(100vh-96px)] rounded-2xl border border-white/40 bg-white/90 p-6 shadow-xs backdrop-blur-xl">
          <div className="mb-5 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <Title className="text-gray-900" level={2}>
              {t('title')}
            </Title>
            <Button
              className="w-fit rounded-lg"
              icon={<ArrowRightOutlined />}
              iconPosition="end"
              onClick={() => navigate('/consumers')}
              type="primary"
            >
              {t('manageConsumers')}
            </Button>
          </div>

          <div className="grid w-full gap-6 lg:grid-cols-[260px_minmax(0,1fr)]">
            <aside className="min-w-0">
              <div className="rounded-[16px] border border-[#E6ECF5] bg-[#FBFCFF] p-4">
                {loadingProfile ? (
                  <div>
                    <Skeleton.Avatar active size={64} />
                    <Skeleton active className="mt-4" paragraph={{ rows: 2 }} />
                  </div>
                ) : (
                  <div className="mb-6 flex items-center gap-3">
                    {avatar ? (
                      <img
                        alt={displayName}
                        className="h-12 w-12 rounded-full object-cover"
                        src={avatar}
                      />
                    ) : (
                      <div className="flex h-12 w-12 items-center justify-center rounded-[16px] border border-[#E3EAF4] bg-white text-lg font-semibold text-gray-600">
                        {getInitials(displayName)}
                      </div>
                    )}
                    <div className="min-w-0">
                      <div className="truncate text-base font-medium text-gray-800">
                        {displayName}
                      </div>
                      {displayEmail && (
                        <div className="mt-1 truncate text-xs text-gray-400">{displayEmail}</div>
                      )}
                      <div className="mt-2 inline-flex rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-medium text-emerald-700">
                        {t('activeAccount')}
                      </div>
                    </div>
                  </div>
                )}

                <nav className="space-y-1">
                  {navItems.map((item) => {
                    const isActive = activeSection === item.key;

                    return (
                      <button
                        className={`flex w-full items-center gap-3 rounded-[12px] px-3 py-3 text-left text-sm font-medium transition-colors ${
                          isActive
                            ? 'bg-white text-colorPrimary shadow-[0_1px_4px_rgba(66,76,112,0.08)]'
                            : 'text-gray-600 hover:bg-white hover:text-gray-950'
                        }`}
                        key={item.key}
                        onClick={() => setActiveSection(item.key)}
                        type="button"
                      >
                        <span
                          className={`text-base ${
                            isActive ? 'text-colorPrimary' : 'text-gray-500'
                          }`}
                        >
                          {item.icon}
                        </span>
                        <span className="min-w-0 flex-1 truncate">{item.label}</span>
                        {item.comingSoon && (
                          <span className="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] font-medium text-gray-500">
                            {t('comingSoon')}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </nav>
              </div>
            </aside>

            <main className="min-w-0 rounded-[16px] border border-[#E6ECF5] bg-white p-6 shadow-[0_1px_3px_rgba(15,23,42,0.04)]">
              {renderActiveContent()}
            </main>
          </div>
        </section>
      </div>
    </Layout>
  );
};

export default Profile;
