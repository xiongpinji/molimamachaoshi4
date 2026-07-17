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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alibaba.himarket.dto.params.order.QueryProductOrderParam;
import com.alibaba.himarket.dto.result.common.PageResult;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.service.ProductOrderService;
import com.alibaba.himarket.support.enums.ProductOrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductOrderControllerTest {

    private ProductOrderService productOrderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productOrderService = mock(ProductOrderService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ProductOrderController(productOrderService))
                        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                        .build();
    }

    @Test
    void createOrderUsesConsumerScopedRoute() throws Exception {
        when(productOrderService.createOrder(eq("consumer-a"), any()))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .consumerId("consumer-a")
                                .productId("product-a")
                                .productName("Legal Assistant")
                                .amount(new BigDecimal("99.90"))
                                .currency("CNY")
                                .build());

        mockMvc.perform(
                        post("/consumers/consumer-a/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":\"product-a\"}"))
                .andExpect(status().isOk());

        verify(productOrderService).createOrder(eq("consumer-a"), any());
    }

    @Test
    void listOrdersUsesConsumerScopedGetRoute() throws Exception {
        when(productOrderService.listOrders(eq("consumer-a"), any(), any()))
                .thenReturn(
                        PageResult.of(
                                java.util.List.of(
                                        ProductOrderResult.builder()
                                                .orderId("order-a")
                                                .consumerId("consumer-a")
                                                .productId("product-a")
                                                .status(ProductOrderStatus.PAID)
                                                .build()),
                                1,
                                10,
                                1));

        mockMvc.perform(get("/consumers/consumer-a/orders")).andExpect(status().isOk());

        verify(productOrderService).listOrders(eq("consumer-a"), any(), any());
    }

    @Test
    void listOrdersBindsProductIdAndActiveOnlyQueryParams() throws Exception {
        when(productOrderService.listOrders(eq("consumer-a"), any(), any()))
                .thenReturn(PageResult.of(java.util.List.of(), 1, 1, 0));

        mockMvc.perform(
                        get("/consumers/consumer-a/orders")
                                .queryParam("productId", "product-a")
                                .queryParam("activeOnly", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<QueryProductOrderParam> captor =
                ArgumentCaptor.forClass(QueryProductOrderParam.class);
        verify(productOrderService).listOrders(eq("consumer-a"), captor.capture(), any());
        assertEquals("product-a", captor.getValue().getProductId());
        assertEquals(Boolean.TRUE, captor.getValue().getActiveOnly());
    }

    @Test
    void getOrderUsesConsumerScopedDetailRoute() throws Exception {
        when(productOrderService.getOrder("consumer-a", "order-a"))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .consumerId("consumer-a")
                                .productId("product-a")
                                .status(ProductOrderStatus.PAID)
                                .build());

        mockMvc.perform(get("/consumers/consumer-a/orders/order-a")).andExpect(status().isOk());

        verify(productOrderService).getOrder("consumer-a", "order-a");
    }

    @Test
    void markOrderPaidUsesOrderScopedPatchRoute() throws Exception {
        when(productOrderService.markOrderPaid("consumer-a", "order-a"))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .consumerId("consumer-a")
                                .productId("product-a")
                                .status(ProductOrderStatus.PAID)
                                .build());

        mockMvc.perform(patch("/consumers/consumer-a/orders/order-a/paid"))
                .andExpect(status().isOk());

        verify(productOrderService).markOrderPaid("consumer-a", "order-a");
    }

    @Test
    void cancelOrderUsesOrderScopedPatchRoute() throws Exception {
        when(productOrderService.cancelOrder("consumer-a", "order-a"))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .consumerId("consumer-a")
                                .productId("product-a")
                                .status(ProductOrderStatus.CANCELLED)
                                .build());

        mockMvc.perform(patch("/consumers/consumer-a/orders/order-a/cancel"))
                .andExpect(status().isOk());

        verify(productOrderService).cancelOrder("consumer-a", "order-a");
    }
}
