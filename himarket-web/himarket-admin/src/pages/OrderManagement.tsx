import { AdminPageHeader } from '@/components/common';
import { OrderListModal } from '@/components/subscription/OrderListModal';
import { useLocale } from '@/contexts/LocaleContext';

export default function OrderManagement() {
  const { t } = useLocale();

  return (
    <div className="space-y-6">
      <AdminPageHeader description={t('page.orders.description')} title={t('page.orders.title')} />

      <OrderListModal
        consumerId="admin-global"
        consumerName={t('page.orders.allConsumers')}
        embedded={true}
        onCancel={() => undefined}
        useTopLevelApi={true}
        visible={true}
      />
    </div>
  );
}
