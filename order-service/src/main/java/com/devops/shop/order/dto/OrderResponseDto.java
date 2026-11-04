package com.devops.shop.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
    Long id,
    String customerName,
    String customerEmail,
    String customerAddress,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt,
    List<OrderItemDto> items
) {}
