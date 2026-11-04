package com.devops.shop.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductCreateRequest(
    @NotBlank(message = "Назва товару обов'язкова")
    @Size(min = 2, max = 150, message = "Назва має містити від 2 до 150 символів")
    String name,

    String description,

    @NotNull(message = "Ціна обов'язкова")
    @DecimalMin(value = "0.01", message = "Ціна має бути більшою за 0")
    BigDecimal price,

    @NotBlank(message = "Категорія обов'язкова")
    String category,

    @NotNull(message = "Кількість обов'язкова")
    @Min(value = 0, message = "Кількість не може бути від'ємною")
    Integer stock,

    String imageUrl
) {}
