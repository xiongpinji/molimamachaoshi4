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

package com.alibaba.himarket.controller;

import com.alibaba.himarket.core.annotation.AdminOrDeveloperAuth;
import com.alibaba.himarket.core.annotation.DeveloperAuth;
import com.alibaba.himarket.dto.params.order.CreateProductOrderParam;
import com.alibaba.himarket.dto.params.order.QueryProductOrderParam;
import com.alibaba.himarket.dto.result.common.PageResult;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.service.ProductOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Order Management", description = "Create product purchase orders")
@RestController
@RequestMapping("/consumers/{consumerId}/orders")
@RequiredArgsConstructor
@Validated
public class ProductOrderController {

    private final ProductOrderService productOrderService;

    @Operation(summary = "Create product order")
    @PostMapping
    @DeveloperAuth
    public ProductOrderResult createOrder(
            @PathVariable String consumerId, @RequestBody @Valid CreateProductOrderParam param) {
        return productOrderService.createOrder(consumerId, param);
    }

    @Operation(summary = "List product orders")
    @GetMapping
    @AdminOrDeveloperAuth
    public PageResult<ProductOrderResult> listOrders(
            @PathVariable String consumerId, QueryProductOrderParam param, Pageable pageable) {
        return productOrderService.listOrders(consumerId, param, pageable);
    }

    @Operation(summary = "Get product order")
    @GetMapping("/{orderId}")
    @AdminOrDeveloperAuth
    public ProductOrderResult getOrder(
            @PathVariable String consumerId, @PathVariable String orderId) {
        return productOrderService.getOrder(consumerId, orderId);
    }

    @Operation(summary = "Mark product order paid")
    @PatchMapping("/{orderId}/paid")
    @AdminOrDeveloperAuth
    public ProductOrderResult markOrderPaid(
            @PathVariable String consumerId, @PathVariable String orderId) {
        return productOrderService.markOrderPaid(consumerId, orderId);
    }

    @Operation(summary = "Cancel product order")
    @PatchMapping("/{orderId}/cancel")
    @AdminOrDeveloperAuth
    public ProductOrderResult cancelOrder(
            @PathVariable String consumerId, @PathVariable String orderId) {
        return productOrderService.cancelOrder(consumerId, orderId);
    }
}
