package com.alibaba.himarket.dto.result.order;

import com.alibaba.himarket.dto.converter.OutputConverter;
import com.alibaba.himarket.entity.PaymentRecord;
import com.alibaba.himarket.support.enums.PaymentRecordStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordResult implements OutputConverter<PaymentRecordResult, PaymentRecord> {

    private String paymentRecordId;

    private String orderId;

    private String provider;

    private String providerTransactionId;

    private BigDecimal amount;

    private String currency;

    private PaymentRecordStatus status;

    private String callbackPayload;

    private LocalDateTime createAt;

    private LocalDateTime updatedAt;
}
