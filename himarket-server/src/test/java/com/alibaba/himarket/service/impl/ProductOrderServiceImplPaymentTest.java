package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.params.order.CreateProductOrderParam;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
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
import com.alibaba.himarket.support.enums.ProductType;
import com.alibaba.himarket.support.product.CommerceConfig;
import com.alibaba.himarket.support.product.CommercePricingMode;
import com.alibaba.himarket.support.product.ProductFeature;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ProductOrderServiceImplPaymentTest {

    @Test
    void markOrderPaidApprovesSubscriptionForPaidAgent() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);
        AtomicReference<ProductOrder> savedOrder = new AtomicReference<>();
        AtomicReference<PaymentRecord> savedPaymentRecord = new AtomicReference<>();

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(contextHolder.getPortal()).thenReturn("portal-a");
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
        when(productOrderRepository.findFirstByConsumerIdAndProductIdAndStatusInOrderByCreateAtDesc(
                        any(), any(), any()))
                .thenReturn(Optional.empty());
        when(productService.getProduct("product-a"))
                .thenReturn(
                        paidProduct(
                                "product-a",
                                "Legal Assistant",
                                new BigDecimal("99.90"),
                                "CNY",
                                CommercePricingMode.ONE_TIME));
        when(productOrderRepository.save(any(ProductOrder.class)))
                .thenAnswer(
                        invocation -> {
                            ProductOrder order = invocation.getArgument(0);
                            savedOrder.set(order);
                            return order;
                        });
        when(productOrderRepository.findByOrderId(any()))
                .thenAnswer(invocation -> Optional.ofNullable(savedOrder.get()));
        when(paymentRecordRepository.findByOrderId(any())).thenReturn(List.of());
        when(paymentRecordRepository.save(any(PaymentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            PaymentRecord paymentRecord = invocation.getArgument(0);
                            savedPaymentRecord.set(paymentRecord);
                            return paymentRecord;
                        });

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        ProductOrderResult createdOrder =
                service.createOrder(
                        "consumer-a",
                        CreateProductOrderParam.builder().productId("product-a").build());

        ProductOrderResult paidOrder =
                service.markOrderPaid("consumer-a", createdOrder.getOrderId());

        assertEquals(ProductOrderStatus.PAID, paidOrder.getStatus());
        assertNotNull(paidOrder.getPaidAt());
        assertNotNull(savedPaymentRecord.get());
        assertEquals(PaymentRecordStatus.SUCCEEDED, savedPaymentRecord.get().getStatus());
        assertEquals("manual-confirmation", savedPaymentRecord.get().getCallbackPayload());
        verify(consumerService).activatePurchasedProduct("consumer-a", "product-a");
    }

    private static ProductResult paidProduct(
            String productId,
            String productName,
            BigDecimal amount,
            String currency,
            CommercePricingMode pricingMode) {
        ProductResult result = new ProductResult();
        result.setProductId(productId);
        result.setName(productName);
        result.setType(ProductType.AGENT_API);
        result.setFeature(
                ProductFeature.builder()
                        .commerceConfig(
                                CommerceConfig.builder()
                                        .enabled(true)
                                        .amount(amount)
                                        .currency(currency)
                                        .pricingMode(pricingMode)
                                        .build())
                        .build());
        return result;
    }
}
