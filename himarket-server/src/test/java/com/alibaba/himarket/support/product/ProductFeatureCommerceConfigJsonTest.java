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

package com.alibaba.himarket.support.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.himarket.utils.JsonUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductFeatureCommerceConfigJsonTest {

    @Test
    void shouldSerializeAndDeserializeCommerceConfig() {
        ProductFeature feature =
                ProductFeature.builder()
                        .commerceConfig(
                                CommerceConfig.builder()
                                        .enabled(true)
                                        .pricingMode(CommercePricingMode.ONE_TIME)
                                        .amount(new BigDecimal("99.90"))
                                        .currency("CNY")
                                        .displayLabel("首发价")
                                        .build())
                        .build();

        String json = JsonUtil.toJson(feature);

        assertTrue(json.contains("\"commerceConfig\""));

        ProductFeature parsed = JsonUtil.parse(json, ProductFeature.class);

        assertNotNull(parsed.getCommerceConfig());
        assertTrue(Boolean.TRUE.equals(parsed.getCommerceConfig().getEnabled()));
        assertEquals(CommercePricingMode.ONE_TIME, parsed.getCommerceConfig().getPricingMode());
        assertEquals(0, new BigDecimal("99.90").compareTo(parsed.getCommerceConfig().getAmount()));
        assertEquals("CNY", parsed.getCommerceConfig().getCurrency());
        assertEquals("首发价", parsed.getCommerceConfig().getDisplayLabel());
    }
}
