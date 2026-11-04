package com.devops.shop.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemDto(
    @NotNull(message = "ID товару обов'язковий")
    Long productId,

    @NotBlank(message = "Назва товару обов'язкова")
    String productName,

    @NotNull(message = "Ціна обов'язкова")
    @DecimalMin(value = "0.01", message = "Ціна має бути більшою за 0")
    BigDecimal price,

    @NotNull(message = "Кількість обов'язкова")
    @Min(value = 1, message = "Кількість має бути щонайменше 1")
    Integer quantity
) {}
