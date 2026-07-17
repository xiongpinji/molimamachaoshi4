package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.result.mcp.McpConfigResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
import com.alibaba.himarket.entity.Consumer;
import com.alibaba.himarket.entity.ProductSubscription;
import com.alibaba.himarket.repository.ConsumerCredentialRepository;
import com.alibaba.himarket.repository.ConsumerRepository;
import com.alibaba.himarket.repository.SubscriptionRepository;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.service.hichat.manager.ToolManager;
import com.alibaba.himarket.support.enums.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class McpToolServiceImplEntitlementTest {

    @Test
    void listMcpToolsRejectsPendingSubscription() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ConsumerCredentialRepository credentialRepository =
                mock(ConsumerCredentialRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        ProductService productService = mock(ProductService.class);
        ToolManager toolManager = mock(ToolManager.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(consumerRepository.findByDeveloperIdAndIsPrimary("dev-a", true))
                .thenReturn(
                        Optional.of(
                                Consumer.builder()
                                        .consumerId("consumer-a")
                                        .developerId("dev-a")
                                        .isPrimary(true)
                                        .build()));
        ProductResult product = new ProductResult();
        product.setProductId("product-a");
        product.setSubscribable(true);
        when(productService.getProduct("product-a")).thenReturn(product);
        when(subscriptionRepository.findByConsumerIdAndProductId("consumer-a", "product-a"))
                .thenReturn(
                        Optional.of(
                                ProductSubscription.builder()
                                        .consumerId("consumer-a")
                                        .productId("product-a")
                                        .status(SubscriptionStatus.PENDING)
                                        .build()));

        McpToolServiceImpl service =
                new McpToolServiceImpl(
                        contextHolder,
                        consumerRepository,
                        credentialRepository,
                        subscriptionRepository,
                        productService,
                        toolManager);

        assertThrows(
                BusinessException.class,
                () -> service.listMcpTools("product-a", new McpConfigResult()));
    }

    @Test
    void listMcpToolsAllowsOpenAccessProductWithoutSubscription() {
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        ConsumerCredentialRepository credentialRepository =
                mock(ConsumerCredentialRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        ProductService productService = mock(ProductService.class);
        ToolManager toolManager = mock(ToolManager.class);

        ProductResult product = new ProductResult();
        product.setProductId("product-open");
        product.setSubscribable(false);
        when(productService.getProduct("product-open")).thenReturn(product);
        when(toolManager.fetchTools(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        McpToolServiceImpl service =
                new McpToolServiceImpl(
                        contextHolder,
                        consumerRepository,
                        credentialRepository,
                        subscriptionRepository,
                        productService,
                        toolManager);

        assertDoesNotThrow(() -> service.listMcpTools("product-open", new McpConfigResult()));
    }
}
