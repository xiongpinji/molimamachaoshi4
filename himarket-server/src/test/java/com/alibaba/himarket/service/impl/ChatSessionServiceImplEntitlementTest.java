package com.alibaba.himarket.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.dto.params.chat.CreateChatSessionParam;
import com.alibaba.himarket.dto.result.consumer.ConsumerResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
import com.alibaba.himarket.dto.result.product.SubscriptionResult;
import com.alibaba.himarket.repository.ChatRepository;
import com.alibaba.himarket.repository.ChatSessionRepository;
import com.alibaba.himarket.service.ConsumerService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.enums.SubscriptionStatus;
import com.alibaba.himarket.support.enums.TalkType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ChatSessionServiceImplEntitlementTest {

    @Test
    void createSessionRejectsUnapprovedProduct() {
        ChatSessionRepository sessionRepository = mock(ChatSessionRepository.class);
        ChatRepository chatRepository = mock(ChatRepository.class);
        ProductService productService = mock(ProductService.class);
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(contextHolder.isDeveloper()).thenReturn(true);
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
                .thenReturn(
                        List.of(
                                subscription("product-a", SubscriptionStatus.PENDING.name()),
                                subscription("product-b", SubscriptionStatus.APPROVED.name())));

        ChatSessionServiceImpl service =
                new ChatSessionServiceImpl(
                        sessionRepository,
                        chatRepository,
                        productService,
                        contextHolder,
                        consumerService,
                        eventPublisher);

        CreateChatSessionParam param = new CreateChatSessionParam();
        param.setName("session-a");
        param.setTalkType(TalkType.MODEL);
        param.setProducts(List.of("product-a"));

        assertThrows(BusinessException.class, () -> service.createSession(param));
    }

    @Test
    void createSessionAllowsOpenAccessProductWithoutSubscription() {
        ChatSessionRepository sessionRepository = mock(ChatSessionRepository.class);
        ChatRepository chatRepository = mock(ChatRepository.class);
        ProductService productService = mock(ProductService.class);
        ContextHolder contextHolder = mock(ContextHolder.class);
        ConsumerService consumerService = mock(ConsumerService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        when(contextHolder.getUser()).thenReturn("dev-a");
        when(contextHolder.isDeveloper()).thenReturn(true);
        ProductResult product = new ProductResult();
        product.setProductId("product-open");
        product.setSubscribable(false);
        when(productService.getProduct("product-open")).thenReturn(product);
        when(sessionRepository.findBySessionId(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(
                        invocation ->
                                java.util.Optional.of(
                                        com.alibaba.himarket.entity.ChatSession.builder()
                                                .sessionId(invocation.getArgument(0))
                                                .userId("dev-a")
                                                .products(List.of("product-open"))
                                                .build()));

        ChatSessionServiceImpl service =
                new ChatSessionServiceImpl(
                        sessionRepository,
                        chatRepository,
                        productService,
                        contextHolder,
                        consumerService,
                        eventPublisher);

        CreateChatSessionParam param = new CreateChatSessionParam();
        param.setName("session-a");
        param.setTalkType(TalkType.MODEL);
        param.setProducts(List.of("product-open"));

        assertDoesNotThrow(() -> service.createSession(param));
    }

    private static SubscriptionResult subscription(String productId, String status) {
        SubscriptionResult result = new SubscriptionResult();
        result.setProductId(productId);
        result.setStatus(status);
        return result;
    }
}
