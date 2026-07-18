package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.params.order.QueryProductOrderParam;
import com.alibaba.himarket.dto.result.common.PageResult;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

class ProductOrderServiceImplListTest {

    @Test
    void listOrdersReturnsConsumerOrders() {
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
        when(productOrderRepository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<ProductOrder>>any(),
                        org.mockito.ArgumentMatchers
                                .<org.springframework.data.domain.Pageable>any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(
                                        ProductOrder.builder()
                                                .orderId("order-a")
                                                .consumerId("consumer-a")
                                                .productId("product-a")
                                                .productName("Legal Assistant")
                                                .amount(new BigDecimal("99.90"))
                                                .currency("CNY")
                                                .pricingMode(CommercePricingMode.ONE_TIME)
                                                .status(ProductOrderStatus.PAID)
                                                .build()),
                                PageRequest.of(0, 10),
                                1));
        when(paymentRecordRepository.findByOrderId("order-a"))
                .thenReturn(
                        List.of(
                                PaymentRecord.builder()
                                        .paymentRecordId("pay-a")
                                        .orderId("order-a")
                                        .provider("MANUAL")
                                        .amount(new BigDecimal("99.90"))
                                        .currency("CNY")
                                        .status(PaymentRecordStatus.SUCCEEDED)
                                        .build()));

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        PageResult<ProductOrderResult> result =
                service.listOrders(
                        "consumer-a", new QueryProductOrderParam(), PageRequest.of(0, 10));

        assertEquals(1, result.getNumber());
        assertEquals(1, result.getTotalElements());
        assertEquals("order-a", result.getContent().get(0).getOrderId());
        assertEquals(ProductOrderStatus.PAID, result.getContent().get(0).getStatus());
        assertEquals("Legal Assistant", result.getContent().get(0).getProductName());
        assertEquals(1, result.getContent().get(0).getPaymentRecords().size());
    }

    @Test
    void listOrdersRejectsForeignConsumer() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(consumerRepository.findByDeveloperIdAndConsumerId("dev-a", "consumer-a"))
                .thenReturn(Optional.empty());

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        assertThrows(
                RuntimeException.class,
                () ->
                        service.listOrders(
                                "consumer-a", new QueryProductOrderParam(), PageRequest.of(0, 10)));
    }
}
