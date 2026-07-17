package com.alibaba.himarket.controller;

import com.alibaba.himarket.core.annotation.AdminAuth;
import com.alibaba.himarket.dto.params.order.PaymentCallbackParam;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Product Order Management", description = "Top-level admin order operations")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class AdminProductOrderController {

    private final ProductOrderService productOrderService;

    @Operation(summary = "List orders")
    @GetMapping
    @AdminAuth
    public PageResult<ProductOrderResult> listOrders(
            QueryProductOrderParam param, Pageable pageable) {
        return productOrderService.listAllOrders(param, pageable);
    }

    @Operation(summary = "Get order")
    @GetMapping("/{orderId}")
    @AdminAuth
    public ProductOrderResult getOrder(@PathVariable String orderId) {
        return productOrderService.getOrderByAdmin(orderId);
    }

    @Operation(summary = "Mark order paid")
    @PatchMapping("/{orderId}/paid")
    @AdminAuth
    public ProductOrderResult markOrderPaid(@PathVariable String orderId) {
        return productOrderService.markOrderPaidByAdmin(orderId);
    }

    @Operation(summary = "Cancel order")
    @PatchMapping("/{orderId}/cancel")
    @AdminAuth
    public ProductOrderResult cancelOrder(@PathVariable String orderId) {
        return productOrderService.cancelOrderByAdmin(orderId);
    }

    @Operation(summary = "Handle payment callback")
    @PatchMapping("/{orderId}/callback")
    @AdminAuth
    public ProductOrderResult handlePaymentCallback(
            @PathVariable String orderId, @RequestBody @Valid PaymentCallbackParam param) {
        return productOrderService.handlePaymentCallback(orderId, param);
    }
}
