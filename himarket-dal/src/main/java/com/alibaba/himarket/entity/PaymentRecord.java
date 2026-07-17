package com.alibaba.himarket.entity;

import com.alibaba.himarket.support.enums.PaymentRecordStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "payment_record",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {"payment_record_id"},
                    name = "uk_payment_record_id")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_record_id", length = 64, nullable = false)
    private String paymentRecordId;

    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Column(name = "provider", length = 64, nullable = false)
    private String provider;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 16, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private PaymentRecordStatus status;

    @Column(name = "callback_payload", columnDefinition = "text")
    private String callbackPayload;
}
