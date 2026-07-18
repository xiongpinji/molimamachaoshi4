package com.alibaba.himarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alibaba.himarket.dto.result.common.PageResult;
import com.alibaba.himarket.dto.result.order.ProductOrderResult;
import com.alibaba.himarket.service.ProductOrderService;
import com.alibaba.himarket.support.enums.ProductOrderStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminProductOrderControllerTest {

    private ProductOrderService productOrderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productOrderService = mock(ProductOrderService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AdminProductOrderController(productOrderService))
                        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                        .build();
    }

    @Test
    void listOrdersUsesTopLevelAdminRoute() throws Exception {
        when(productOrderService.listAllOrders(any(), any()))
                .thenReturn(
                        PageResult.of(
                                List.of(
                                        ProductOrderResult.builder()
                                                .orderId("order-a")
                                                .consumerId("consumer-a")
                                                .status(ProductOrderStatus.PAID)
                                                .build()),
                                1,
                                10,
                                1));

        mockMvc.perform(get("/orders")).andExpect(status().isOk());

        verify(productOrderService).listAllOrders(any(), any());
    }

    @Test
    void getOrderUsesTopLevelAdminDetailRoute() throws Exception {
        when(productOrderService.getOrderByAdmin("order-a"))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .consumerId("consumer-a")
                                .status(ProductOrderStatus.PAID)
                                .build());

        mockMvc.perform(get("/orders/order-a")).andExpect(status().isOk());

        verify(productOrderService).getOrderByAdmin("order-a");
    }

    @Test
    void markOrderPaidUsesTopLevelAdminActionRoute() throws Exception {
        when(productOrderService.markOrderPaidByAdmin("order-a"))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .status(ProductOrderStatus.PAID)
                                .build());

        mockMvc.perform(patch("/orders/order-a/paid")).andExpect(status().isOk());

        verify(productOrderService).markOrderPaidByAdmin("order-a");
    }

    @Test
    void handlePaymentCallbackUsesTopLevelAdminCallbackRoute() throws Exception {
        when(productOrderService.handlePaymentCallback(
                        org.mockito.ArgumentMatchers.eq("order-a"), any()))
                .thenReturn(
                        ProductOrderResult.builder()
                                .orderId("order-a")
                                .status(ProductOrderStatus.PAID)
                                .build());

        mockMvc.perform(
                        patch("/orders/order-a/callback")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"provider\":\"MOCK\",\"providerTransactionId\":\"txn-a\",\"status\":\"SUCCEEDED\",\"callbackPayload\":\"mock-callback\"}"))
                .andExpect(status().isOk());

        verify(productOrderService)
                .handlePaymentCallback(org.mockito.ArgumentMatchers.eq("order-a"), any());
    }
}
