package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.entity.Consumer;
import com.alibaba.himarket.entity.PaymentRecord;
import com.alibaba.himarket.entity.ProductOrder;
import com.alibaba.himarket.repository.ConsumerRepository;
import com.alibaba.himarket.repository.PaymentRecordRepository;
import com.alibaba.himarket.repository.ProductOrderRepository;
import com.alibaba.himarket.service.ConsumerService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.enums.PaymentRecordStatus;
import com.alibaba.himarket.support.enums.ProductOrderStatus;
import com.alibaba.himarket.support.product.CommercePricingMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductOrderServiceImplCancelTest {

    @Test
    void cancelOrderMarksPendingOrderCancelled() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(consumerRepository.findByDeveloperIdAndConsumerId("dev-a", "consumer-a"))
                .thenReturn(
                        Optional.of(
                                Consumer.builder()
                                        .consumerId("consumer-a")
                                        .developerId("dev-a")
                                        .portalId("portal-a")
                                        .name("Consumer A")
                                        .build()));
        when(productOrderRepository.findByOrderId("order-a"))
                .thenReturn(
                        Optional.of(
                                ProductOrder.builder()
                                        .orderId("order-a")
                                        .consumerId("consumer-a")
                                        .developerId("dev-a")
                                        .productId("product-a")
                                        .productName("Legal Assistant")
                                        .amount(new BigDecimal("99.90"))
                                        .currency("CNY")
                                        .pricingMode(CommercePricingMode.ONE_TIME)
                                        .status(ProductOrderStatus.PENDING_PAYMENT)
                                        .build()));
        when(paymentRecordRepository.findByOrderId("order-a")).thenReturn(List.of());
        when(paymentRecordRepository.save(org.mockito.ArgumentMatchers.any(PaymentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRecordRepository.findByOrderId("order-a"))
                .thenReturn(
                        List.of(
                                PaymentRecord.builder()
                                        .paymentRecordId("pay-a")
                                        .orderId("order-a")
                                        .provider("MANUAL")
                                        .amount(new BigDecimal("99.90"))
                                        .currency("CNY")
                                        .status(PaymentRecordStatus.CANCELLED)
                                        .build()));

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        ProductOrderResult result = service.cancelOrder("consumer-a", "order-a");

        assertEquals(ProductOrderStatus.CANCELLED, result.getStatus());
        assertEquals(PaymentRecordStatus.CANCELLED, result.getPaymentRecords().get(0).getStatus());
    }

    @Test
    void cancelOrderRejectsPaidOrder() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(consumerRepository.findByDeveloperIdAndConsumerId("dev-a", "consumer-a"))
                .thenReturn(
                        Optional.of(
                                Consumer.builder()
                                        .consumerId("consumer-a")
                                        .developerId("dev-a")
                                        .portalId("portal-a")
                                        .name("Consumer A")
                                        .build()));
        when(productOrderRepository.findByOrderId("order-a"))
                .thenReturn(
                        Optional.of(
                                ProductOrder.builder()
                                        .orderId("order-a")
                                        .consumerId("consumer-a")
                                        .developerId("dev-a")
                                        .status(ProductOrderStatus.PAID)
                                        .build()));

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        assertThrows(BusinessException.class, () -> service.cancelOrder("consumer-a", "order-a"));
    }
}
