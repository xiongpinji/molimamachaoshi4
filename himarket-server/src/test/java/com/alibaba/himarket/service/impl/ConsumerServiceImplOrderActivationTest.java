package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.result.product.ProductRefResult;
import com.alibaba.himarket.entity.Consumer;
import com.alibaba.himarket.entity.ConsumerCredential;
import com.alibaba.himarket.entity.ProductSubscription;
import com.alibaba.himarket.repository.ConsumerCredentialRepository;
import com.alibaba.himarket.repository.ConsumerRefRepository;
import com.alibaba.himarket.repository.ConsumerRepository;
import com.alibaba.himarket.repository.SubscriptionRepository;
import com.alibaba.himarket.service.GatewayService;
import com.alibaba.himarket.service.PortalService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.consumer.ConsumerAuthConfig;
import com.alibaba.himarket.support.enums.GatewayType;
import com.alibaba.himarket.support.enums.SourceType;
import com.alibaba.himarket.support.enums.SubscriptionStatus;
import com.alibaba.himarket.support.gateway.GatewayConfig;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConsumerServiceImplOrderActivationTest {

    @Test
    void activatePurchasedProductCreatesSubscriptionAndAuthorizesGatewayProduct() {
        PortalService portalService = mock(PortalService.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        GatewayService gatewayService = mock(GatewayService.class);
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerCredentialRepository credentialRepository =
                mock(ConsumerCredentialRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        ProductService productService = mock(ProductService.class);
        ConsumerRefRepository consumerRefRepository = mock(ConsumerRefRepository.class);
        AtomicReference<ProductSubscription> savedSubscription = new AtomicReference<>();

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
        when(credentialRepository.findByConsumerId("consumer-a"))
                .thenReturn(
                        Optional.of(ConsumerCredential.builder().consumerId("consumer-a").build()));
        when(subscriptionRepository.findByConsumerIdAndProductId("consumer-a", "product-a"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(ProductSubscription.class)))
                .thenAnswer(
                        invocation -> {
                            ProductSubscription subscription = invocation.getArgument(0);
                            savedSubscription.set(subscription);
                            return subscription;
                        });
        when(subscriptionRepository.saveAndFlush(any(ProductSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(gatewayService.getGatewayConfig("gw-a"))
                .thenReturn(
                        GatewayConfig.builder()
                                .gatewayId("gw-a")
                                .gatewayType(GatewayType.APIG_API)
                                .build());
        when(consumerRefRepository.findAllByConsumerIdAndGatewayType(
                        eq("consumer-a"), eq(GatewayType.APIG_API), any()))
                .thenReturn(List.of());
        when(gatewayService.createConsumer(any(), any(), any())).thenReturn("gw-consumer-a");
        ProductRefResult productRef = new ProductRefResult();
        productRef.setProductId("product-a");
        productRef.setGatewayId("gw-a");
        productRef.setSourceType(SourceType.GATEWAY);
        when(productService.getProductRef("product-a")).thenReturn(productRef);
        when(gatewayService.authorizeConsumer(any(), any(), any()))
                .thenReturn(new ConsumerAuthConfig());

        ConsumerServiceImpl service =
                new ConsumerServiceImpl(
                        portalService,
                        consumerRepository,
                        gatewayService,
                        contextHolder,
                        credentialRepository,
                        subscriptionRepository,
                        productService,
                        consumerRefRepository);

        service.activatePurchasedProduct("consumer-a", "product-a");

        assertEquals(SubscriptionStatus.APPROVED, savedSubscription.get().getStatus());
        assertNotNull(savedSubscription.get().getConsumerAuthConfig());
        verify(gatewayService).authorizeConsumer(eq("gw-a"), any(), eq(productRef));
    }

    @Test
    void activatePurchasedProductReturnsApprovedSubscriptionIdempotently() {
        PortalService portalService = mock(PortalService.class);
        ConsumerRepository consumerRepository = mock(ConsumerRepository.class);
        GatewayService gatewayService = mock(GatewayService.class);
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerCredentialRepository credentialRepository =
                mock(ConsumerCredentialRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        ProductService productService = mock(ProductService.class);
        ConsumerRefRepository consumerRefRepository = mock(ConsumerRefRepository.class);

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
        when(credentialRepository.findByConsumerId("consumer-a"))
                .thenReturn(
                        Optional.of(ConsumerCredential.builder().consumerId("consumer-a").build()));
        when(subscriptionRepository.findByConsumerIdAndProductId("consumer-a", "product-a"))
                .thenReturn(
                        Optional.of(
                                ProductSubscription.builder()
                                        .subscriptionId("sub-a")
                                        .productId("product-a")
                                        .consumerId("consumer-a")
                                        .status(SubscriptionStatus.APPROVED)
                                        .build()));

        ConsumerServiceImpl service =
                new ConsumerServiceImpl(
                        portalService,
                        consumerRepository,
                        gatewayService,
                        contextHolder,
                        credentialRepository,
                        subscriptionRepository,
                        productService,
                        consumerRefRepository);

        service.activatePurchasedProduct("consumer-a", "product-a");

        verify(gatewayService, never()).authorizeConsumer(any(), any(), any());
        verify(subscriptionRepository, never()).saveAndFlush(any(ProductSubscription.class));
    }
}
