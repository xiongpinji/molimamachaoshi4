package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.params.coding.CreateCodingSessionParam;
import com.alibaba.himarket.dto.result.consumer.ConsumerResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
import com.alibaba.himarket.dto.result.product.SubscriptionResult;
import com.alibaba.himarket.repository.CodingSessionRepository;
import com.alibaba.himarket.service.ConsumerService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.enums.SubscriptionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodingSessionServiceImplEntitlementTest {

    @Test
    void createSessionRejectsUnapprovedModel() {
        CodingSessionRepository sessionRepository = mock(CodingSessionRepository.class);
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        ProductResult product = new ProductResult();
        product.setProductId("product-a");
        product.setSubscribable(true);
        when(productService.getProduct("product-a")).thenReturn(product);
        when(consumerService.getPrimaryConsumer("dev-a"))
                .thenReturn(
                        new ConsumerResult()
                                .convertFrom(
                                        com.alibaba.himarket.entity.Consumer.builder()
                                                .consumerId("consumer-a")
                                                .build()));
        when(consumerService.listConsumerSubscriptions("consumer-a"))
                .thenReturn(List.of(subscription("product-b", SubscriptionStatus.APPROVED.name())));

        CodingSessionServiceImpl service =
                new CodingSessionServiceImpl(
                        sessionRepository, contextHolder, consumerService, productService);

        CreateCodingSessionParam param = new CreateCodingSessionParam();
        param.setCliSessionId("cli-session-a");
        param.setModelProductId("product-a");

        assertThrows(BusinessException.class, () -> service.createSession(param));
    }

    @Test
    void createSessionAllowsOpenAccessModel() {
        CodingSessionRepository sessionRepository = mock(CodingSessionRepository.class);
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ProductService productService = mock(ProductService.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        ProductResult product = new ProductResult();
        product.setProductId("product-open");
        product.setSubscribable(false);
        when(productService.getProduct("product-open")).thenReturn(product);

        CodingSessionServiceImpl service =
                new CodingSessionServiceImpl(
                        sessionRepository, contextHolder, consumerService, productService);

        CreateCodingSessionParam param = new CreateCodingSessionParam();
        param.setCliSessionId("cli-session-a");
        param.setModelProductId("product-open");

        assertDoesNotThrow(() -> service.createSession(param));
    }

    private static SubscriptionResult subscription(String productId, String status) {
        SubscriptionResult result = new SubscriptionResult();
        result.setProductId(productId);
        result.setStatus(status);
        return result;
    }
}
