export type ProductAccessState =
  | 'purchase_login'
  | 'purchase_available'
  | 'purchase_continue_payment'
  | 'purchase_activation_processing'
  | 'purchase_pending'
  | 'purchase_active'
  | 'subscribe_login'
  | 'subscribe_available'
  | 'subscribe_pending'
  | 'subscribe_active'
  | 'open_access';

interface ResolveProductAccessStateInput {
  hasApprovedSubscription: boolean;
  hasPendingSubscription: boolean;
  hasPendingPaymentOrder: boolean;
  hasPaidOrder: boolean;
  isLoggedIn: boolean;
  isPaid: boolean;
  subscribable: boolean;
}

export function resolveProductAccessState({
  hasApprovedSubscription,
  hasPendingSubscription,
  hasPendingPaymentOrder,
  hasPaidOrder,
  isLoggedIn,
  isPaid,
  subscribable,
}: ResolveProductAccessStateInput): ProductAccessState {
  if (!subscribable) {
    return 'open_access';
  }

  if (isPaid) {
    if (!isLoggedIn) {
      return 'purchase_login';
    }
    if (hasApprovedSubscription) {
      return 'purchase_active';
    }
    if (hasPendingPaymentOrder) {
      return 'purchase_continue_payment';
    }
    if (hasPaidOrder) {
      return 'purchase_activation_processing';
    }
    if (hasPendingSubscription) {
      return 'purchase_pending';
    }
    return 'purchase_available';
  }

  if (!isLoggedIn) {
    return 'subscribe_login';
  }
  if (hasApprovedSubscription) {
    return 'subscribe_active';
  }
  if (hasPendingSubscription) {
    return 'subscribe_pending';
  }
  return 'subscribe_available';
}
