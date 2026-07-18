package com.alibaba.himarket.dto.params.order;

import com.alibaba.himarket.support.enums.PaymentRecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCallbackParam {

    @NotBlank(message = "Provider cannot be blank")
    private String provider;

    private String providerTransactionId;

    @NotNull(message = "Status cannot be null")
    private PaymentRecordStatus status;

    private String callbackPayload;
}
