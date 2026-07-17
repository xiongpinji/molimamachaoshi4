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

package com.alibaba.himarket.entity;

import com.alibaba.himarket.support.enums.ProductOrderStatus;
import com.alibaba.himarket.support.product.CommercePricingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "product_order",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {"order_id"},
                    name = "uk_order_id")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Column(name = "product_id", length = 64, nullable = false)
    private String productId;

    @Column(name = "consumer_id", length = 64, nullable = false)
    private String consumerId;

    @Column(name = "developer_id", length = 64, nullable = false)
    private String developerId;

    @Column(name = "portal_id", length = 64, nullable = false)
    private String portalId;

    @Column(name = "product_name", length = 128, nullable = false)
    private String productName;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 16, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", length = 32, nullable = false)
    private CommercePricingMode pricingMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ProductOrderStatus status;

    @Column(name = "paid_at", columnDefinition = "datetime(3)")
    private LocalDateTime paidAt;
}
