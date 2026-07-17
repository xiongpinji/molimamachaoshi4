package com.alibaba.himarket.service.impl;

import com.alibaba.himarket.core.constant.Resources;
import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.exception.ErrorCode;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.core.utils.IdGenerator;
import com.alibaba.himarket.dto.params.coding.CreateCodingSessionParam;
import com.alibaba.himarket.dto.params.coding.UpdateCodingSessionParam;
import com.alibaba.himarket.dto.result.coding.CodingSessionResult;
import com.alibaba.himarket.dto.result.common.PageResult;
import com.alibaba.himarket.dto.result.consumer.ConsumerResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
import com.alibaba.himarket.dto.result.product.SubscriptionResult;
import com.alibaba.himarket.entity.CodingSession;
import com.alibaba.himarket.repository.CodingSessionRepository;
import com.alibaba.himarket.service.CodingSessionService;
import com.alibaba.himarket.service.ConsumerService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.enums.SubscriptionStatus;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CodingSessionServiceImpl implements CodingSessionService {

    private final CodingSessionRepository sessionRepository;
    private final ContextHolder contextHolder;
    private final ConsumerService consumerService;
    private final ProductService productService;

    private static final int MAX_SESSIONS_PER_USER = 50;

    @Override
    public CodingSessionResult createSession(CreateCodingSessionParam param) {
        validateApprovedModel(param.getModelProductId());
        String sessionId = IdGenerator.genSessionId();
        CodingSession session = param.convertTo();
        session.setUserId(contextHolder.getUser());
        session.setSessionId(sessionId);

        sessionRepository.save(session);
        cleanupExtraSessions();

        return new CodingSessionResult().convertFrom(session);
    }

    @Override
    public PageResult<CodingSessionResult> listSessions(Pageable pageable) {
        Page<CodingSession> sessions =
                sessionRepository.findByUserIdOrderByUpdatedAtDesc(
                        contextHolder.getUser(), pageable);

        return new PageResult<CodingSessionResult>()
                .convertFrom(sessions, session -> new CodingSessionResult().convertFrom(session));
    }

    @Override
    public CodingSessionResult updateSession(String sessionId, UpdateCodingSessionParam param) {
        CodingSession session = findUserSession(sessionId);
        param.update(session);

        sessionRepository.saveAndFlush(session);

        return new CodingSessionResult().convertFrom(session);
    }

    @Override
    public void deleteSession(String sessionId) {
        CodingSession session = findUserSession(sessionId);
        sessionRepository.delete(session);
    }

    private CodingSession findUserSession(String sessionId) {
        return sessionRepository
                .findBySessionIdAndUserId(sessionId, contextHolder.getUser())
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND, Resources.CHAT_SESSION, sessionId));
    }

    private void validateApprovedModel(String modelProductId) {
        if (modelProductId == null || modelProductId.isBlank()) {
            return;
        }

        ProductResult modelProduct = productService.getProduct(modelProductId);
        if (Boolean.FALSE.equals(modelProduct.getSubscribable())) {
            return;
        }

        ConsumerResult primaryConsumer =
                consumerService.getPrimaryConsumer(contextHolder.getUser());
        Set<String> approvedProductIds =
                consumerService.listConsumerSubscriptions(primaryConsumer.getConsumerId()).stream()
                        .filter(s -> SubscriptionStatus.APPROVED.name().equals(s.getStatus()))
                        .map(SubscriptionResult::getProductId)
                        .collect(Collectors.toSet());
        if (!approvedProductIds.contains(modelProductId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Model product `%s` is not approved for this consumer"
                            .formatted(modelProductId));
        }
    }

    private void cleanupExtraSessions() {
        String userId = contextHolder.getUser();
        int count = sessionRepository.countByUserId(userId);
        if (count > MAX_SESSIONS_PER_USER) {
            // Keep the newest MAX_SESSIONS_PER_USER sessions, delete the rest.
            // Results are ordered by updatedAt DESC (newest first).
            // Page 0 = newest 50 (keep), page 1+ = older (delete).
            sessionRepository
                    .findByUserIdOrderByUpdatedAtDesc(
                            userId, PageRequest.of(1, MAX_SESSIONS_PER_USER))
                    .forEach(sessionRepository::delete);
        }
    }
}
