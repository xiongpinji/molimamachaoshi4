package com.alibaba.himarket.repository;

import com.alibaba.himarket.entity.PaymentRecord;
import java.util.List;
import java.util.Optional;

public interface PaymentRecordRepository extends BaseRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByPaymentRecordId(String paymentRecordId);

    Optional<PaymentRecord> findFirstByOrderIdOrderByCreateAtDesc(String orderId);

    List<PaymentRecord> findByOrderId(String orderId);
}
