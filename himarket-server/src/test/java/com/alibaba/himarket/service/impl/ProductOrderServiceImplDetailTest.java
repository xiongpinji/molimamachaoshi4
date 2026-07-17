package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.alibaba.himarket.support.enums.ProductOrderStatus;
import com.alibaba.himarket.support.product.CommercePricingMode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductOrderServiceImplDetailTest {

    @Test
    void getOrderReturnsOrderOwnedByConsumer() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(contextHolder.isDeveloper()).thenReturn(true);
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
                                        .status(ProductOrderStatus.PAID)
                                        .build()));

        when(paymentRecordRepository.findByOrderId("order-a"))
                .thenReturn(
                        java.util.List.of(
                                PaymentRecord.builder()
                                        .paymentRecordId("pay-a")
                                        .orderId("order-a")
                                        .provider("MANUAL")
                                        .amount(new BigDecimal("99.90"))
                                        .currency("CNY")
                                        .status(
                                                com.alibaba.himarket.support.enums
                                                        .PaymentRecordStatus.SUCCEEDED)
                                        .build()));

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        ProductOrderResult result = service.getOrder("consumer-a", "order-a");

        assertEquals("order-a", result.getOrderId());
        assertEquals("consumer-a", result.getConsumerId());
        assertEquals(ProductOrderStatus.PAID, result.getStatus());
    }
}
