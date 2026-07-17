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

import com.alibaba.himarket.core.constant.Resources;
import com.alibaba.himarket.core.exception.BusinessException;
import com.alibaba.himarket.core.exception.ErrorCode;
import com.alibaba.himarket.core.security.ContextHolder;
import com.alibaba.himarket.core.utils.IdGenerator;
import com.alibaba.himarket.dto.params.order.CreateProductOrderParam;
import com.alibaba.himarket.dto.params.order.PaymentCallbackParam;
import com.alibaba.himarket.dto.params.order.QueryProductOrderParam;
import com.alibaba.himarket.dto.result.common.PageResult;
import com.alibaba.himarket.dto.result.order.PaymentRecordResult;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.dto.result.product.ProductResult;
import com.alibaba.himarket.entity.Consumer;
import com.alibaba.himarket.entity.PaymentRecord;
import com.alibaba.himarket.entity.ProductOrder;
import com.alibaba.himarket.repository.ConsumerRepository;
import com.alibaba.himarket.repository.PaymentRecordRepository;
import com.alibaba.himarket.repository.ProductOrderRepository;
import com.alibaba.himarket.service.ConsumerService;
import com.alibaba.himarket.service.ProductOrderService;
import com.alibaba.himarket.service.ProductService;
import com.alibaba.himarket.support.enums.PaymentRecordStatus;
import com.alibaba.himarket.support.enums.ProductOrderStatus;
import com.alibaba.himarket.support.product.CommerceConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductOrderServiceImpl implements ProductOrderService {

    private static final String ORDER_PREFIX = "order-";
    private static final List<ProductOrderStatus> ACTIVE_ORDER_STATUSES =
            List.of(ProductOrderStatus.PENDING_PAYMENT, ProductOrderStatus.PAID);

    private final ContextHolder contextHolder;
    private final ConsumerRepository consumerRepository;
    private final ProductOrderRepository productOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ProductService productService;
    private final ConsumerService consumerService;

    @Override
    public ProductOrderResult createOrder(String consumerId, CreateProductOrderParam param) {
        Consumer consumer =
                consumerRepository
                        .findByDeveloperIdAndConsumerId(contextHolder.getUser(), consumerId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                Resources.CONSUMER,
                                                consumerId));

        productOrderRepository
                .findFirstByConsumerIdAndProductIdAndStatusInOrderByCreateAtDesc(
                        consumerId, param.getProductId(), ACTIVE_ORDER_STATUSES)
                .ifPresent(
                        activeOrder -> {
                            throw new BusinessException(
                                    ErrorCode.CONFLICT,
                                    "Active order `%s` already exists for product `%s`"
                                            .formatted(
                                                    activeOrder.getOrderId(),
                                                    param.getProductId()));
                        });

        ProductResult product = productService.getProduct(param.getProductId());
        CommerceConfig commerceConfig = resolveCommerceConfig(product);

        ProductOrder order =
                ProductOrder.builder()
                        .orderId(IdGenerator.genIdWithPrefix(ORDER_PREFIX))
                        .productId(product.getProductId())
                        .consumerId(consumerId)
                        .developerId(contextHolder.getUser())
                        .portalId(consumer.getPortalId())
                        .productName(product.getName())
                        .amount(normalizeAmount(commerceConfig.getAmount()))
                        .currency(commerceConfig.getCurrency())
                        .pricingMode(commerceConfig.getPricingMode())
                        .status(ProductOrderStatus.PENDING_PAYMENT)
                        .build();

        return toResult(productOrderRepository.save(order));
    }

    @Override
    public ProductOrderResult markOrderPaid(String consumerId, String orderId) {
        ProductOrder order = loadOwnedOrder(consumerId, orderId);

        if (order.getStatus() == ProductOrderStatus.PAID) {
            return toResult(order);
        }

        if (order.getStatus() != ProductOrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Order `%s` cannot be marked as paid from status `%s`"
                            .formatted(orderId, order.getStatus()));
        }

        order.setStatus(ProductOrderStatus.PAID);
        order.setPaidAt(java.time.LocalDateTime.now());
        productOrderRepository.save(order);

        PaymentRecord paymentRecord =
                PaymentRecord.builder()
                        .paymentRecordId(IdGenerator.genIdWithPrefix("pay-"))
                        .orderId(order.getOrderId())
                        .provider("MANUAL")
                        .providerTransactionId(null)
                        .amount(order.getAmount())
                        .currency(order.getCurrency())
                        .status(PaymentRecordStatus.SUCCEEDED)
                        .callbackPayload("manual-confirmation")
                        .build();
        paymentRecordRepository.save(paymentRecord);

        consumerService.activatePurchasedProduct(order.getConsumerId(), order.getProductId());
        return toResult(order);
    }

    @Override
    public ProductOrderResult cancelOrder(String consumerId, String orderId) {
        ProductOrder order = loadOwnedOrder(consumerId, orderId);

        if (order.getStatus() == ProductOrderStatus.CANCELLED) {
            return toResult(order);
        }
        if (order.getStatus() != ProductOrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Order `%s` cannot be cancelled from status `%s`"
                            .formatted(orderId, order.getStatus()));
        }

        order.setStatus(ProductOrderStatus.CANCELLED);
        productOrderRepository.save(order);

        PaymentRecord paymentRecord =
                PaymentRecord.builder()
                        .paymentRecordId(IdGenerator.genIdWithPrefix("pay-"))
                        .orderId(order.getOrderId())
                        .provider("MANUAL")
                        .providerTransactionId(null)
                        .amount(order.getAmount())
                        .currency(order.getCurrency())
                        .status(PaymentRecordStatus.CANCELLED)
                        .callbackPayload("manual-cancel")
                        .build();
        paymentRecordRepository.save(paymentRecord);
        return toResult(order);
    }

    @Override
    public PageResult<ProductOrderResult> listOrders(
            String consumerId, QueryProductOrderParam param, Pageable pageable) {
        resolveAccessibleConsumer(consumerId);

        org.springframework.data.domain.Page<ProductOrder> page =
                productOrderRepository.findAll(buildOrderSpec(consumerId, param), pageable);
        return new PageResult<ProductOrderResult>().convertFrom(page, this::toResult);
    }

    @Override
    public ProductOrderResult getOrder(String consumerId, String orderId) {
        return toResult(loadOwnedOrder(consumerId, orderId));
    }

    @Override
    public ProductOrderResult getOrderByAdmin(String orderId) {
        ProductOrder order =
                productOrderRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                Resources.PRODUCT_ORDER,
                                                orderId));
        return toResult(order);
    }

    @Override
    public ProductOrderResult markOrderPaidByAdmin(String orderId) {
        ProductOrder order =
                productOrderRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                Resources.PRODUCT_ORDER,
                                                orderId));
        return markOrderPaid(order.getConsumerId(), orderId);
    }

    @Override
    public ProductOrderResult cancelOrderByAdmin(String orderId) {
        ProductOrder order =
                productOrderRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                Resources.PRODUCT_ORDER,
                                                orderId));
        return cancelOrder(order.getConsumerId(), orderId);
    }

    @Override
    public PageResult<ProductOrderResult> listAllOrders(
            QueryProductOrderParam param, Pageable pageable) {
        org.springframework.data.domain.Page<ProductOrder> page =
                productOrderRepository.findAll(buildOrderSpec(null, param), pageable);
        return new PageResult<ProductOrderResult>().convertFrom(page, this::toResult);
    }

    @Override
    public ProductOrderResult handlePaymentCallback(String orderId, PaymentCallbackParam param) {
        ProductOrder order =
                productOrderRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                Resources.PRODUCT_ORDER,
                                                orderId));

        PaymentRecord paymentRecord =
                PaymentRecord.builder()
                        .paymentRecordId(IdGenerator.genIdWithPrefix("pay-"))
                        .orderId(order.getOrderId())
                        .provider(param.getProvider())
                        .providerTransactionId(param.getProviderTransactionId())
                        .amount(order.getAmount())
                        .currency(order.getCurrency())
                        .status(param.getStatus())
                        .callbackPayload(param.getCallbackPayload())
                        .build();
        paymentRecordRepository.save(paymentRecord);

        if (param.getStatus() == PaymentRecordStatus.SUCCEEDED) {
            if (order.getStatus() != ProductOrderStatus.PAID) {
                order.setStatus(ProductOrderStatus.PAID);
                order.setPaidAt(java.time.LocalDateTime.now());
                productOrderRepository.save(order);
                consumerService.activatePurchasedProduct(
                        order.getConsumerId(), order.getProductId());
            }
        } else if (param.getStatus() == PaymentRecordStatus.CANCELLED) {
            if (order.getStatus() == ProductOrderStatus.PENDING_PAYMENT) {
                order.setStatus(ProductOrderStatus.CANCELLED);
                productOrderRepository.save(order);
            }
        } else if (param.getStatus() == PaymentRecordStatus.FAILED) {
            if (order.getStatus() == ProductOrderStatus.PENDING_PAYMENT) {
                order.setStatus(ProductOrderStatus.FAILED);
                productOrderRepository.save(order);
            }
        }

        return toResult(order);
    }

    private ProductOrderResult toResult(ProductOrder order) {
        ProductOrderResult result = new ProductOrderResult().convertFrom(order);
        List<PaymentRecord> paymentRecords =
                paymentRecordRepository.findByOrderId(order.getOrderId());
        result.setPaymentRecords(
                paymentRecords.stream()
                        .map(paymentRecord -> new PaymentRecordResult().convertFrom(paymentRecord))
                        .toList());
        return result;
    }

    private CommerceConfig resolveCommerceConfig(ProductResult product) {
        CommerceConfig commerceConfig =
                product.getFeature() != null ? product.getFeature().getCommerceConfig() : null;
        if (commerceConfig == null || !Boolean.TRUE.equals(commerceConfig.getEnabled())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Product `%s` is not available for purchase".formatted(product.getProductId()));
        }
        if (commerceConfig.getPricingMode() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Product `%s` is missing pricing mode".formatted(product.getProductId()));
        }
        if (commerceConfig.getCurrency() == null || commerceConfig.getCurrency().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Product `%s` is missing currency".formatted(product.getProductId()));
        }
        if (commerceConfig.getAmount() == null
                || commerceConfig.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Product `%s` has invalid price".formatted(product.getProductId()));
        }
        return commerceConfig;
    }

    private org.springframework.data.jpa.domain.Specification<ProductOrder> buildOrderSpec(
            String consumerId, QueryProductOrderParam param) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (consumerId != null && !consumerId.isBlank()) {
                predicates.add(cb.equal(root.get("consumerId"), consumerId));
            }
            if (param.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), param.getStatus()));
            }
            if (param.getProductId() != null && !param.getProductId().isBlank()) {
                predicates.add(cb.equal(root.get("productId"), param.getProductId()));
            }
            if (Boolean.TRUE.equals(param.getActiveOnly())) {
                predicates.add(root.get("status").in(ACTIVE_ORDER_STATUSES));
            }
            if (param.getProductName() != null && !param.getProductName().isBlank()) {
                String likePattern = "%" + param.getProductName().trim() + "%";
                predicates.add(cb.like(root.get("productName"), likePattern));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ProductOrder loadOwnedOrder(String consumerId, String orderId) {
        resolveAccessibleConsumer(consumerId);

        ProductOrder order =
                productOrderRepository
                        .findByOrderId(orderId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                Resources.PRODUCT_ORDER,
                                                orderId));

        if (contextHolder.isAdministrator()) {
            if (!consumerId.equals(order.getConsumerId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, Resources.PRODUCT_ORDER, orderId);
            }
            return order;
        }

        if (!contextHolder.getUser().equals(order.getDeveloperId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, Resources.PRODUCT_ORDER, orderId);
        }
        return order;
    }

    private Consumer resolveAccessibleConsumer(String consumerId) {
        if (contextHolder.isAdministrator()) {
            return consumerRepository
                    .findByConsumerId(consumerId)
                    .orElseThrow(
                            () ->
                                    new BusinessException(
                                            ErrorCode.NOT_FOUND, Resources.CONSUMER, consumerId));
        }
        return consumerRepository
                .findByDeveloperIdAndConsumerId(contextHolder.getUser(), consumerId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND, Resources.CONSUMER, consumerId));
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
