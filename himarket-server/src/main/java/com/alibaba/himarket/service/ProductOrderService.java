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

package com.alibaba.himarket.service;

import com.alibaba.himarket.dto.params.order.CreateProductOrderParam;
import com.alibaba.himarket.dto.params.order.PaymentCallbackParam;
import com.alibaba.himarket.dto.params.order.QueryProductOrderParam;
import com.alibaba.himarket.dto.result.common.PageResult;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import org.springframework.data.domain.Pageable;

public interface ProductOrderService {

    ProductOrderResult createOrder(String consumerId, CreateProductOrderParam param);

    ProductOrderResult markOrderPaid(String consumerId, String orderId);

    ProductOrderResult cancelOrder(String consumerId, String orderId);

    PageResult<ProductOrderResult> listOrders(
            String consumerId, QueryProductOrderParam param, Pageable pageable);

    ProductOrderResult getOrder(String consumerId, String orderId);

    PageResult<ProductOrderResult> listAllOrders(QueryProductOrderParam param, Pageable pageable);

    ProductOrderResult getOrderByAdmin(String orderId);

    ProductOrderResult markOrderPaidByAdmin(String orderId);

    ProductOrderResult cancelOrderByAdmin(String orderId);

    ProductOrderResult handlePaymentCallback(String orderId, PaymentCallbackParam param);
}
