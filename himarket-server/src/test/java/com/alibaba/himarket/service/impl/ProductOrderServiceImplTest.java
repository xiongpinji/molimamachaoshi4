/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.params.order.CreateProductOrderParam;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
import com.alibaba.himarket.entity.Consumer;
import com.alibaba.himarket.entity.ProductOrder;
import com.alibaba.himarket.repository.ConsumerRepository;
import com.alibaba.himarket.repository.PaymentRecordRepository;
import com.alibaba.himarket.repository.ProductOrderRepository;
import com.alibaba.himarket.service.ConsumerService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.enums.ProductOrderStatus;
import com.alibaba.himarket.support.enums.ProductType;
import com.alibaba.himarket.support.product.CommerceConfig;
import com.alibaba.himarket.support.product.CommercePricingMode;
import com.alibaba.himarket.support.product.ProductFeature;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ProductOrderServiceImplTest {

    @Test
    void createOrderSnapshotsPaidProductPricing() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ProductService productService = mock(ProductService.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        AtomicReference<ProductOrder> savedOrder = new AtomicReference<>();

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
                        eq("consumer-a"), eq("product-a"), any()))
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

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        ProductOrderResult result =
                service.createOrder(
                        "consumer-a",
                        CreateProductOrderParam.builder().productId("product-a").build());

        assertEquals("consumer-a", result.getConsumerId());
        assertEquals("product-a", result.getProductId());
        assertEquals("Legal Assistant", result.getProductName());
        assertEquals(ProductOrderStatus.PENDING_PAYMENT, result.getStatus());
        assertEquals(0, new BigDecimal("99.90").compareTo(result.getAmount()));
        assertEquals("CNY", result.getCurrency());
        assertEquals(CommercePricingMode.ONE_TIME, result.getPricingMode());
        assertEquals("portal-a", savedOrder.get().getPortalId());
        assertEquals("dev-a", savedOrder.get().getDeveloperId());
    }

    @Test
    void createOrderRejectsExistingActiveOrderForSameProduct() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ProductService productService = mock(ProductService.class);
        ConsumerService consumerService = mock(ConsumerService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(contextHolder.isDeveloper()).thenReturn(true);
        when(consumerRepository.findByDeveloperIdAndConsumerId("dev-a", "consumer-a"))
                .thenReturn(
                        Optional.of(
                                Consumer.builder()
                                        .consumerId("consumer-a")
                                        .developerId("dev-a")
                                        .portalId("portal-a")
                                        .build()));
        when(productOrderRepository.findFirstByConsumerIdAndProductIdAndStatusInOrderByCreateAtDesc(
                        eq("consumer-a"), eq("product-a"), any()))
                .thenReturn(
                        Optional.of(
                                ProductOrder.builder()
                                        .orderId("order-a")
                                        .consumerId("consumer-a")
                                        .productId("product-a")
                                        .status(ProductOrderStatus.PENDING_PAYMENT)
                                        .build()));
        when(productService.getProduct("product-a"))
                .thenReturn(
                        paidProduct(
                                "product-a",
                                "Legal Assistant",
                                new BigDecimal("99.90"),
                                "CNY",
                                CommercePricingMode.ONE_TIME));

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        assertThrows(
                BusinessException.class,
                () ->
                        service.createOrder(
                                "consumer-a",
                                CreateProductOrderParam.builder().productId("product-a").build()));
    }

    @Test
    void createOrderRejectsProductWithoutEnabledCommerceConfig() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ProductOrderRepository productOrderRepository = mock(ProductOrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        ProductService productService = mock(ProductService.class);
        ConsumerService consumerService = mock(ConsumerService.class);

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
                        eq("consumer-a"), eq("product-free"), any()))
                .thenReturn(Optional.empty());
        when(productService.getProduct("product-free"))
                .thenReturn(
                        paidProduct(
                                "product-free",
                                "Free Agent",
                                new BigDecimal("0.00"),
                                "CNY",
                                CommercePricingMode.ONE_TIME,
                                false));

        ProductOrderServiceImpl service =
                new ProductOrderServiceImpl(
                        contextHolder,
                        consumerRepository,
                        productOrderRepository,
                        paymentRecordRepository,
                        productService,
                        consumerService);

        assertThrows(
                BusinessException.class,
                () ->
                        service.createOrder(
                                "consumer-a",
                                CreateProductOrderParam.builder()
                                        .productId("product-free")
                                        .build()));
    }

    private static ProductResult paidProduct(
            String productId,
            String productName,
            BigDecimal amount,
            String currency,
            CommercePricingMode pricingMode) {
        return paidProduct(productId, productName, amount, currency, pricingMode, true);
    }

    private static ProductResult paidProduct(
            String productId,
            String productName,
            BigDecimal amount,
            String currency,
            CommercePricingMode pricingMode,
            boolean enabled) {
        ProductResult result = new ProductResult();
        result.setProductId(productId);
        result.setName(productName);
        result.setType(ProductType.AGENT_API);
        result.setFeature(
                ProductFeature.builder()
                        .commerceConfig(
                                CommerceConfig.builder()
                                        .enabled(enabled)
                                        .amount(amount)
                                        .currency(currency)
                                        .pricingMode(pricingMode)
                                        .build())
                        .build());
        return result;
    }
}
